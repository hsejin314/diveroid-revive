package com.sejin.diveroidrevive

import android.graphics.PointF
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent

/**
 * 케이스 플런저가 만드는 화면 터치를 버튼 이벤트로 변환. 캘리브레이션 없이 고정 구역 사용.
 *
 * 구역(가로 화면 기준): 오른쪽 28% 세로 띠를 위/중간/아래 세 밴드로 나눔.
 * 플런저는 위·아래가 같은 열, 중간만 조금 더 오른쪽이지만 밴드는 세로 위치만 보므로 무관.
 * 폰 크기가 달라도 케이스 안에서 화면이 차지하는 비율은 비슷하므로 비율 기반이면 대체로 맞는다.
 *
 * 롱프레스는 임계 도달 시 1회만 발화. 수압으로 버튼이 눌린 채 고착돼도 반복 발화 없음.
 */
class ButtonDetector(
    private val w: Int,
    private val h: Int,
    private val longPressMs: Long = 700,
    private val onShort: (Btn) -> Unit,
    private val onLong: (Btn) -> Unit,
    /** 눌림 시작 시 버튼, 뗄 때 null — 시각/촉각 피드백용 */
    private val onPress: (Btn?) -> Unit,
) {
    companion object {
        const val STRIP_X = 0.72f          // 이 비율보다 오른쪽만 버튼 구역
        private const val TOP_MAX = 0.38f
        private const val MID_MAX = 0.62f

        fun zone(x: Float, y: Float, w: Int, h: Int): Btn? {
            if (x < w * STRIP_X) return null
            val fy = y / h
            return when {
                fy < TOP_MAX -> Btn.TOP
                fy < MID_MAX -> Btn.MID
                else -> Btn.BOTTOM
            }
        }

        /** 구역 표시 원의 중심 (중간 버튼은 실제 플런저처럼 살짝 더 오른쪽) */
        fun center(btn: Btn, w: Int, h: Int) = PointF(
            w * if (btn == Btn.MID) 0.94f else 0.90f,
            h * when (btn) { Btn.TOP -> 0.27f; Btn.MID -> 0.50f; Btn.BOTTOM -> 0.73f },
        )
    }

    private val handler = Handler(Looper.getMainLooper())
    private var current: Btn? = null
    private var longFired = false
    private val fireLong = Runnable {
        current?.let { longFired = true; onLong(it) }
    }

    fun onTouch(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                current = zone(e.x, e.y, w, h) ?: return false
                longFired = false
                onPress(current)
                handler.postDelayed(fireLong, longPressMs)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                handler.removeCallbacks(fireLong)
                val b = current ?: return false
                current = null
                onPress(null)
                if (!longFired && e.actionMasked == MotionEvent.ACTION_UP) onShort(b)
            }
        }
        return true
    }
}
