package com.pitlanehq.android.model

import kotlin.math.max
import kotlin.math.min

/** Two lap traces on the same distance grid (every [step] metres), ready to draw and compare. */
class Compared(val step: Double, val speedA: List<Double>, val speedB: List<Double>?, val thrA: List<Double>, val brkA: List<Double>, val delta: List<Double>?, val tA: List<Double>, val tB: List<Double>?)

/** One place on the lap where time goes, with what is different there. */
data class Loss(val fromM: Int, val lost: Double, val brakeDiffM: Int?, val minA: Double, val minB: Double, val throttleDiffM: Int?)

private fun at(tr: Trace, d: Double, k: Int): Double {
    val x = d / tr.bin
    val i = x.toInt().coerceIn(0, tr.rows.size - 1)
    val j = min(i + 1, tr.rows.size - 1)
    val f = (x - i).coerceIn(0.0, 1.0)
    return tr.rows[i][k] * (1 - f) + tr.rows[j][k] * f
}

fun compare(a: Trace, b: Trace?, step: Double = 10.0): Compared {
    val len = (a.rows.size - 1) * a.bin
    val lenB = b?.let { (it.rows.size - 1) * it.bin } ?: len
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
        tA, tB
    )
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
