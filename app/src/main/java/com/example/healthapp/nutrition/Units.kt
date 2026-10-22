package com.example.healthapp.nutrition

/** キロジュール（kJ）を kcal に換算する。1 kcal = 4.184 kJ。 */
fun kilojouleToKcal(kj: Double): Double = (kj / 4.184).round1()
