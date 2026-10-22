package com.example.healthapp.nutrition

/** 体重の記録がない日に使う体重（kg）。 */
const val DEFAULT_WEIGHT_KG = 60.0

/**
 * 消費カロリー（kcal）= MET × 体重(kg) × 時間(h)。
 * 運動の強度（MET）は「身体活動のメッツ表」の目安値を使う。
 */
fun exerciseKcal(met: Double, weightKg: Double, minutes: Double): Double {
    require(met > 0 && weightKg > 0 && minutes > 0) { "MET・体重・時間は 0 より大きい値にしてください" }
    return (met * weightKg * minutes / 60).round1()
}
