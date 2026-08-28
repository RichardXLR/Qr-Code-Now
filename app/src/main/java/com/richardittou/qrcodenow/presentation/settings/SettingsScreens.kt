package com.richardittou.qrcodenow.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.richardittou.qrcodenow.BuildConfig
import com.richardittou.qrcodenow.domain.action.AndroidExternalActionLauncher
import com.richardittou.qrcodenow.domain.model.AppTheme
import com.richardittou.qrcodenow.presentation.history.HistoryViewModel
import com.richardittou.qrcodenow.ui.theme.BrandRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onCredits: () -> Unit,
    onPrivacy: () -> Unit,
    onLicenses: () -> Unit,
    historyViewModel: HistoryViewModel = hiltViewModel()
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var clearDialog by remember { mutableStateOf(false) }
    var autoOpenDialog by remember { mutableStateOf(false) }
    Scaffold(topBar = { TopAppBar(title = { Text("Configurações") }) }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            item { SectionTitle("Aparência") }
            item {
                Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                    Column {
                        AppTheme.entries.forEach { theme ->
                            Row(
                                Modifier.fillMaxWidth().clickable { viewModel.setTheme(theme) }.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Palette, null, tint = if (settings.theme == theme) BrandRed else MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(theme.label(), Modifier.weight(1f).padding(start = 12.dp))
                                RadioButton(settings.theme == theme, onClick = { viewModel.setTheme(theme) })
                            }
                        }
                    }
                }
            }
            item { SectionTitle("Leitura") }
            item {
                SettingSwitch(
                    "Vibrar após leitura",
                    "Confirma discretamente quando um QR Code é reconhecido.",
                    settings.vibrationEnabled,
                    viewModel::setVibration
                )
            }
            item {
                SettingSwitch(
                    "Abrir links seguros automaticamente",
                    "Somente endereços HTTP/HTTPS válidos e sem sinais de risco.",
                    settings.autoOpenSafeUrls
                ) { enabled -> if (enabled) autoOpenDialog = true else viewModel.setAutoOpen(false) }
            }
            item { SectionTitle("Dados e informações") }
            item { SettingsLink("Limpar histórico", Icons.Outlined.DeleteSweep, { clearDialog = true }) }
            item { SettingsLink("Créditos do criador", Icons.Outlined.Person, onCredits) }
            item { SettingsLink("Política de privacidade", Icons.Outlined.PrivacyTip, onPrivacy) }
            item { SettingsLink("Licenças de código aberto", Icons.Outlined.Code, onLicenses) }
            item { Text("Versão ${BuildConfig.VERSION_NAME}", modifier = Modifier.fillMaxWidth().padding(20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
    }
    if (clearDialog) AlertDialog(
        onDismissRequest = { clearDialog = false },
        title = { Text("Limpar histórico?") },
        text = { Text("Todas as leituras e favoritos serão excluídos permanentemente.") },
        confirmButton = { TextButton(onClick = { historyViewModel.clearHistory(); clearDialog = false }) { Text("Excluir tudo") } },
        dismissButton = { TextButton(onClick = { clearDialog = false }) { Text("Cancelar") } }
    )
    if (autoOpenDialog) AlertDialog(
        onDismissRequest = { autoOpenDialog = false },
        title = { Text("Ativar abertura automática?") },
        text = { Text("Links reconhecidos como seguros serão enviados diretamente ao navegador após a leitura. Endereços suspeitos continuarão exigindo confirmação.") },
        confirmButton = {
            TextButton(onClick = { viewModel.setAutoOpen(true); autoOpenDialog = false }) { Text("Ativar") }
        },
        dismissButton = { TextButton(onClick = { autoOpenDialog = false }) { Text("Cancelar") } }
    )
}

@Composable
private fun SectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp))

@Composable
private fun SettingSwitch(title: String, description: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable { onChecked(!checked) }.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onChecked)
    }
}

@Composable
private fun SettingsLink(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null)
        Text(title, Modifier.weight(1f).padding(start = 14.dp))
        Icon(Icons.Outlined.ChevronRight, null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val launcher = remember { AndroidExternalActionLauncher() }
    var error by remember { mutableStateOf(false) }
    InfoScaffold("Créditos do criador", onBack) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.Person, null, tint = BrandRed)
                    Text("Criado por Richard Ittou", style = MaterialTheme.typography.titleLarge)
                    Text("Desenvolvedor e criador do QR Code Now")
                    SocialLink("Instagram", "https://www.instagram.com/richard.ittou/") {
                        launcher.openUrl(context, "https://www.instagram.com/richard.ittou/").onFailure { error = true }
                    }
                    SocialLink("GitHub", "https://github.com/RichardXLR") {
                        launcher.openUrl(context, "https://github.com/RichardXLR").onFailure { error = true }
                    }
                    if (error) Text("Nenhum navegador está disponível.", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SocialLink(label: String, url: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, null, tint = MaterialTheme.colorScheme.primary)
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(url, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun PrivacyScreen(onBack: () -> Unit) = InfoScaffold("Política de privacidade", onBack) {
    item { Text("Privacidade em primeiro lugar", style = MaterialTheme.typography.headlineMedium) }
    item { Text("O QR Code Now processa imagens, câmera e QR Codes localmente. O aplicativo não possui servidores, contas, publicidade, analytics ou rastreadores e não solicita permissão de internet.") }
    item { Text("Câmera e imagens", style = MaterialTheme.typography.titleLarge); Text("A câmera é usada somente durante a leitura. Imagens escolhidas pelo seletor do Android não são copiadas nem enviadas para terceiros.") }
    item { Text("Histórico", style = MaterialTheme.typography.titleLarge); Text("Leituras e favoritos ficam no banco de dados privado do aplicativo. O backup em nuvem está desativado e você pode apagar os dados a qualquer momento.") }
    item { Text("Ações externas", style = MaterialTheme.typography.titleLarge); Text("Links, mapas, telefone e compartilhamento são abertos por aplicativos escolhidos pelo Android. Links suspeitos exigem confirmação. A permissão de dispositivos próximos só é solicitada quando você decide conectar a uma rede Wi-Fi lida pelo aplicativo e nunca é usada para localização.") }
}

@Composable
fun LicensesScreen(onBack: () -> Unit) {
    val licenses = listOf(
        "AndroidX / Jetpack" to "Apache License 2.0",
        "Jetpack Compose / Material 3" to "Apache License 2.0",
        "CameraX" to "Apache License 2.0",
        "ML Kit Barcode Scanning" to "Google ML Kit Terms",
        "Dagger / Hilt" to "Apache License 2.0",
        "ZXing" to "Apache License 2.0",
        "Kotlin / Coroutines" to "Apache License 2.0"
    )
    InfoScaffold("Licenças", onBack) {
        items(licenses) { (name, license) ->
            Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(name, style = MaterialTheme.typography.titleMedium)
                Text(license, color = MaterialTheme.colorScheme.onSurfaceVariant)
                HorizontalDivider(Modifier.padding(top = 12.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InfoScaffold(title: String, onBack: () -> Unit, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(title) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Voltar") } }
        )
    }) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            LazyColumn(
                Modifier.fillMaxWidth().widthIn(max = 840.dp).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                content = content
            )
        }
    }
}

private fun AppTheme.label(): String = when (this) {
    AppTheme.SYSTEM -> "Seguir o sistema"
    AppTheme.LIGHT -> "Claro"
    AppTheme.DARK -> "Escuro"
}
