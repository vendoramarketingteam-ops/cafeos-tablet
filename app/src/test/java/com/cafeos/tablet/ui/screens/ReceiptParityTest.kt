package com.cafeos.tablet.ui.screens

import com.cafeos.tablet.data.BusinessSettings
import com.cafeos.tablet.data.Order
import com.cafeos.tablet.data.OrderItem
import com.cafeos.tablet.data.OrderOption
import com.cafeos.tablet.data.Payment
import com.cafeos.tablet.data.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Parity: the on-screen gamified receipt (ReceiptView ← [buildReceiptModel])
 * must carry the SAME BIR-official content and the SAME totals derivation as
 * ReceiptPrinter.generateReceiptPdf, and must never hide/truncate/reformat
 * receipt data (CaféOS Constitution §III / guardrail #1).
 *
 * `buildReceiptModel` is a pure function (no Android context), so this is a
 * fast JVM unit test. The VAT split formula is asserted to be IDENTICAL to
 * ReceiptPrinter's  vat = totalDue * rate / (100 + rate).
 */
class ReceiptParityTest {

    private fun settings() = BusinessSettings(
        id = 1,
        shopName = "Test Café",
        address = "123 Bean St, Manila",
        tin = "214-890-123-000",
        vatRate = 12.0,
        footerMessage = "Thank you for visiting!"
    )

    private fun order(
        totalAmount: Double = 224.0,
        discountAmount: Double = 32.0,
        discountReason: String? = "STUDENT_PWD",
        customerName: String = "",
        paymentMethod: String = "CASH",
        orderType: String = "DINE_IN",
        tableId: Int? = 3,
        tableLocation: String? = null,
        orderNumber: String = "INV-000100"
    ) = Order(
        orderNumber = orderNumber,
        customerName = customerName,
        status = "COMPLETED",
        totalAmount = totalAmount,
        discountAmount = discountAmount,
        discountRate = 0.125,
        discountReason = discountReason,
        paymentMethod = paymentMethod,
        orderType = orderType,
        tableId = tableId,
        tableLocation = tableLocation,
        createdAt = 1_700_000_000_000L,
        updatedAt = 1_700_000_000_000L
    )

    private fun products() = listOf(
        Product(id = 1, name = "Espresso", price = 120.0, categoryId = 1),
        Product(id = 2, name = "Cookie", price = 16.0, categoryId = 1)
    )

    @Test
    fun receiptModelCarriesAllBirFieldsAndMatchesVmTotals() {
        val settings = settings()
        val ord = order()
        val items = listOf(
            OrderItem(id = 10, orderId = 1, productId = 1, quantity = 2, unitPrice = 120.0, subtotal = 240.0),
            OrderItem(id = 11, orderId = 1, productId = 2, quantity = 1, unitPrice = 16.0, subtotal = 16.0)
        )
        val optionsByItem = mapOf(
            10 to listOf(OrderOption(orderItemId = 10, name = "Milk", value = "Oat", priceDelta = 10.0))
        )
        val payment = Payment(orderId = 1, method = "CASH", amount = 224.0, amountTendered = 300.0, change = 76.0)

        val model = buildReceiptModel(ord, items, products(), settings, optionsByItem, payment)

        // ── BIR header block (never hidden/truncated) ──
        assertEquals("Test Café", model.shopName)
        assertEquals("123 Bean St, Manila", model.address)
        assertEquals("214-890-123-000", model.tin)
        assertEquals("INV-000100", model.orderNumber)
        assertTrue(model.date.isNotBlank())          // Date & Time present
        assertEquals("Dine-in", model.orderType)    // DINE_IN → Dine-in mapping
        assertEquals("Table 3", model.location)      // tableId=3, no location override
        assertEquals("Walk-in Customer", model.customer) // blank customerName → default

        // ── Line items (full detail, not truncated) ──
        assertEquals(3, model.lines.size)            // 2 products + 1 option sub-row
        assertEquals("Espresso", model.lines[0].description)
        assertTrue(model.lines[1].description.contains("Milk: Oat"))   // option sub-row preserved, not truncated
        assertEquals("Cookie", model.lines[2].description)
        assertTrue(model.lines[0].amount.isNotBlank())

        // ── Totals / VAT (single source: order.totalAmount — VM computes) ──
        assertEquals(256.0, model.subtotal, 0.001)              // sum of item.subtotal
        assertEquals(32.0, model.discount, 0.001)                // order.discountAmount
        assertEquals("STUDENT_PWD", model.discountReason)
        assertEquals(224.0, model.totalDue, 0.001)               // == order.totalAmount (no recompute)
        assertEquals(24.0, model.vat, 0.001)                    // 224 * 12 / 112  (= ReceiptPrinter formula)
        assertEquals(200.0, model.vatableSales, 0.001)            // 224 - 24
        // VAT split is internally consistent (vatable + vat == total).
        assertEquals(model.totalDue, model.vatableSales + model.vat, 0.001)

        // ── Amount in words (BIR requirement) ──
        assertTrue(model.amountInWords.contains("Pesos"))
        assertTrue(model.amountInWords.contains("00/100"))

        // ── Payment block ──
        assertEquals("CASH", model.paymentMethod)
        assertEquals(300.0, model.tendered!!, 0.001)            // change > 0.01 → tendered shown
        assertEquals(76.0, model.change!!, 0.001)
        assertEquals("Thank you for visiting!", model.footer)
    }

    @Test
    fun receiptModelOmitsDiscountAndChangeWhenAbsent() {
        val settings = settings()
        val ord = order(
            totalAmount = 100.0,
            discountAmount = 0.0,
            discountReason = null,
            paymentMethod = "GCASH",
            orderType = "TAKEOUT",
            tableId = null,
            customerName = "Juan",
            orderNumber = "INV-000101"
        )
        val items = listOf(
            OrderItem(orderId = 1, productId = 1, quantity = 1, unitPrice = 100.0, subtotal = 100.0)
        )
        val noOpts: Map<Int, List<OrderOption>> = emptyMap()
        val payment = Payment(orderId = 1, method = "GCASH", amountTendered = 100.0, change = 0.0)

        val model = buildReceiptModel(ord, items, products(), settings, noOpts, payment)

        assertEquals(100.0, model.totalDue, 0.001)
        assertEquals(0.0, model.discount, 0.001)                  // no discount line shown
        assertEquals(100.0 * 12.0 / 112.0, model.vat, 0.001)      // VAT formula still parity
        assertEquals(100.0 - model.vat, model.vatableSales, 0.001)
        assertEquals("Takeout", model.orderType)                  // TAKEOUT → Takeout
        assertEquals("Takeaway", model.location)                  // no table/location → Takeaway
        assertEquals("Juan", model.customer)
        assertEquals("GCASH", model.paymentMethod)
        assertNull(model.tendered)                               // change == 0 → not shown
        assertNull(model.change)
    }

    @Test
    fun vatFormulaMatchesReceiptPrinterExactly() {
        // Independent restatement of ReceiptPrinter's derivation:
        //   subtotal = Σ item.subtotal
        //   discount = order.discountAmount.coerceAtLeast(0)
        //   totalDue = order.totalAmount.coerceAtLeast(0)
        //   rate = settings.vatRate.coerceIn(0,100)
        //   vat = totalDue * rate / (100 + rate)
        val rate = 12.0
        for (totalDue in listOf(0.0, 50.0, 224.0, 1234.56)) {
            val expectedVat = totalDue * rate / (100.0 + rate)
            val model = buildReceiptModel(
                order(totalAmount = totalDue, discountAmount = 0.0, discountReason = null,
                      orderNumber = "X-$totalDue"),
                listOf(OrderItem(orderId = 1, productId = 1, quantity = 1, unitPrice = totalDue, subtotal = totalDue)),
                products(), settings(), emptyMap(), null
            )
            assertEquals(expectedVat, model.vat, 0.001)
            assertTrue(abs(model.totalDue - model.vat - model.vatableSales) < 0.001)
            // Order total is the single source of truth — never recomputed from items here.
            assertEquals(totalDue, model.totalDue, 0.001)
            assertEquals(totalDue, model.totalDue, 0.001)
        }
    }
}
