package com.example.healthapp.nutrition

import kotlin.math.roundToInt

/** 目標に対する達成率（%）。目標が0以下のときは0。 */
fun achievementPercent(actual: Double, goal: Double): Int =
    if (goal <= 0) 0 else (actual / goal * 100).roundToInt()

/** 体重の増減（最後 − 最初）。2件未満のときは null。 */
fun weightChange(kgInDateOrder: List<Double>): Double? =
    if (kgInDateOrder.size < 2) null else (kgInDateOrder.last() - kgInDateOrder.first()).round1()
