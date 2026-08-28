package com.richardittou.qrcodenow.presentation.generator

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.richardittou.qrcodenow.data.media.QrImageStore
import com.richardittou.qrcodenow.domain.model.QrType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneratorScreen(viewModel: GeneratorViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val imageStore = remember { QrImageStore() }
    var typeMenu by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            message = null
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Criar QR Code") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                ExposedDropdownMenuBox(
                    expanded = typeMenu,
                    onExpandedChange = { typeMenu = !typeMenu }
                ) {
                    OutlinedTextField(
                        value = state.type.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Tipo de QR Code") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeMenu) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = typeMenu, onDismissRequest = { typeMenu = false }) {
                        QrType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.label) },
                                onClick = { viewModel.setType(type); typeMenu = false }
                            )
                        }
                    }
                }
            }
            item {
                GeneratorFields(state, viewModel)
            }
            item {
                Button(onClick = viewModel::generate, modifier = Modifier.fillMaxWidth(), enabled = !state.generating) {
                    Text("Gerar QR Code")
                }
                if (state.generating) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp)) }
            }
            state.bitmap?.let { bitmap ->
                item {
                    OutlinedCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                bitmap.asImageBitmap(),
                                "QR Code gerado",
                                modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp).aspectRatio(1f)
                            )
                            Spacer(Modifier.height(12.dp))
                            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = {
                                    imageStore.save(context, bitmap).onSuccess { message = "Imagem salva em Pictures/QR Code Now" }
                                        .onFailure { message = "Não foi possível salvar a imagem." }
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Outlined.Save, null)
                                    Spacer(Modifier.size(8.dp))
                                    Text("Salvar imagem")
                                }
                                FilledTonalButton(onClick = {
                                    imageStore.save(context, bitmap).onSuccess { uri ->
                                        imageStore.share(context, uri).onFailure { message = "Não foi possível compartilhar." }
                                    }.onFailure { message = "Não foi possível preparar a imagem." }
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Icon(Icons.Outlined.Share, null)
                                    Spacer(Modifier.size(8.dp))
                                    Text("Compartilhar")
                                }
                                FilledTonalButton(
                                    onClick = viewModel::addToFavorites,
                                    enabled = !state.isFavorite,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Outlined.Favorite, null)
                                    Spacer(Modifier.size(8.dp))
                                    Text(if (state.isFavorite) "Adicionado aos favoritos" else "Adicionar aos favoritos")
                                }
                            }
                            state.payload?.let { Text(it, maxLines = 4, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun GeneratorFields(state: GeneratorUiState, viewModel: GeneratorViewModel) {
    var showWifiPassword by rememberSaveable(state.type) { mutableStateOf(false) }
    val labels = when (state.type) {
        QrType.TEXT, QrType.URL, QrType.PHONE, QrType.PIX, QrType.APP_LINK, QrType.CUSTOM -> listOf(primaryLabel(state.type))
        QrType.SMS -> listOf("Telefone", "Mensagem")
        QrType.EMAIL -> listOf("E-mail", "Assunto", "Mensagem")
        QrType.WIFI -> listOf("Nome da rede", "Senha", "Segurança (WPA, WEP ou nopass)")
        QrType.LOCATION -> listOf("Latitude", "Longitude")
        QrType.CONTACT -> listOf("Nome", "Telefone", "E-mail")
        QrType.EVENT -> listOf("Título", "Data/hora (AAAAMMDDTHHMMSS)", "Local")
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = state.primary,
            onValueChange = viewModel::setPrimary,
            label = { Text(labels[0]) },
            modifier = Modifier.fillMaxWidth(),
            minLines = if (state.type in setOf(QrType.TEXT, QrType.CUSTOM, QrType.PIX)) 3 else 1,
            singleLine = state.type !in setOf(QrType.TEXT, QrType.CUSTOM, QrType.PIX),
            keyboardOptions = KeyboardOptions(keyboardType = state.type.primaryKeyboardType())
        )
        labels.getOrNull(1)?.let { label ->
            OutlinedTextField(
                state.secondary,
                viewModel::setSecondary,
                label = { Text(label) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = state.type !in setOf(QrType.SMS, QrType.EMAIL),
                keyboardOptions = KeyboardOptions(keyboardType = state.type.secondaryKeyboardType()),
                visualTransformation = if (state.type == QrType.WIFI && !showWifiPassword) PasswordVisualTransformation() else VisualTransformation.None,
                trailingIcon = if (state.type == QrType.WIFI) ({
                    TextButton(onClick = { showWifiPassword = !showWifiPassword }) {
                        Text(if (showWifiPassword) "Ocultar" else "Mostrar")
                    }
                }) else null
            )
        }
        labels.getOrNull(2)?.let { label ->
            if (state.type == QrType.WIFI) {
                WifiSecurityField(state.tertiary, viewModel::setTertiary)
            } else {
                OutlinedTextField(
                    state.tertiary,
                    viewModel::setTertiary,
                    label = { Text(label) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = if (state.type == QrType.CONTACT) KeyboardType.Email else KeyboardType.Text)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WifiSecurityField(value: String, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("WPA2", "WPA3", "WPA", "WEP", "nopass")
    ExposedDropdownMenuBox(expanded, { expanded = !expanded }) {
        OutlinedTextField(
            value = value.ifBlank { "WPA2" },
            onValueChange = {},
            readOnly = true,
            label = { Text("Segurança") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(expanded, { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(if (option == "nopass") "Sem senha" else option) }, onClick = {
                    onValueChange(option)
                    expanded = false
                })
            }
        }
    }
}

private fun QrType.primaryKeyboardType(): KeyboardType = when (this) {
    QrType.URL, QrType.APP_LINK -> KeyboardType.Uri
    QrType.PHONE, QrType.SMS -> KeyboardType.Phone
    QrType.EMAIL -> KeyboardType.Email
    QrType.LOCATION -> KeyboardType.Decimal
    else -> KeyboardType.Text
}

private fun QrType.secondaryKeyboardType(): KeyboardType = when (this) {
    QrType.LOCATION -> KeyboardType.Decimal
    QrType.CONTACT -> KeyboardType.Phone
    else -> KeyboardType.Text
}

private fun primaryLabel(type: QrType): String = when (type) {
    QrType.TEXT -> "Texto"
    QrType.URL -> "Endereço (URL)"
    QrType.PHONE -> "Telefone"
    QrType.PIX -> "Código PIX copia e cola"
    QrType.APP_LINK -> "Link do aplicativo"
    QrType.CUSTOM -> "Informação personalizada"
    else -> "Conteúdo"
}
