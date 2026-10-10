package com.sejin.diveroidrevive

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/** 몰입 모드 + 화면 항상 켜짐. 케이스 안에서는 잠금 해제가 불가능하므로 화면이 꺼지면 안 된다. */
fun Activity.enterFullscreen(root: View) {
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowInsetsControllerCompat(window, root).apply {
        hide(WindowInsetsCompat.Type.systemBars())
        systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
}

/**
 * sensorLandscape 로 시작해 현재 잡힌 가로 방향에 고정.
 * 다이빙 중 카메라를 위/아래로 향할 때 화면이 뒤집혀 버튼 좌표가 틀어지는 것을 막는다.
 */
fun Activity.lockOrientation(delayMs: Long = 1500) {
    window.decorView.postDelayed({
        if (!isFinishing) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
    }, delayMs)
}
