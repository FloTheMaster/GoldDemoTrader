package com.golddemotrader.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val bg = Color(0xFF101827)
private val panel = Color(0xFF1A2638)
private val green = Color(0xFF28C894)
private val red = Color(0xFFFF667A)

data class Trade(val id: Int, val side: String, val entry: Double, val lots: Double, val sl: Double?, val tp: Double?)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = darkColorScheme(background = bg, surface = panel, primary = green)) { TradingScreen() } }
    }
}

@Composable
private fun TradingScreen() {
    var price by remember { mutableDoubleStateOf(4000.0) }
    var history by remember { mutableStateOf(List(30) { 3990.0 + it * 0.3 }) }
    var balance by remember { mutableDoubleStateOf(10000.0) }
    var lotsText by remember { mutableStateOf("0.10") }
    var slText by remember { mutableStateOf("") }
    var tpText by remember { mutableStateOf("") }
    var trades by remember { mutableStateOf(listOf<Trade>()) }
    var closed by remember { mutableStateOf(listOf<String>()) }
    var tab by remember { mutableStateOf("Trade") }
    var message by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1800)
            price = (price + Random.nextDouble(-2.0, 2.0)).coerceAtLeast(100.0)
            history = (history + price).takeLast(30)
            val hit = trades.filter { t ->
                (t.sl != null && if (t.side == "BUY") price <= t.sl else price >= t.sl) ||
                (t.tp != null && if (t.side == "BUY") price >= t.tp else price <= t.tp)
            }
            hit.forEach { t ->
                val result = pnl(t, price)
                balance += result
                closed = listOf("${t.side} ${t.lots} lot · ${money(result)} USD · SL/TP") + closed
            }
            if (hit.isNotEmpty()) trades = trades.filterNot { it in hit }
        }
    }
    val floating = trades.sumOf { pnl(it, price) }

    Column(Modifier.fillMaxSize().background(bg).padding(16.dp)) {
        Text("GOLD DEMO TRADER", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("XAU/USD · PREZZI SIMULATI · SOLO DEMO", color = Color.LightGray, fontSize = 11.sp)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = panel)) {
                Column(Modifier.padding(12.dp)) { Text("SALDO VIRTUALE", color = Color.LightGray, fontSize = 11.sp); Text("${money(balance)} $", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            }
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = panel)) {
                Column(Modifier.padding(12.dp)) { Text("EQUITY", color = Color.LightGray, fontSize = 11.sp); Text("${money(balance + floating)} $", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp) }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(colors = CardDefaults.cardColors(containerColor = panel)) {
            Column(Modifier.padding(14.dp)) {
                Text("XAU/USD", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(String.format("%.2f", price), color = green, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Canvas(Modifier.fillMaxWidth().height(100.dp)) {
                    val lo = history.minOrNull() ?: 0.0
                    val hi = (history.maxOrNull() ?: 1.0).coerceAtLeast(lo + 0.1)
                    val points = history.mapIndexed { i, v -> Offset(i * size.width / (history.size - 1), size.height - ((v - lo) / (hi - lo)).toFloat() * size.height) }
                    for (i in 0 until points.lastIndex) drawLine(if (points[i + 1].y < points[i].y) green else red, points[i], points[i + 1], 3f)
                }
                Text("Grafico illustrativo, non TradingView", color = Color.LightGray, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(lotsText, { lotsText = it }, label = { Text("Lotti (0.01–100)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(slText, { slText = it }, label = { Text("Stop Loss") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(tpText, { tpText = it }, label = { Text("Take Profit") }, singleLine = true, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { place("BUY", price, lotsText, slText, tpText, trades, { trades = it }, { message = it }) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = green)) { Text("BUY", color = bg, fontWeight = FontWeight.Bold) }
            Button(onClick = { place("SELL", price, lotsText, slText, tpText, trades, { trades = it }, { message = it }) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = red)) { Text("SELL", color = Color.White, fontWeight = FontWeight.Bold) }
        }
        if (message.isNotBlank()) Text(message, color = Color.Yellow, fontSize = 12.sp)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Trade", "Posizioni", "Storico").forEach { label -> FilterChip(selected = tab == label, onClick = { tab = label }, label = { Text(label) }) }
        }
        when (tab) {
            "Posizioni" -> LazyColumn {
                items(trades, key = { it.id }) { t ->
                    Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = panel)) {
                        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("${t.side} · ${t.lots} lot", color = if (t.side == "BUY") green else red, fontWeight = FontWeight.Bold)
                                Text("Entrata ${money(t.entry)} · P/L ${money(pnl(t, price))} $", color = Color.White, fontSize = 12.sp)
                                Text("SL ${t.sl ?: "—"} · TP ${t.tp ?: "—"}", color = Color.LightGray, fontSize = 11.sp)
                            }
                            TextButton(onClick = { val p = pnl(t, price); balance += p; closed = listOf("${t.side} · ${money(p)} USD · chiusa manualmente") + closed; trades = trades.filterNot { it.id == t.id } }) { Text("Chiudi") }
                        }
                    }
                }
            }
            "Storico" -> LazyColumn { items(closed) { Text(it, color = Color.White, modifier = Modifier.padding(6.dp)) } }
            else -> Column { Text("Posizioni aperte: ${trades.size}", color = Color.White); Text("P/L flottante: ${money(floating)} USD", color = if (floating >= 0) green else red) }
        }
        Spacer(Modifier.height(8.dp))
        Text("Attenzione: demo locale, nessun prezzo reale. Le operazioni non sono salvate online e possono sparire chiudendo l'app.", color = Color.LightGray, fontSize = 10.sp)
    }
}

private fun place(side: String, price: Double, lotsText: String, slText: String, tpText: String, trades: List<Trade>, update: (List<Trade>) -> Unit, report: (String) -> Unit) {
    val lots = lotsText.toDoubleOrNull()
    if (lots == null || lots <= 0.0 || lots > 100.0) { report("Inserisci un volume valido tra 0.01 e 100 lotti."); return }
    val sl = slText.toDoubleOrNull()
    val tp = tpText.toDoubleOrNull()
    if (side == "BUY" && ((sl != null && sl >= price) || (tp != null && tp <= price))) { report("Per BUY: SL sotto il prezzo e TP sopra."); return }
    if (side == "SELL" && ((sl != null && sl <= price) || (tp != null && tp >= price))) { report("Per SELL: SL sopra il prezzo e TP sotto."); return }
    update(trades + Trade((trades.maxOfOrNull { it.id } ?: 0) + 1, side, price, lots, sl, tp))
    report("Ordine demo $side aperto a ${money(price)}.")
}
private fun pnl(t: Trade, current: Double): Double = (if (t.side == "BUY") current - t.entry else t.entry - current) * t.lots * 100.0
private fun money(v: Double): String = String.format("%,.2f", v)
