package com.sejin.diveroidrevive

import android.content.Context
import android.content.pm.ActivityInfo
import android.util.Rational

enum class Btn { TOP, MID, BOTTOM }

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("diveroidrevive", Context.MODE_PRIVATE)

    var video4k: Boolean
        get() = sp.getBoolean("video4k", false)
        set(v) = sp.edit().putBoolean("video4k", v).apply()

    /** "16:9" | "16:10" | "4:3" */
    var aspect: String
        get() = sp.getString("aspect", "16:9")!!
        set(v) = sp.edit().putString("aspect", v).apply()

    fun aspectRational(): Rational = when (aspect) {
        "16:10" -> Rational(16, 10)
        "4:3" -> Rational(4, 3)
        else -> Rational(16, 9)
    }

    /** "auto" = 센서로 잡은 가로 방향에 고정, "left" = 폰 상단이 왼쪽, "right" = 폰 상단이 오른쪽 */
    var orientation: String
        get() = sp.getString("orientation", "auto")!!
        set(v) = sp.edit().putString("orientation", v).apply()

    fun orientationFlag(): Int = when (orientation) {
        "left" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        "right" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
        else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
    }

    /** 자동 절전까지 대기(분). 0 = 끔 */
    var autoSleepMin: Int
        get() = sp.getInt("autoSleepMin", 2)
        set(v) = sp.edit().putInt("autoSleepMin", v).apply()

    /** 수중 보정 강도 1(약) 2(중) 3(강) */
    var uwStrength: Int
        get() = sp.getInt("uwStrength", 2)
        set(v) = sp.edit().putInt("uwStrength", v).apply()

    /** 진행 중 다이빙 시작 시각(epoch ms). 0 = 없음. 앱이 죽어도 이어지도록 저장 */
    var diveStart: Long
        get() = sp.getLong("diveStart", 0L)
        set(v) = sp.edit().putLong("diveStart", v).apply()

    /** 진행 중 다이빙 시작 위치 "lat,lon" */
    var diveStartLoc: String?
        get() = sp.getString("diveStartLoc", null)
        set(v) = sp.edit().putString("diveStartLoc", v).apply()

    /** 시작 위치의 지명(영어). 기록 제목 기본값 */
    var diveStartPlace: String?
        get() = sp.getString("diveStartPlace", null)
        set(v) = sp.edit().putString("diveStartPlace", v).apply()
}
