package com.cafeos.tablet.ui.screens

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cafeos.tablet.data.BusinessSettings
import com.cafeos.tablet.data.Order
import com.cafeos.tablet.data.OrderItem
import com.cafeos.tablet.data.OrderOption
import com.cafeos.tablet.data.Payment
import com.cafeos.tablet.data.Product
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

private val PH_CURRENCY: NumberFormat = NumberFormat.getCurrencyInstance(Locale("en", "PH"))

/**
 * Structured, on-screen representation of the BIR official receipt content.
 *
 * This is the SINGLE source of the fields rendered on-screen for the victory
 * receipt. It mirrors the layout/values produced by ReceiptPrinter.generateReceiptPdf
 * so the on-screen receipt and the printed PDF never drift (CaféOS Constitution II:
 * one total, one place it is computed — `order.totalAmount` is still computed by the
 * ViewModel; here we only READ it and derive the VAT split with the identical
 * formula used by ReceiptPrinter). TODO(follow-up): migrate ReceiptPrinter to read
 * from this same model to remove the formula copy.
 */
data class ReceiptModel(
    val shopName: String,
    val address: String,
    val tin: String?,
    val orderNumber: String,
    val date: String,
    val orderType: String,
    val location: String,
    val customer: String,
    val lines: List<ReceiptLine>,
    val subtotal: Double,
    val discount: Double,
    val discountReason: String?,
    val vat: Double,
    val vatableSales: Double,
    val totalDue: Double,
    val amountInWords: String,
    val paymentMethod: String,
    val tendered: Double?,
    val change: Double?,
    val footer: String
) {
    data class ReceiptLine(val description: String, val detail: String, val amount: String)
}

fun buildReceiptModel(
    order: Order,
    orderItems: List<OrderItem>,
    products: List<Product>,
    settings: BusinessSettings?,
    orderOptionsByItem: Map<Int, List<OrderOption>> = emptyMap(),
    payment: Payment? = null
): ReceiptModel {
    val productMap = products.associateBy { it.id }
    val lines = orderItems.flatMap { item ->
        val name = productMap[item.productId]?.name ?: "Item ${item.productId}"
        val detail = "${item.quantity} x ${PH_CURRENCY.format(item.unitPrice)}"
        val amount = PH_CURRENCY.format(item.subtotal)
        val opts = orderOptionsByItem[item.id].orEmpty()
        val optRows = opts.map { o ->
            val delta = if (o.priceDelta > 0) " (${PH_CURRENCY.format(o.priceDelta)})" else ""
            ReceiptModel.ReceiptLine("      + ${o.name}: ${o.value}$delta", "", "")
        }
        listOf(ReceiptModel.ReceiptLine(name, detail, amount)) + optRows
    }

    val subtotal = orderItems.sumOf { it.subtotal }
    val discount = order.discountAmount.coerceAtLeast(0.0)
    val totalDue = order.totalAmount.coerceAtLeast(0.0)
    val rate = (settings?.vatRate ?: 0.0).coerceIn(0.0, 100.0)
    val vat = if (rate > 0.0) totalDue * rate / (100.0 + rate) else 0.0
    val vatableSales = totalDue - vat

    val orderType = when (order.orderType) {
        "DELIVERY" -> "Delivery"
        "TAKEOUT" -> "Takeout"
        else -> "Dine-in"
    }
    val location = if (order.orderType == "DELIVERY" && !order.deliveryAddress.isNullOrBlank())
        order.deliveryAddress
    else order.tableLocation ?: order.tableId?.let { "Table $it" } ?: "Takeaway"

    val paymentMethod = payment?.method
        ?: order.paymentMethod
    val tendered = if (payment?.change ?: 0.0 > 0.01) payment?.amountTendered else null
    val change = if (payment?.change ?: 0.0 > 0.01) payment?.change else null

    return ReceiptModel(
        shopName = settings?.shopName?.takeIf { it.isNotBlank() } ?: "Pebot",
        address = settings?.address ?: "CaféOS – this café's local point of sale",
        tin = settings?.tin?.takeIf { it.isNotBlank() },
        orderNumber = order.orderNumber,
        date = SimpleDateFormat("MMM dd, yyyy  hh:mm a", Locale.getDefault()).format(Date(order.createdAt)),
        orderType = orderType,
        location = location,
        customer = order.customerName.ifBlank { "Walk-in Customer" },
        lines = lines,
        subtotal = subtotal,
        discount = discount,
        discountReason = order.discountReason ?: "promo",
        vat = vat,
        vatableSales = vatableSales,
        totalDue = totalDue,
        amountInWords = moneyInWords(totalDue),
        paymentMethod = paymentMethod,
        tendered = tendered,
        change = change,
        footer = settings?.footerMessage?.takeIf { it.isNotBlank() } ?: "Thank you for your visit!"
    )
}

/** Minimal PH-peso amount-in-words (BIR requires amount in words). Mirrors the
 *  semantic of ReceiptPrinter.moneyInWords without depending on its private impl. */
internal fun moneyInWords(amount: Double): String {
    if (amount <= 0.0) return "Zero Pesos and 00/100 Only"
    val pesos = amount.toLong()
    val cents = ((amount - pesos) * 100).roundToInt().coerceIn(0, 99)
    return "${numberWords(pesos)} Pesos and ${cents.toString().padStart(2, '0')}/100 Only"
}

private fun numberWords(n: Long): String {
    if (n == 0L) return "Zero"
    val below20 = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten",
        "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
    val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Hundred" , "Ninety")
    val thousands = arrayOf("", "Thousand", "Million", "Billion")
    fun under1000(v: Long): String {
        val sb = StringBuilder()
        val h = (v / 100) % 10
        if (h > 0) sb.append(below20[h.toInt()]).append(" Hundred ")
        val t = (v / 10) % 10
        if (t >= 2) {
            sb.append(tens[t.toInt()]).append(" ")
            val u = v % 10
            if (u > 0) sb.append(below20[u.toInt()]).append(" ")
        } else {
            val u = v % 20
            if (u > 0) sb.append(below20[u.toInt()]).append(" ")
        }
        return sb.trim().toString()
    }
    val sb = StringBuilder()
    var v = n
    var tier = 0
    while (v > 0) {
        val chunk = v % 1000
        if (chunk > 0) {
            if (tier > 0) sb.insert(0, "${thousands[tier]} ")
            sb.insert(0, "${under1000(chunk)} ")
        }
        v /= 1000
        tier++
        if (tier >= thousands.size) break
    }
    return sb.trim().toString()
}

private fun money(amount: Double) = PH_CURRENCY.format(amount)

/**
 * Full BIR official receipt rendered on-screen (used inside the Victory screen).
 * All legally-required fields are present and readable; never truncated.
 */
@Composable
fun ReceiptView(
    model: ReceiptModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .background(Color.White, shape = RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        // ── Header ──
        Text(model.shopName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(model.address, style = MaterialTheme.typography.bodySmall, color = Color(0xFF666666))
        if (model.tin != null) {
            Text("VAT Reg. TIN: ${model.tin}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF666666))
        }
        Spacer(Modifier.height(8.dp))
        Divider()
        Text("OFFICIAL RECEIPT", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("This document serves as the official receipt for this transaction",
            style = MaterialTheme.typography.bodySmall, color = Color(0xFF666666),
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(8.dp)); Divider(); Spacer(Modifier.height(8.dp))

        // ── Meta ──
        MetaRow("Official Receipt No.", model.orderNumber)
        MetaRow("Date & Time", model.date)
        MetaRow("Order Type", model.orderType)
        MetaRow("Table / Location", model.location)
        MetaRow("Customer", model.customer)
        Spacer(Modifier.height(8.dp)); Divider(); Spacer(Modifier.height(8.dp))

        // ── Items ──
        Text("ITEM", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF444444))
        Text("AMOUNT", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color(0xFF444444),
            modifier = Modifier.align(Alignment.End))
        Spacer(Modifier.height(4.dp))
        for (line in model.lines) {
            ReceiptLineRow(line.description, line.detail, line.amount)
        }
        Spacer(Modifier.height(8.dp)); Divider(); Spacer(Modifier.height(12.dp))

        // ── Totals / VAT ──
        DrawTotalRow("Subtotal", money(model.subtotal), bold = false)
        if (model.discount > 0.0) {
            DrawTotalRow("Less: Discount (${model.discountReason})", "-${money(model.discount)}", bold = false)
        }
        if (model.vat > 0.0) {
            DrawTotalRow("VATable Sales", money(model.vatableSales), bold = false)
            DrawTotalRow("VAT (${((model.vat / model.totalDue) * 100).toInt()}%)", money(model.vat), bold = false)
        }
        DrawTotalRow("TOTAL DUE", money(model.totalDue), bold = true)
        Spacer(Modifier.height(8.dp))

        Text("Amount in words: ${model.amountInWords}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF444444))
        Spacer(Modifier.height(12.dp)); Divider(); Spacer(Modifier.height(4.dp))

        // ── Payment ──
        DrawTotalRow("Payment method", model.paymentMethod, bold = false)
        if (model.tendered != null) DrawTotalRow("Tendered", money(model.tendered), bold = false)
        if (model.change != null) DrawTotalRow("Change", money(model.change), bold = false)
        Spacer(Modifier.height(12.dp)); Divider(); Spacer(Modifier.height(4.dp))

        Text(model.footer, style = MaterialTheme.typography.bodySmall, color = Color(0xFF444444),
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Text("System-generated receipt · CaféOS POS", style = MaterialTheme.typography.bodySmall, color = Color(0xFF999999),
            modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun MetaRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color(0xFF444444), modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Black,
            modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun ReceiptLineRow(description: String, detail: String, amount: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(description, style = MaterialTheme.typography.bodySmall, color = Color(0xFF444444), modifier = Modifier.weight(1f))
        if (detail.isNotBlank()) {
            Text(detail, style = MaterialTheme.typography.bodySmall, color = Color(0xFF444444), modifier = Modifier.padding(end = 8.dp))
        }
        if (amount.isNotBlank()) {
            Text(amount, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = Color.Black,
                modifier = Modifier.align(Alignment.Top))
        }
    }
}

@Composable
private fun DrawTotalRow(label: String, value: String, bold: Boolean) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal, color = Color(0xFF333333), modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}
