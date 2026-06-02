package com.accounting.newapp

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LightMode
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
    MaterialTheme(
        colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme(),
        shapes = MaterialTheme.shapes.copy(
            small = RoundedCornerShape(8.dp),
            medium = RoundedCornerShape(8.dp),
            large = RoundedCornerShape(10.dp),
        ),
    ) {
        var destination by remember { mutableStateOf(Destination.Home) }
        Scaffold(
            topBar = { AppTopBar(destination.label) },
            bottomBar = {
                NavigationBar {
                    Destination.entries.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            },
        ) { padding ->
            Surface(Modifier.fillMaxSize().padding(padding)) {
                when (destination) {
                    Destination.Home -> HomeScreen(viewModel)
                    Destination.Bills -> BillsScreen(viewModel)
                    Destination.Reports -> ReportsScreen(viewModel)
                    Destination.Settings -> SettingsScreen(viewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppTopBar(title: String) {
    CenterAlignedTopAppBar(title = { Text(title, fontWeight = FontWeight.SemiBold) })
}

@Composable
private fun HomeScreen(viewModel: AppViewModel) {
    val state by viewModel.homeUiState.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SummaryPanel(
                title = "今日消费",
                value = state.todayTotalCents.formatMoney(),
                subtitle = if (state.pendingTransactions.isEmpty()) "记录已同步" else "${state.pendingTransactions.size} 笔待确认",
            )
        }
        item { SectionTitle("最近记录") }
        items(state.recentTransactions) { transaction ->
            TransactionRow(transaction = transaction, onClick = {})
        }
        if (state.recentTransactions.isEmpty()) {
            item { EmptyState("开启权限后，支付完成会自动出现在这里。") }
        }
    }
}

@Composable
private fun BillsScreen(viewModel: AppViewModel) {
    val transactions by viewModel.transactions.collectAsState()
    var editing by remember { mutableStateOf<TransactionEntity?>(null) }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { SectionTitle("全部账单") }
        items(transactions, key = { it.id }) { transaction ->
            TransactionRow(transaction = transaction, onClick = { editing = transaction })
        }
        if (transactions.isEmpty()) {
            item { EmptyState("还没有账单。可以先完成一次支付，或等通知识别自动生成。") }
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
private fun ReportsScreen(viewModel: AppViewModel) {
    val state by viewModel.reportUiState.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                ReportPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = state.period == period,
                        onClick = { viewModel.setPeriod(period) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = ReportPeriod.entries.size),
                    ) {
                        Text(period.label)
                    }
                }
            }
        }
        item {
            SummaryPanel(title = "${state.period.label}支出", value = state.totalCents.formatMoney(), subtitle = "按已记录账单实时统计")
        }
        item { SectionTitle("分类占比") }
        items(state.categories) { total ->
            BarRow(label = total.category, amount = total.amountCents, max = state.categories.maxOfOrNull { it.amountCents } ?: 0)
        }
        item { SectionTitle("商户排行") }
        items(state.merchants) { total ->
            BarRow(label = total.merchant, amount = total.amountCents, max = state.merchants.maxOfOrNull { it.amountCents } ?: 0)
        }
        if (state.categories.isEmpty()) {
            item { EmptyState("当前周期还没有消费记录。") }
        }
    }
}

@Composable
private fun SettingsScreen(viewModel: AppViewModel) {
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

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { SectionTitle("权限") }
        item {
            ActionCard(
                icon = Icons.Rounded.Security,
                title = "无障碍支付识别",
                subtitle = "开启后可识别支付完成页面。",
                action = "去开启",
                onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            )
        }
        item {
            ActionCard(
                icon = Icons.Rounded.Sync,
                title = "通知读取降级识别",
                subtitle = "支付页面未捕获时，尝试读取支付成功通知。",
                action = "去开启",
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            )
        }
        item { SectionTitle("主题") }
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
        item { SectionTitle("导出") }
        item {
            ActionCard(Icons.Rounded.Download, "导出 CSV", "保存账单明细，适合表格软件打开。", "导出") {
                csvLauncher.launch("自动记账-${System.currentTimeMillis()}.csv")
            }
        }
        item {
            ActionCard(Icons.Rounded.Download, "导出 Excel", "包含账单明细、分类汇总和月度汇总。", "导出") {
                xlsxLauncher.launch("自动记账-${System.currentTimeMillis()}.xlsx")
            }
        }
    }
}

@Composable
private fun SummaryPanel(title: String, value: String, subtitle: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(subtitle, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
        }
    }
}

@Composable
private fun TransactionRow(transaction: TransactionEntity, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(transaction.category.take(1), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(transaction.merchant, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text("${transaction.category} · ${transaction.sourceApp} · ${transaction.occurredAtMillis.formatDate()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(transaction.amountCents.formatMoney(), fontWeight = FontWeight.Bold)
                if (transaction.status == ConfirmationStatus.Pending) {
                    Text("待确认", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun BarRow(label: String, amount: Long, max: Long) {
    val fraction = if (max <= 0) 0f else amount.toFloat() / max
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text(amount.formatMoney(), fontWeight = FontWeight.SemiBold)
        }
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            drawRoundRect(Color(0xFFE5E5EA), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f))
            drawRoundRect(Color(0xFF007AFF), size = size.copy(width = size.width * fraction), cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f))
        }
    }
}

@Composable
private fun ActionCard(icon: ImageVector, title: String, subtitle: String, action: String, onClick: () -> Unit) {
    Card {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onClick) { Text(action) }
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
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun EmptyState(text: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    secondary = Color(0xFF34C759),
    tertiary = Color(0xFFFF9F0A),
    background = Color(0xFFF5F5F7),
    surface = Color.White,
    surfaceVariant = Color(0xFFE5E5EA),
)

private fun darkColorScheme() = androidx.compose.material3.darkColorScheme(
    primary = Color(0xFF0A84FF),
    secondary = Color(0xFF30D158),
    tertiary = Color(0xFFFFB340),
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
)
