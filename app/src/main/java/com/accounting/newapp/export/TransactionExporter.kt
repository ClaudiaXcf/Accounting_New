package com.accounting.newapp.export

import com.accounting.newapp.data.TransactionEntity
import com.accounting.newapp.report.formatMoney
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object TransactionExporter {
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault())

    fun writeCsv(transactions: List<TransactionEntity>, outputStream: OutputStream) {
        outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
            writer.appendLine("时间,金额,商户,分类,来源,支付方式,状态,备注")
            transactions.forEach { transaction ->
                writer.appendLine(
                    listOf(
                        dateFormatter.format(Instant.ofEpochMilli(transaction.occurredAtMillis)),
                        transaction.amountCents.formatMoney(),
                        transaction.merchant,
                        transaction.category,
                        transaction.sourceApp,
                        transaction.paymentMethod,
                        transaction.status.name,
                        transaction.note,
                    ).joinToString(",") { it.csvEscape() }
                )
            }
        }
    }

    fun writeXlsx(transactions: List<TransactionEntity>, outputStream: OutputStream) {
        ZipOutputStream(outputStream).use { zip ->
            zip.putText("[Content_Types].xml", contentTypes)
            zip.putText("_rels/.rels", rels)
            zip.putText("xl/workbook.xml", workbook)
            zip.putText("xl/_rels/workbook.xml.rels", workbookRels)
            zip.putText("xl/worksheets/sheet1.xml", worksheet("账单明细", transactionRows(transactions)))
            zip.putText("xl/worksheets/sheet2.xml", worksheet("分类汇总", categoryRows(transactions)))
            zip.putText("xl/worksheets/sheet3.xml", worksheet("月度汇总", monthRows(transactions)))
        }
    }

    private fun transactionRows(transactions: List<TransactionEntity>): List<List<String>> {
        return listOf(listOf("时间", "金额", "商户", "分类", "来源", "支付方式", "状态", "备注")) +
            transactions.map {
                listOf(
                    dateFormatter.format(Instant.ofEpochMilli(it.occurredAtMillis)),
                    it.amountCents.formatMoney(),
                    it.merchant,
                    it.category,
                    it.sourceApp,
                    it.paymentMethod,
                    it.status.name,
                    it.note,
                )
            }
    }

    private fun categoryRows(transactions: List<TransactionEntity>): List<List<String>> {
        val totals = transactions.groupBy { it.category }.mapValues { entry -> entry.value.sumOf { it.amountCents } }
        return listOf(listOf("分类", "金额")) + totals.entries.sortedByDescending { it.value }
            .map { listOf(it.key, it.value.formatMoney()) }
    }

    private fun monthRows(transactions: List<TransactionEntity>): List<List<String>> {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM").withZone(ZoneId.systemDefault())
        val totals = transactions.groupBy { formatter.format(Instant.ofEpochMilli(it.occurredAtMillis)) }
            .mapValues { entry -> entry.value.sumOf { it.amountCents } }
        return listOf(listOf("月份", "金额")) + totals.entries.sortedBy { it.key }
            .map { listOf(it.key, it.value.formatMoney()) }
    }

    private fun worksheet(name: String, rows: List<List<String>>): String {
        val rowsXml = rows.mapIndexed { rowIndex, row ->
            val cells = row.mapIndexed { colIndex, value ->
                val cell = "${columnName(colIndex)}${rowIndex + 1}"
                """<c r="$cell" t="inlineStr"><is><t>${value.xmlEscape()}</t></is></c>"""
            }.joinToString("")
            """<row r="${rowIndex + 1}">$cells</row>"""
        }.joinToString("")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
    <sheetPr><tabColor rgb="FF007AFF"/></sheetPr>
    <dimension ref="A1:H${rows.size}"/>
    <sheetViews><sheetView workbookViewId="0"/></sheetViews>
    <sheetFormatPr defaultRowHeight="18"/>
    <sheetData>$rowsXml</sheetData>
</worksheet>""".trimIndent()
    }

    private fun columnName(index: Int): String = ('A'.code + index).toChar().toString()

    private fun ZipOutputStream.putText(path: String, value: String) {
        putNextEntry(ZipEntry(path))
        write(value.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun String.csvEscape(): String = "\"${replace("\"", "\"\"")}\""
    private fun String.xmlEscape(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private const val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
    <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
    <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

    private const val rels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val workbook = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
    <sheets>
        <sheet name="账单明细" sheetId="1" r:id="rId1"/>
        <sheet name="分类汇总" sheetId="2" r:id="rId2"/>
        <sheet name="月度汇总" sheetId="3" r:id="rId3"/>
    </sheets>
</workbook>"""

    private const val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
    <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
    <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
</Relationships>"""
}

