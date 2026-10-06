package com.pitlanehq.android.model

import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.min

/** Two lap traces on the same distance grid (every [step] metres), ready to draw and compare. */
class Compared(
    val step: Double, val speedA: List<Double>, val speedB: List<Double>?, val thrA: List<Double>, val brkA: List<Double>,
    val delta: List<Double>?, val tA: List<Double>, val tB: List<Double>?,
    val thrB: List<Double>? = null, val brkB: List<Double>? = null,
    val gearA: List<Double>? = null, val gearB: List<Double>? = null
)

/** One place on the lap where time goes, with what is different there. */
data class Loss(val fromM: Int, val lost: Double, val brakeDiffM: Int?, val minA: Double, val minB: Double, val throttleDiffM: Int?)

private fun at(tr: Trace, d: Double, k: Int): Double {
    if (tr.rows.isEmpty() || tr.bin <= 0) return 0.0
    val x = d / tr.bin
    val i = x.toInt().coerceIn(0, tr.rows.size - 1)
    val j = min(i + 1, tr.rows.size - 1)
    val f = (x - i).coerceIn(0.0, 1.0)
    val v = tr.rows[i].getOrElse(k) { 0.0 } * (1 - f) + tr.rows[j].getOrElse(k) { 0.0 } * f
    return if (v.isFinite()) v else 0.0
}

fun compare(a: Trace, b: Trace?, step0: Double = 10.0): Compared {
    val len = (a.rows.size - 1) * a.bin
    val lenB = b?.let { (it.rows.size - 1) * it.bin } ?: len
    // at most 400 points per line: enough for a phone screen, light to draw and to touch
    val step = max(step0, min(len, lenB) / 400.0)
    val n = max(2, (min(len, lenB) / step).toInt())
    val d = (0 until n).map { it * step }
    val tA = d.map { at(a, it, 5) }
    val tB = b?.let { tr -> d.map { at(tr, it, 5) } }
    return Compared(
        step,
        d.map { at(a, it, 0) * 3.6 },
        b?.let { tr -> d.map { at(tr, it, 0) * 3.6 } },
        d.map { at(a, it, 1) },
        d.map { at(a, it, 2) },
        tB?.let { tb -> tA.indices.map { tA[it] - tb[it] } },
        tA, tB,
        b?.let { tr -> d.map { at(tr, it, 1) } },
        b?.let { tr -> d.map { at(tr, it, 2) } },
        d.map { a.rows[(it / a.bin).roundToInt().coerceIn(0, a.rows.size - 1)].getOrElse(3) { 0.0 } },
        b?.let { tr -> d.map { tr.rows[(it / tr.bin).roundToInt().coerceIn(0, tr.rows.size - 1)].getOrElse(3) { 0.0 } } }
    )
}

/**
 * One corner in four phases, as driver coaches read data: braking (brake point, how hard), entry
 * (releasing the brake into the turn, trail braking, coasting), apex (minimum speed) and exit (when
 * the throttle comes back). [tip] is an I18n key with [args], about the phase that loses the most.
 * The same analysis as the web's braking coach.
 */
data class Corner(
    val n: Int, val atM: Int, val lost: Double, val phases: Map<String, Double>, val phase: String?,
    val coastA: Double, val coastB: Double, val tip: String?, val args: List<Any>
)

val PHASE_KEYS = listOf("brake", "entry", "apex", "exit")

fun corners(c: Compared): List<Corner> {
    val tB = c.tB ?: return emptyList()
    val sB = c.speedB ?: return emptyList()
    val bB = c.brkB ?: return emptyList()
    val hB = c.thrB ?: return emptyList()
    val n = minOf(c.tA.size, tB.size, sB.size, bB.size, hB.size)
    if (n < 10) return emptyList()
    fun bins(m: Double) = max(1, (m / c.step).toInt())
    fun seg(x: Int, y: Int) = if (x < y) (c.tA[y] - c.tA[x]) - (tB[y] - tB[x]) else 0.0
    // braking zones on the reference: from the brake to the slowest point before the throttle is back
    val zones = ArrayList<Pair<Int, Int>>()
    var i = 1
    while (i < n) {
        if (bB[i] > .12 && bB[i - 1] <= .12) {
            var k = i
            var imin = i
            while (k < n && k < i + bins(600.0) && !(hB[k] > .6 && bB[k] < .05)) {
                if (sB[k] < sB[imin]) imin = k
                k++
            }
            if (sB[i] - sB[imin] > 15) zones.add(i to imin)
            i = max(k, i + 1)
        } else i++
    }
    return zones.mapIndexed { idx, (zi, zmin) ->
        val i0 = max(0, zi - bins(50.0))
        val i1 = min(n - 1, zmin + bins(150.0))
        val lost = seg(i0, i1)
        // the same corner on this lap: its brake point within 100 m
        val w = bins(100.0)
        val ja = (max(1, zi - w)..min(n - 1, zi + w)).filter { c.brkA[it] > .12 && c.brkA[it - 1] <= .12 }.minByOrNull { kotlin.math.abs(it - zi) }
        val aMin = ja?.let { j -> (j..i1).minByOrNull { c.speedA[it] } }
        var ipb = zi
        for (k in zi..zmin) if (bB[k] > bB[ipb]) ipb = k
        val rel = (ipb..zmin).firstOrNull { bB[it] < .05 } ?: zmin
        val a0 = max(rel, zmin - bins(20.0))
        val a1 = min(i1, zmin + bins(20.0))
        val ph = mapOf("brake" to seg(i0, rel), "entry" to seg(rel, a0), "apex" to seg(a0, a1), "exit" to seg(a1, i1))
        fun coast(sp: List<Double>, br: List<Double>) = (i0..i1).count { sp[it] < .05 && br[it] < .05 } * c.step
        val coastA = coast(c.thrA, c.brkA)
        val coastB = coast(hB, bB)
        var tip: String? = null
        var args: List<Any> = emptyList()
        var phase: String? = null
        if (lost > .03) {
            phase = ph.maxBy { it.value }.key
            val dd = ja?.let { ((it - zi) * c.step).toInt() }
            var ipa = ja ?: 0
            if (ja != null && aMin != null) for (k in ja..aMin) if (c.brkA[k] > c.brkA[ipa]) ipa = k
            val pa = if (ja != null) c.brkA[ipa] else 0.0
            val relA = if (ja != null && aMin != null) (ipa..aMin).firstOrNull { c.brkA[it] < .05 } ?: aMin else null
            val trailA = relA?.let { ((it - ipa) * c.step).toInt() }
            val trailB = ((rel - ipb) * c.step).toInt()
            val dmin = aMin?.let { c.speedA[it] - sB[zmin] }
            val puA = aMin?.let { m -> (m..i1).firstOrNull { c.thrA[it] > .5 } }
            val puB = (zmin..i1).firstOrNull { hB[it] > .5 }
            val late = if (puA != null && puB != null) ((puA - puB) * c.step).toInt() else null
            when (phase) {
                "brake" -> when {
                    dd != null && dd < -6 -> { tip = "tip_brake_later"; args = listOf(-dd) }
                    ja != null && pa < bB[ipb] - .1 -> { tip = "tip_brake_harder"; args = listOf((pa * 100).toInt(), (bB[ipb] * 100).toInt()) }
                    dd != null && dd > 6 -> { tip = "tip_brake_earlier"; args = listOf(dd) }
                    else -> tip = "tip_brake_generic"
                }
                "entry" -> when {
                    trailA != null && trailB - trailA >= 15 -> { tip = "tip_trail"; args = listOf(trailA, trailB) }
                    coastA - coastB >= 10 -> { tip = "tip_coast"; args = listOf(coastA.toInt(), coastB.toInt()) }
                    else -> tip = "tip_entry_speed"
                }
                "apex" -> if (dmin != null && dmin < -2) { tip = "tip_apex_speed"; args = listOf((-dmin).toInt()) } else tip = "tip_apex_line"
                else -> when {
                    late != null && late >= 8 -> { tip = "tip_throttle"; args = listOf(late) }
                    coastA - coastB >= 10 -> tip = "tip_no_wait"
                    else -> tip = "tip_full_throttle"
                }
            }
        }
        Corner(idx + 1, (zi * c.step).toInt(), lost, ph, phase, coastA, coastB, tip, args)
    }
}

/** The three stretches of about 250 m where the lap loses the most time against the reference. */
fun losses(c: Compared, segM: Double = 250.0): List<Loss> {
    val tB = c.tB ?: return emptyList()
    val per = max(1, (segM / c.step).toInt())
    val out = ArrayList<Loss>()
    var s = 0
    while (s < c.tA.size - 1) {
        val e = min(c.tA.size - 1, s + per)
        val lost = (c.tA[e] - c.tA[s]) - (tB[e] - tB[s])
        val speedB = c.speedB!!
        fun firstBrake(brk: List<Double>?): Int? = brk?.let { (s..e).firstOrNull { i -> it[i] > 0.3 } }
        val minA = (s..e).minOf { c.speedA[it] }
        val minB = (s..e).minOf { speedB[it] }
        // braking: the reference has no brake channel here, so use where its speed starts to drop
        val bA = firstBrake(c.brkA)
        val bB = (s until e).firstOrNull { i -> i + 2 <= e && speedB[i + 2] < speedB[i] - 3 }
        val brakeDiff = if (bA != null && bB != null) ((bA - bB) * c.step).toInt() else null
        val fullA = (s..e).firstOrNull { c.thrA[it] > 0.95 && c.speedA[it] > minA + 2 }
        val fullB = (s..e).firstOrNull { speedB[it] > minB + 2 && it > (s..e).minBy { k -> speedB[k] } }
        val thrDiff = if (fullA != null && fullB != null) ((fullA - fullB) * c.step).toInt() else null
        out.add(Loss((s * c.step).toInt(), lost, brakeDiff, minA, minB, thrDiff))
        s = e
    }
    return out.filter { it.lost > 0.02 }.sortedByDescending { it.lost }.take(3)
}
