package cn.ac.lz233.tarnhelm.ui.extensions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import cn.ac.lz233.tarnhelm.R
import cn.ac.lz233.tarnhelm.databinding.ActivityExtensionsBinding
import cn.ac.lz233.tarnhelm.extension.ExtensionManager
import cn.ac.lz233.tarnhelm.extension.ExtensionRecord
import cn.ac.lz233.tarnhelm.ui.SecondaryBaseActivity
import com.google.android.material.snackbar.Snackbar
import com.permissionx.guolindev.PermissionX
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.async
import kotlinx.coroutines.launch

class ExtensionsActivity : SecondaryBaseActivity() {

    private val binding by lazy { ActivityExtensionsBinding.inflate(layoutInflater) }

    private val onExtInstallExceptionHandler = CoroutineExceptionHandler { _, exception ->
        // TODO: handle different exceptions during extension installation
        Log.e("ExtensionManager", "Failed to install extension", exception)
    }

    private val selectFileCallback = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val fileUri = result.data?.data ?: return@registerForActivityResult

            // check the file extension is .dex
            if (fileUri.path?.endsWith(".dex") == false) {
//                    Snackbar.make(binding.root, R.string.extensionNotSupported, Snackbar.LENGTH_SHORT).show() // TODO
                return@registerForActivityResult
            }

            contentResolver.openInputStream(fileUri)?.let {
                launch(onExtInstallExceptionHandler) {
                    ExtensionManager.installExtension(it)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        toolbar = binding.toolbar
        setContentView(binding.root)
        setSupportActionBar(toolbar)



        binding.openWebImageView.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://tarnhelm.project.ac.cn/rules.html")))
        }

        binding.importFab.setOnClickListener {
            startImport()
        }

        extensionList = ExtensionManager.getInstalledExtensions()

        binding.extensionsComposeView.setContent {
            Column(Modifier.fillMaxSize()) {
                Button(
                    onClick = {
                        val ext = ExtensionManager.getInstalledExtensions().firstOrNull()
                        ext?.let { ExtensionManager.enableExtension(it) }
                    }
                ) {
                    Text("Enable")
                }
                Spacer(Modifier.padding(vertical = 5.dp))
                Button(
                    onClick = {
                        val ext = ExtensionManager.getInstalledExtensions().firstOrNull()
                        ext?.let { ExtensionManager.disableExtension(it) }
                    }
                ) {
                    Text("Disable")
                }
                Spacer(Modifier.padding(vertical = 5.dp))
                Button(
                    onClick = {
                        val ext = ExtensionManager.getInstalledExtensions().firstOrNull()
                        ext?.let { ExtensionManager.uninstallExtension(it) }
                    }
                ) {
                    Text("Uninstall")
                }
                Spacer(Modifier.padding(vertical = 5.dp))
                Button(
                    onClick = {
                        val ext = ExtensionManager.getInstalledExtensions().firstOrNull()
                        ext?.let {
                            launch {
                                val result = async { ExtensionManager.requestHandleString(it, "Hello, World!") }
                                Log.d("ExtensionManager", "HandleString: ${result.await()}")
                            }
                        }
                    }
                ) {
                    Text("HandleString")
                }
                Spacer(Modifier.padding(vertical = 5.dp))
                Button(
                    onClick = {
                        val ext = ExtensionManager.getInstalledExtensions().firstOrNull()
                        ext?.let {
                            launch {
                                val result = async { ExtensionManager.requestCheckUpdate(it) }
                                Log.d("ExtensionManager", "CheckUpdate: ${result.await()}")
                            }
                        }
                    }
                ) {
                    Text("CheckUpdate")
                }
                Spacer(Modifier.padding(vertical = 5.dp))
                Button(
                    onClick = {
                        val ext = ExtensionManager.getInstalledExtensions().firstOrNull()
                        ext?.let {
                            ExtensionManager.startExtensionConfigurationPanel(it, this@ExtensionsActivity)
                        }
                    }
                ) {
                    Text("Panel")
                }
            }
        }

//        ExtensionManager.startExtensionConfigurationPanel("cn.ac.lz233.tarnhelm.ext.example", this)
    }

    private fun startImport() {
        // When using SAF, READ_EXTERNAL_STORAGE is not needed any more from Android Q
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) listOf() else listOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        PermissionX.init(this)
            .permissions(permissions)
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {
                    val selectFileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }
                    selectFileCallback.launch(selectFileIntent)
                } else {
                    Snackbar.make(binding.root, R.string.backupRequestPermissionFailedToast, Toast.LENGTH_SHORT).show()
                }
            }
    }

    companion object {
        fun actionStart(context: Context) = context.startActivity(Intent(context, ExtensionsActivity::class.java))
    }
}

var extensionList by mutableStateOf<List<ExtensionRecord>>(emptyList())

@Preview
@Composable
fun ExtensionItem() {
    // name, version, versionName, author, description, hasConfigurationPanel, enabled, regexes
    val name = "Extension Name"
    val versionName = "v1.0.0"
    val version = 100
    val author = "lz233"
//    val description = "This is a ${"long ".repeat(100)}description."
    val description = "This is a short description."
    val enabled = false
    val hasUpdate by remember { mutableStateOf(true) }
    val regexes = listOf(
//        "(\\W|^)[\\w.\\-]{0,25}@(yahoo|hotmail|gmail)\\.com(\\W|\$)",
//        "192\\.168\\.1\\.",
//        "(\\W|^)stock\\stips(\\W|\$)",
//        "(?i)(\\W|^)(baloney|darn|drat|fooey|gosh\\sdarnit|heck)(\\W|\$)",
//        "f[a4@][s5\\\$][t7] +c[a4@][s5\\\$]h",
//        "^[0-9]+\$",
//        "[1-9][0-9]*|0",
        "^[+-]?[1-9][0-9]*|0\$",
        "[a-zA-Z_][0-9a-zA-Z_]*",
        "^\\w+\\.(gif|png|jpg|jpeg)\$"
    )
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.fillMaxWidth(0.8F)) {
                    Text(
                        text = name,
                        fontSize = TextUnit(20F, TextUnitType.Sp),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Version: $versionName ($version)",
                        fontSize = TextUnit(12F, TextUnitType.Sp)
                    )
                    Text(
                        text = "Author: $author",
                        fontSize = TextUnit(12F, TextUnitType.Sp)
                    )
                }
                Spacer(modifier = Modifier.weight(1F))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Switch(
                        checked = enabled,
                        onCheckedChange = { /*TODO*/ },
                    )
                }
            }

            Spacer(Modifier.fillMaxWidth().padding(vertical = 5.dp))

            Text(
                text = description,
                style = TextStyle(fontSize = TextUnit(12F, TextUnitType.Sp))
            )

            Spacer(Modifier.fillMaxWidth().padding(vertical = 5.dp))

            if (regexes.size > 6) {
                Text(
                    buildAnnotatedString {
                        append("Regexes:\n")
                        append(regexes.take(5).joinToString("\n"))
                    },
                    fontSize = TextUnit(12F, TextUnitType.Sp),
                    lineHeight = TextUnit(1.5F, TextUnitType.Em)
                )
                Text(
                    text = "... and ${regexes.size - 5} more",
                    style = TextStyle(color = Color(0xFF3578E5), fontWeight = FontWeight.Bold),
                    fontSize = TextUnit(12F, TextUnitType.Sp),
                    lineHeight = TextUnit(1.5F, TextUnitType.Em),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { /* TODO */ }
                )
            } else {
                Text(
                    text = regexes.joinToString("\n"),
                    fontSize = TextUnit(12F, TextUnitType.Sp),
                    lineHeight = TextUnit(1.5F, TextUnitType.Em)
                )
            }


            Spacer(Modifier.fillMaxWidth().padding(vertical = 5.dp))
            HorizontalDivider(thickness = 3.dp)
            Spacer(Modifier.fillMaxWidth().padding(vertical = 3.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasUpdate) {
                    Button(
                        onClick = { /*TODO*/ }
                    ) {
                        Text(
                            text = "Update"
                        )
                    }
                }
                Button(
                    onClick = { /*TODO*/ }
                ) {
                    Text(
                        text = "Uninstall"
                    )
                }
            }
        }
    }
}