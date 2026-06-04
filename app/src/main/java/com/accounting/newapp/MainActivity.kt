package com.accounting.newapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Assessment
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Message
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.accounting.newapp.data.ConfirmationStatus
import com.accounting.newapp.data.ThemeMode
import com.accounting.newapp.data.TransactionEntity
import com.accounting.newapp.export.TransactionExporter
import com.accounting.newapp.report.ReportPeriod
import com.accounting.newapp.report.formatMoney
import com.accounting.newapp.ui.AppViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: AppViewModel = viewModel(factory = AppViewModel.Factory(application))
            AccountingApp(viewModel)
        }
    }
}

private enum class Destination(val label: String, val icon: ImageVector) {
    Home("首页", Icons.Rounded.Home),
    Bills("账单", Icons.AutoMirrored.Rounded.List),
    Reports("报表", Icons.Rounded.Assessment),
    Trash("回收站", Icons.Rounded.Delete),
    Settings("设置", Icons.Rounded.Settings),
}

@Composable
private fun AccountingApp(viewModel: AppViewModel) {
    val themeMode by viewModel.themeMode.collectAsState()
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()

    MaterialTheme(
        colorScheme = colorScheme,
        shapes = MaterialTheme.shapes.copy(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
        ),
    ) {
        var destination by remember { mutableStateOf(Destination.Home) }

        val backgroundGradient = if (darkTheme) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF000000),
                    Color(0xFF1C1C1E),
                    Color(0xFF2C2C2E),
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFF5F5F7),
                    Color(0xFFE8E8ED),
                    Color(0xFFDDDDE6),
                )
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    GlassTopAppBar(
                        title = destination.label,
                        isDark = darkTheme,
                    )
                },
                bottomBar = {
                    GlassNavigationBar(
                        isDark = darkTheme,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        NavigationBar(
                            containerColor = Color.Transparent,
                        ) {
                            Destination.entries.forEach { item ->
                                NavigationBarItem(
                                    selected = destination == item,
                                    onClick = { destination = item },
                                    icon = { Icon(item.icon, contentDescription = item.label) },
                                    label = { Text(item.label) },
                                )
                            }
                        }
                    }
                },
            ) { padding ->
                Surface(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .background(Color.Transparent),
                ) {
                    when (destination) {
                        Destination.Home -> HomeScreen(viewModel, darkTheme)
                        Destination.Bills -> BillsScreen(viewModel, darkTheme)
                        Destination.Reports -> ReportsScreen(viewModel, darkTheme)
                        Destination.Trash -> TrashScreen(viewModel, darkTheme)
                        Destination.Settings -> SettingsScreen(viewModel, darkTheme)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GlassTopAppBar(title: String, isDark: Boolean) {
    val backgroundColor = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1C1C1E).copy(alpha = 0.85f),
                Color(0xFF1C1C1E).copy(alpha = 0.7f),
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.85f),
                Color.White.copy(alpha = 0.7f),
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            fontWeight = FontWeight.SemiBold,
            color = if (isDark) Color.White else Color(0xFF1C1C1E)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(title: String) {
    CenterAlignedTopAppBar(title = { Text(title, fontWeight = FontWeight.SemiBold) })
}

@Composable
private fun HomeScreen(viewModel: AppViewModel, isDark: Boolean) {
    val state by viewModel.homeUiState.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GlassSummaryPanel(
                title = "今日消费",
                value = state.todayTotalCents.formatMoney(),
                subtitle = if (state.pendingTransactions.isEmpty()) "记录已同步" else "${state.pendingTransactions.size} 笔待确认",
                isDark = isDark,
            )
        }
        item { SectionTitle("最近记录", isDark) }
        items(state.recentTransactions) { transaction ->
            GlassTransactionRow(transaction = transaction, onClick = {}, isDark = isDark)
        }
        if (state.recentTransactions.isEmpty()) {
            item { EmptyState("开启权限后，支付完成会自动出现在这里。", isDark) }
        }
    }
}

@Composable
private fun BillsScreen(viewModel: AppViewModel, isDark: Boolean) {
    val transactions by viewModel.transactions.collectAsState()
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SectionTitle("全部账单", isDark) }
        items(transactions, key = { it.id }) { transaction ->
            GlassTransactionRow(transaction = transaction, onClick = { editing = transaction }, isDark = isDark)
        }
        if (transactions.isEmpty()) {
            item { EmptyState("还没有账单。可以先完成一次支付，或等通知识别自动生成。", isDark) }
        }
    }
    editing?.let { transaction ->
        EditTransactionDialog(
            transaction = transaction,
            onDismiss = { editing = null },
            onSave = {
                viewModel.updateTransaction(it.copy(status = ConfirmationStatus.Confirmed))
                editing = null
            },
            onDelete = {
                viewModel.deleteTransaction(transaction)
                editing = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportsScreen(viewModel: AppViewModel, isDark: Boolean) {
    val state by viewModel.reportUiState.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            GlassSegmentedButtons(
                selectedPeriod = state.period,
                onPeriodSelected = { viewModel.setPeriod(it) },
                isDark = isDark,
            )
        }
        item {
            GlassSummaryPanel(
                title = "${state.period.label}支出",
                value = state.totalCents.formatMoney(),
                subtitle = "按已记录账单实时统计",
                isDark = isDark,
            )
        }
        item { SectionTitle("分类占比", isDark) }
        items(state.categories) { total ->
            GlassBarRow(label = total.category, amount = total.amountCents, max = state.categories.maxOfOrNull { it.amountCents } ?: 0, isDark = isDark)
        }
        item { SectionTitle("商户排行", isDark) }
        items(state.merchants) { total ->
            GlassBarRow(label = total.merchant, amount = total.amountCents, max = state.merchants.maxOfOrNull { it.amountCents } ?: 0, isDark = isDark)
        }
        if (state.categories.isEmpty()) {
            item { EmptyState("当前周期还没有消费记录。", isDark) }
        }
    }
}

@Composable
private fun TrashScreen(viewModel: AppViewModel, isDark: Boolean) {
    val trashItems by viewModel.trashTransactions.collectAsState()
    var deleting by remember { mutableStateOf<TransactionEntity?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SectionTitle("回收站", isDark) }
        if (trashItems.isEmpty()) {
            item { EmptyState("回收站是空的，删除的记录会在这里保留 30 天。", isDark) }
        }
        items(trashItems, key = { it.id }) { transaction ->
            TrashTransactionRow(
                transaction = transaction,
                isDark = isDark,
                onRestore = { viewModel.restoreTransaction(transaction) },
                onPermanentDelete = { deleting = transaction },
            )
        }
    }
    deleting?.let { transaction ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("永久删除") },
            text = { Text("此操作不可恢复，确定要永久删除这条记录吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.permanentlyDeleteTransaction(transaction)
                    deleting = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun TrashTransactionRow(
    transaction: TransactionEntity,
    isDark: Boolean,
    onRestore: () -> Unit,
    onPermanentDelete: () -> Unit,
) {
    val backgroundColor = if (isDark) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF2C2C2E).copy(alpha = 0.6f),
                Color(0xFF2C2C2E).copy(alpha = 0.3f),
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.8f),
                Color.White.copy(alpha = 0.5f),
            )
        )
    }
    val textColor = if (isDark) Color.White else Color(0xFF1C1C1E)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        transaction.merchant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        "${transaction.amountCents.formatMoney()} · ${transaction.occurredAtMillis.formatDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF636366)
                    )
                }
                TextButton(onClick = onRestore) {
                    Icon(Icons.Rounded.RestoreFromTrash, contentDescription = "恢复", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("恢复")
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: AppViewModel, isDark: Boolean) {
    val context = LocalContext.current
    val themeMode by viewModel.themeMode.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.use { stream -> TransactionExporter.writeCsv(transactions, stream) } }
    }
    val xlsxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.use { stream -> TransactionExporter.writeXlsx(transactions, stream) } }
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* 用户授权或拒绝后无需额外操作，状态会自动刷新 */ }

    val hasSmsPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
    } else {
        true // Android 12 及以下 RECEIVE_SMS 在安装时已授予
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SectionTitle("权限", isDark) }
        item {
            GlassActionCard(
                icon = Icons.Rounded.Security,
                title = "无障碍支付识别",
                subtitle = "开启后可识别支付完成页面。",
                action = "去开启",
                isDark = isDark,
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            )
        }
        item {
            GlassActionCard(
                icon = Icons.Rounded.Sync,
                title = "通知读取降级识别",
                subtitle = "支付页面未捕获时，尝试读取支付成功通知。",
                action = "去开启",
                isDark = isDark,
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            )
        }
        item {
            val smsAction = if (hasSmsPermission) "已开启" else "去开启"
            val smsSubtitle = if (hasSmsPermission) "银行短信将自动识别为账单。" else "开启后可从银行短信自动识别消费记录。"
            GlassActionCard(
                icon = Icons.Rounded.Message,
                title = "银行短信自动记账",
                subtitle = smsSubtitle,
                action = smsAction,
                isDark = isDark,
                onClick = {
                    if (!hasSmsPermission) {
                        smsPermissionLauncher.launch(Manifest.permission.RECEIVE_SMS)
                    }
                },
            )
        }
        item { SectionTitle("主题", isDark) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    AssistChip(
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(mode.label) },
                        leadingIcon = {
                            Icon(
                                when (mode) {
                                    ThemeMode.System -> Icons.Rounded.Sync
                                    ThemeMode.Light -> Icons.Rounded.LightMode
                                    ThemeMode.Dark -> Icons.Rounded.DarkMode
                                },
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        enabled = themeMode != mode,
                    )
                }
            }
        }
        item { SectionTitle("导出", isDark) }
        item {
            GlassActionCard(Icons.Rounded.Download, "导出 CSV", "保存账单明细，适合表格软件打开。", "导出", isDark) {
                csvLauncher.launch("自动记账-${System.currentTimeMillis()}.csv")
            }
        }
        item {
            GlassActionCard(Icons.Rounded.Download, "导出 Excel", "包含账单明细、分类汇总和月度汇总。", "导出", isDark) {
                xlsxLauncher.launch("自动记账-${System.currentTimeMillis()}.xlsx")
            }
        }
    }
}

@Composable
private fun GlassSummaryPanel(title: String, value: String, subtitle: String, isDark: Boolean) {
    val backgroundBrush = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0A84FF).copy(alpha = 0.25f),
                Color(0xFF0A84FF).copy(alpha = 0.1f),
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF007AFF).copy(alpha = 0.15f),
                Color(0xFF007AFF).copy(alpha = 0.05f),
            )
        )
    }
    val textColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.6f) else Color(0xFF1C1C1E).copy(alpha = 0.6f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundBrush)
                .padding(24.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = textColor.copy(alpha = 0.8f))
                Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = textColor)
                Text(subtitle, color = subtitleColor)
            }
        }
    }
}

@Composable
private fun GlassTransactionRow(transaction: TransactionEntity, onClick: () -> Unit, isDark: Boolean) {
    val backgroundColor = if (isDark) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF2C2C2E).copy(alpha = 0.6f),
                Color(0xFF2C2C2E).copy(alpha = 0.3f),
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.8f),
                Color.White.copy(alpha = 0.5f),
            )
        )
    }
    val textColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val subtitleColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF636366)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isDark) Color(0xFF30D158).copy(alpha = 0.2f)
                            else Color(0xFF34C759).copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        transaction.category.take(1),
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color(0xFF30D158) else Color(0xFF34C759)
                    )
                }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        transaction.merchant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        color = textColor
                    )
                    Text(
                        "${transaction.category} · ${transaction.sourceApp} · ${transaction.occurredAtMillis.formatDate()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = subtitleColor
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(transaction.amountCents.formatMoney(), fontWeight = FontWeight.Bold, color = textColor)
                    if (transaction.status == ConfirmationStatus.Pending) {
                        Text("待确认", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF3B30))
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassBarRow(label: String, amount: Long, max: Long, isDark: Boolean) {
    val fraction = if (max <= 0) 0f else amount.toFloat() / max
    val textColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val trackColor = if (isDark) Color(0xFF3A3A3C) else Color(0xFFE5E5EA)
    val progressColor = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f), color = textColor)
            Text(amount.formatMoney(), fontWeight = FontWeight.SemiBold, color = textColor)
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
        ) {
            drawRoundRect(trackColor, cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f))
            drawRoundRect(
                progressColor,
                size = size.copy(width = size.width * fraction),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5f, 5f)
            )
        }
    }
}

@Composable
private fun GlassActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    action: String,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isDark) {
        Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF2C2C2E).copy(alpha = 0.6f),
                Color(0xFF2C2C2E).copy(alpha = 0.3f),
            )
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.8f),
                Color.White.copy(alpha = 0.5f),
            )
        )
    }
    val textColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    val subtitleTextColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF636366)
    val iconColor = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = iconColor)
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(title, fontWeight = FontWeight.SemiBold, color = textColor)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = subtitleTextColor)
                }
                Button(onClick = onClick) { Text(action) }
            }
        }
    }
}

@Composable
private fun GlassSegmentedButtons(
    selectedPeriod: ReportPeriod,
    onPeriodSelected: (ReportPeriod) -> Unit,
    isDark: Boolean
) {
    val selectedColor = if (isDark) Color(0xFF0A84FF) else Color(0xFF007AFF)
    val unselectedColor = if (isDark) Color(0xFF2C2C2E) else Color.White
    val selectedTextColor = Color.White
    val unselectedTextColor = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF1C1C1E)

    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        ReportPeriod.entries.forEachIndexed { index, period ->
            val isSelected = selectedPeriod == period
            SegmentedButton(
                selected = isSelected,
                onClick = { onPeriodSelected(period) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = ReportPeriod.entries.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = selectedColor,
                    activeContentColor = selectedTextColor,
                    inactiveContainerColor = unselectedColor,
                    inactiveContentColor = unselectedTextColor,
                ),
            ) {
                Text(period.label)
            }
        }
    }
}

@Composable
private fun EditTransactionDialog(
    transaction: TransactionEntity,
    onDismiss: () -> Unit,
    onSave: (TransactionEntity) -> Unit,
    onDelete: () -> Unit,
) {
    var merchant by remember { mutableStateOf(transaction.merchant) }
    var category by remember { mutableStateOf(transaction.category) }
    var note by remember { mutableStateOf(transaction.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑账单") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = merchant, onValueChange = { merchant = it }, label = { Text("商户") })
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("分类") })
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注") })
            }
        },
        confirmButton = { TextButton(onClick = { onSave(transaction.copy(merchant = merchant, category = category, note = note)) }) { Text("保存") } },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
    )
}

@Composable
private fun SectionTitle(text: String, isDark: Boolean) {
    val textColor = if (isDark) Color.White else Color(0xFF1C1C1E)
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = textColor
    )
}

@Composable
private fun EmptyState(text: String, isDark: Boolean) {
    val backgroundColor = if (isDark) Color(0xFF2C2C2E).copy(alpha = 0.5f) else Color(0xFFE5E5EA).copy(alpha = 0.5f)
    val textColor = if (isDark) Color.White.copy(alpha = 0.5f) else Color(0xFF636366)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(18.dp)
        ) {
            Text(text, color = textColor)
        }
    }
}

private val ReportPeriod.label: String
    get() = when (this) {
        ReportPeriod.Day -> "日"
        ReportPeriod.Week -> "周"
        ReportPeriod.Month -> "月"
        ReportPeriod.Year -> "年"
    }

private val ThemeMode.label: String
    get() = when (this) {
        ThemeMode.System -> "跟随系统"
        ThemeMode.Light -> "浅色"
        ThemeMode.Dark -> "深色"
    }

private fun Long.formatDate(): String = DateTimeFormatter.ofPattern("MM-dd HH:mm")
    .withZone(ZoneId.systemDefault())
    .format(Instant.ofEpochMilli(this))

private fun lightColorScheme() = androidx.compose.material3.lightColorScheme(
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF007AFF).copy(alpha = 0.12f),
    onPrimaryContainer = Color(0xFF007AFF),
    secondary = Color(0xFF34C759),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF34C759).copy(alpha = 0.12f),
    onSecondaryContainer = Color(0xFF34C759),
    tertiary = Color(0xFFFF9F0A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFF9F0A).copy(alpha = 0.12f),
    onTertiaryContainer = Color(0xFFFF9F0A),
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF1C1C1E),
    surface = Color.White.copy(alpha = 0.72f),
    onSurface = Color(0xFF1C1C1E),
    surfaceVariant = Color.White.copy(alpha = 0.5f),
    onSurfaceVariant = Color(0xFF636366),
    outline = Color(0xFFD1D1D6),
    outlineVariant = Color(0xFFE5E5EA),
)

private fun darkColorScheme() = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF0A84FF).copy(alpha = 0.2f),
    onPrimaryContainer = Color(0xFF0A84FF),
    secondary = Color(0xFF30D158),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF30D158).copy(alpha = 0.2f),
    onSecondaryContainer = Color(0xFF30D158),
    tertiary = Color(0xFFFFB340),
    onTertiary = Color.Black,
    tertiaryContainer = Color(0xFFFFB340).copy(alpha = 0.2f),
    onTertiaryContainer = Color(0xFFFFB340),
    background = Color(0xFF000000),
    onBackground = Color.White,
    surface = Color(0xFF1C1C1E).copy(alpha = 0.72f),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF2C2C2E).copy(alpha = 0.5f),
    onSurfaceVariant = Color(0xFF8E8E93),
    outline = Color(0xFF38383A),
    outlineVariant = Color(0xFF2C2C2E),
)

@Composable
fun GlassNavigationBar(
    modifier: Modifier = Modifier,
    isDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val backgroundColor = if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF1C1C1E).copy(alpha = 0.9f),
                Color(0xFF1C1C1E).copy(alpha = 0.85f),
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.92f),
                Color.White.copy(alpha = 0.88f),
            )
        )
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
        )
        content()
    }
}