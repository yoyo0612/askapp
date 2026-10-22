package com.example.healthapp.data

/** 運動の種類と強度（MET）。値は身体活動のメッツ表（2011年版）を参考にした目安。 */
data class ExerciseType(val name: String, val met: Double)

val EXERCISE_TYPES: List<ExerciseType> = listOf(
    ExerciseType("ウォーキング（普通）", 3.5),
    ExerciseType("ジョギング", 7.0),
    ExerciseType("サイクリング（普通）", 6.8),
    ExerciseType("水泳（ゆっくり）", 5.8),
    ExerciseType("筋力トレーニング", 3.5),
    ExerciseType("ヨガ", 2.5),
    ExerciseType("ストレッチ", 2.3),
)
