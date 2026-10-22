package com.example.healthapp

import android.app.Application
import com.example.healthapp.data.HealthDatabase

class HealthApp : Application() {
    /** 画面をまたいで1つの DB 接続を共有する。 */
    val database: HealthDatabase by lazy { HealthDatabase(this) }
}
