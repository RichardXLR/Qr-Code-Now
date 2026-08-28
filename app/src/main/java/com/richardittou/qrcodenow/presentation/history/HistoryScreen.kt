package com.richardittou.qrcodenow.presentation.history

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Launch
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import com.richardittou.qrcodenow.data.database.HistoryEntity
import com.richardittou.qrcodenow.domain.action.AndroidExternalActionLauncher
import com.richardittou.qrcodenow.domain.parser.DefaultQrContentParser
import com.richardittou.qrcodenow.domain.model.QrType
import com.richardittou.qrcodenow.domain.model.ScanOrigin
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HistoryScreen(favoritesOnly: Boolean, viewModel: HistoryViewModel = hiltViewModel()) {
    LaunchedEffect(favoritesOnly) { viewModel.setFavoritesOnly(favoritesOnly) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var clearDialog by remember { mutableStateOf(false) }
    var opened by remember { mutableStateOf<HistoryEntity?>(null) }
    var typeMenu by remember { mutableStateOf(false) }
    var originMenu by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.deletions.collect { deletion ->
            val count = deletion.items.size
            val result = snackbarHostState.showSnackbar(
                message = if (count == 1) "Item excluído" else "$count itens excluídos",
                actionLabel = "Desfazer",
                withDismissAction = true
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.undo(deletion)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.selectionMode) "${state.selectedIds.size} selecionado(s)"
                        else if (favoritesOnly) "Favoritos" else "Histórico"
                    )
                },
                navigationIcon = {
                    if (state.selectionMode) IconButton(onClick = viewModel::clearSelection) {
                        Icon(Icons.Outlined.Close, "Sair da seleção")
                    }
                },
                actions = {
                    if (state.selectionMode) {
                        IconButton(onClick = viewModel::selectAll) { Icon(Icons.Outlined.SelectAll, "Selecionar todos") }
                        IconButton(onClick = viewModel::deleteSelected, enabled = state.selectedIds.isNotEmpty()) {
                            Icon(Icons.Outlined.Delete, "Excluir selecionados")
                        }
                    } else if (!favoritesOnly && state.items.isNotEmpty()) {
                        IconButton(onClick = viewModel::beginSelection) { Icon(Icons.Outlined.Checklist, "Selecionar itens") }
                        IconButton(onClick = { clearDialog = true }) { Icon(Icons.Outlined.Delete, "Limpar histórico") }
                    } else if (state.items.isNotEmpty()) {
                        IconButton(onClick = viewModel::beginSelection) { Icon(Icons.Outlined.Checklist, "Selecionar itens") }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                leadingIcon = { Icon(Icons.Outlined.Search, null) },
                label = { Text("Pesquisar") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    OutlinedButton(onClick = { typeMenu = true }) {
                        Text(state.typeFilter?.label ?: "Todos os tipos")
                        Icon(Icons.Outlined.ArrowDropDown, null)
                    }
                    DropdownMenu(typeMenu, onDismissRequest = { typeMenu = false }) {
                        DropdownMenuItem(text = { Text("Todos os tipos") }, onClick = {
                            viewModel.setTypeFilter(null); typeMenu = false
                        })
                        QrType.entries.forEach { type ->
                            DropdownMenuItem(text = { Text(type.label) }, onClick = {
                                viewModel.setTypeFilter(type); typeMenu = false
                            })
                        }
                    }
                }
                Column {
                    OutlinedButton(onClick = { originMenu = true }) {
                        Text(state.originFilter?.label() ?: "Todas as origens")
                        Icon(Icons.Outlined.ArrowDropDown, null)
                    }
                    DropdownMenu(originMenu, onDismissRequest = { originMenu = false }) {
                        DropdownMenuItem(text = { Text("Todas as origens") }, onClick = {
                            viewModel.setOriginFilter(null); originMenu = false
                        })
                        ScanOrigin.entries.forEach { origin ->
                            DropdownMenuItem(text = { Text(origin.label()) }, onClick = {
                                viewModel.setOriginFilter(origin); originMenu = false
                            })
                        }
                    }
                }
                FilterChip(
                    selected = state.oldestFirst,
                    onClick = viewModel::toggleSort,
                    label = { Text(if (state.oldestFirst) "Mais antigos" else "Mais recentes") },
                    leadingIcon = { Icon(Icons.Outlined.SwapVert, null) }
                )
            }
            if (state.items.isEmpty()) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(if (favoritesOnly) Icons.Outlined.FavoriteBorder else Icons.Outlined.Search, null)
                    Text(if (favoritesOnly) "Seus favoritos aparecerão aqui" else "Nenhuma leitura encontrada", modifier = Modifier.padding(top = 12.dp))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.items, key = { it.id }) { item ->
                        HistoryCard(
                            item = item,
                            selected = item.id in state.selectedIds,
                            selectionMode = state.selectionMode,
                            onOpen = { opened = item },
                            onSelect = { viewModel.toggleSelection(item.id) },
                            onFavorite = { viewModel.toggleFavorite(item) },
                            onDelete = { viewModel.delete(item.id) }
                        )
                    }
                }
            }
        }
    }

    if (clearDialog) AlertDialog(
        onDismissRequest = { clearDialog = false },
        title = { Text("Limpar todo o histórico?") },
        text = { Text("Todos os itens, inclusive favoritos, serão excluídos permanentemente.") },
        confirmButton = { TextButton(onClick = { viewModel.clearHistory(); clearDialog = false }) { Text("Excluir tudo") } },
        dismissButton = { TextButton(onClick = { clearDialog = false }) { Text("Cancelar") } }
    )
    opened?.let { HistoryDetails(it, onDismiss = { opened = null }) }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryCard(
    item: HistoryEntity,
    selected: Boolean,
    selectionMode: Boolean,
    onOpen: () -> Unit,
    onSelect: () -> Unit,
    onFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        Modifier.fillMaxWidth().combinedClickable(
            onClick = { if (selectionMode) onSelect() else onOpen() },
            onLongClick = onSelect
        )
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            if (selectionMode) Checkbox(selected, onCheckedChange = { onSelect() })
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(item.typeLabel(), color = MaterialTheme.colorScheme.primary)
                Text(formatDate(item.scannedAt), style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onFavorite) {
                Icon(if (item.favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, "Favorito", tint = if (item.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, "Excluir item") }
        }
    }
}

@Composable
private fun HistoryDetails(item: HistoryEntity, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val launcher = remember { AndroidExternalActionLauncher() }
    val parsed = remember(item.rawContent) { DefaultQrContentParser().parse(item.rawContent) }
    var error by remember { mutableStateOf(false) }
    val wifiSupported = parsed !is com.richardittou.qrcodenow.domain.model.QrContent.Wifi ||
        parsed.security.uppercase() in setOf("", "NOPASS", "OPEN", "WPA", "WPA2", "WPA/WPA2", "WPA3", "SAE")
    val wifiPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && parsed is com.richardittou.qrcodenow.domain.model.QrContent.Wifi) {
            launcher.connectWifi(context, parsed).onFailure { error = true }
        } else if (!granted) {
            error = true
        }
    }
    val canOpen = parsed is com.richardittou.qrcodenow.domain.model.QrContent.Url ||
        parsed is com.richardittou.qrcodenow.domain.model.QrContent.Phone ||
        parsed is com.richardittou.qrcodenow.domain.model.QrContent.Sms ||
        parsed is com.richardittou.qrcodenow.domain.model.QrContent.Email ||
        parsed is com.richardittou.qrcodenow.domain.model.QrContent.Location ||
        parsed is com.richardittou.qrcodenow.domain.model.QrContent.AppLink ||
        (parsed is com.richardittou.qrcodenow.domain.model.QrContent.Wifi && wifiSupported)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(item.typeLabel(), color = MaterialTheme.colorScheme.primary)
                if (parsed is com.richardittou.qrcodenow.domain.model.QrContent.Url && parsed.suspicious) {
                    Text("Atenção: confira este endereço antes de abrir.", color = MaterialTheme.colorScheme.error)
                }
                if (parsed is com.richardittou.qrcodenow.domain.model.QrContent.AppLink) {
                    Text("Este link usa o esquema ${parsed.scheme}. Abra somente se reconhecer a origem.", color = MaterialTheme.colorScheme.error)
                }
                if (parsed is com.richardittou.qrcodenow.domain.model.QrContent.Wifi && !wifiSupported) {
                    Text("Esta segurança Wi-Fi não permite conexão direta pelo Android.", color = MaterialTheme.colorScheme.error)
                }
                Text(item.rawContent, maxLines = 10)
                if (error) Text("Não foi possível abrir essa ação.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Row {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("QR Code", item.rawContent))
                }) { Icon(Icons.Outlined.ContentCopy, "Copiar") }
                IconButton(onClick = { launcher.shareText(context, item.rawContent).onFailure { error = true } }) { Icon(Icons.Outlined.Share, "Compartilhar") }
                if (canOpen) IconButton(onClick = {
                    val result = if (parsed is com.richardittou.qrcodenow.domain.model.QrContent.Wifi) {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED) {
                            launcher.connectWifi(context, parsed)
                        } else {
                            wifiPermissionLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
                            Result.success(Unit)
                        }
                    } else {
                        launcher.open(context, parsed)
                    }
                    result.onFailure { error = true }
                }) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Launch,
                        if (parsed is com.richardittou.qrcodenow.domain.model.QrContent.Wifi) "Conectar" else "Abrir"
                    )
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy • HH:mm")
private fun formatDate(epochMillis: Long): String = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(dateFormatter)
private fun HistoryEntity.typeLabel(): String = runCatching { QrType.valueOf(type).label }.getOrDefault("Conteúdo")
private fun ScanOrigin.label(): String = when (this) {
    ScanOrigin.CAMERA -> "Câmera"
    ScanOrigin.GALLERY -> "Imagem"
    ScanOrigin.GENERATED -> "Criados"
}
