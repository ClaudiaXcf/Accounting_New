package com.accounting.newapp.export

import com.accounting.newapp.data.ConfirmationStatus
import com.accounting.newapp.data.TransactionEntity
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class TransactionExporterTest {
    @Test
    fun csvContainsHeadersAndMerchant() {
        val output = ByteArrayOutputStream()

        TransactionExporter.writeCsv(listOf(sampleTransaction()), output)

        val csv = output.toString(Charsets.UTF_8.name())
        assertTrue(csv.contains("时间,金额,商户"))
        assertTrue(csv.contains("星巴克"))
    }

    @Test
    fun xlsxContainsThreeSheets() {
        val output = ByteArrayOutputStream()

        TransactionExporter.writeXlsx(listOf(sampleTransaction()), output)

        val entries = mutableListOf<String>()
        ZipInputStream(output.toByteArray().inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.forEach { entries.add(it.name) }
        }
        assertTrue(entries.contains("xl/worksheets/sheet1.xml"))
        assertTrue(entries.contains("xl/worksheets/sheet2.xml"))
        assertTrue(entries.contains("xl/worksheets/sheet3.xml"))
    }

    private fun sampleTransaction() = TransactionEntity(
        amountCents = 1230,
        merchant = "星巴克",
        category = "餐饮",
        sourceApp = "微信支付",
        paymentMethod = "微信支付",
        occurredAtMillis = 1_780_000_000_000,
        confidence = 0.9f,
        status = ConfirmationStatus.Confirmed,
    )
}

