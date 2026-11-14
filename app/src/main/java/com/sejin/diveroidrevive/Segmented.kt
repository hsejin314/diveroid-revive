package com.sejin.diveroidrevive

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat

/** 시작 화면의 세그먼트 컨트롤. 테두리는 컨테이너(@style/Segment)가, 칸은 여기서 만든다. */
class Segmented(private val container: LinearLayout) {
    private var selected = 0

    fun set(options: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit) {
        val ctx = container.context
        val font = ResourcesCompat.getFont(ctx, R.font.rajdhani_semibold)
        val d = ctx.resources.displayMetrics.density
        selected = selectedIndex
        container.removeAllViews()
        options.forEachIndexed { i, label ->
            if (i > 0) container.addView(View(ctx).apply {
                setBackgroundColor(ctx.getColor(R.color.border))
                layoutParams = LinearLayout.LayoutParams((1 * d).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
            })
            container.addView(TextView(ctx).apply {
                text = label
                typeface = font
                textSize = 15f
                letterSpacing = 0.08f
                isAllCaps = true
                gravity = Gravity.CENTER
                setPadding(0, (11 * d).toInt(), 0, (11 * d).toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setOnClickListener { selected = i; render(); onSelect(i) }
            })
        }
        render()
    }

    private fun render() {
        val ctx = container.context
        var idx = 0
        for (k in 0 until container.childCount) {
            val v = container.getChildAt(k) as? TextView ?: continue
            val sel = idx++ == selected
            v.setBackgroundColor(if (sel) ctx.getColor(R.color.accent) else Color.TRANSPARENT)
            v.setTextColor(ctx.getColor(if (sel) R.color.bg else R.color.text))
        }
    }
}
