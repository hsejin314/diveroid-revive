package com.sejin.diveroidrevive

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.sejin.diveroidrevive.databinding.ActivityMainBinding
import java.util.Locale

/** 시작 화면 = 설정 + 진행 중 다이빙 + 최근 기록. */
class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private lateinit var prefs: Prefs

    // 위치 권한은 여기서 미리 받아 카메라 화면을 방해하지 않게. 거부해도 카메라는 시작(GPS 만 생략)
    private val askLocation = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { openCamera() }

    private val orientKeys = listOf("auto", "left", "right")
    private val orientNames get() = listOf(R.string.orient_auto, R.string.orient_left, R.string.orient_right).map { getString(it) }
    private val orientShort get() = listOf(R.string.orient_auto_short, R.string.orient_left_short, R.string.orient_right_short).map { getString(it) }
    private val langTags = listOf("en", "ko", "ja", "zh")
    private val langNames = listOf("English", "한국어", "日本語", "中文")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        prefs = Prefs(this)
        b.device.text = Build.MODEL

        Segmented(b.segAspect).set(listOf("16:9", "16:10", "4:3"), listOf("16:9", "16:10", "4:3").indexOf(prefs.aspect).coerceAtLeast(0)) {
            prefs.aspect = listOf("16:9", "16:10", "4:3")[it]; refresh()
        }
        Segmented(b.segVideo).set(listOf("FHD", "4K"), if (prefs.video4k) 1 else 0) { prefs.video4k = it == 1; refresh() }
        Segmented(b.segUw).set(listOf(R.string.uw_weak, R.string.uw_mid, R.string.uw_strong).map { getString(it) }, prefs.uwStrength - 1) {
            prefs.uwStrength = it + 1; refresh()
        }
        Segmented(b.segSleep).set(
            listOf(R.string.sleep_1, R.string.sleep_2, R.string.sleep_off).map { getString(it) },
            when (prefs.autoSleepMin) { 1 -> 0; 0 -> 2; else -> 1 },
        ) { prefs.autoSleepMin = listOf(1, 2, 0)[it] }

        b.rowOrient.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.section_orientation)
                .setSingleChoiceItems(orientNames.toTypedArray(), orientKeys.indexOf(prefs.orientation)) { d, i ->
                    prefs.orientation = orientKeys[i]; refresh(); d.dismiss()
                }.show()
        }

        // 언어: AppCompat 의 앱별 로케일. 바꾸면 액티비티가 자동 재생성됨
        b.rowLang.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.section_language)
                .setSingleChoiceItems(langNames.toTypedArray(), langTags.indexOf(currentLang())) { d, i ->
                    d.dismiss()
                    if (langTags[i] != currentLang()) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(langTags[i]))
                }.show()
        }

        b.startBlock.setOnClickListener {
            if (Gps.granted(this)) openCamera()
            else askLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        b.btnLogs.setOnClickListener { startActivity(Intent(this, LogListActivity::class.java)) }
        b.lastDive.setOnClickListener { startActivity(Intent(this, LogListActivity::class.java)) }
        b.btnEndDive.setOnClickListener {
            DiveLog.finishDive(this, prefs)
            refresh()
        }
    }

    private fun currentLang() = AppCompatDelegate.getApplicationLocales()[0]?.language ?: Locale.getDefault().language
    private fun openCamera() = startActivity(Intent(this, CameraActivity::class.java))

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val active = prefs.diveStart != 0L
        b.btnEndDive.visibility = if (active) View.VISIBLE else View.GONE
        val summary = "${getString(R.string.lens_wide)} · ${prefs.aspect} · ${if (prefs.video4k) "4K" else "FHD"} · UW ${
            listOf(R.string.uw_level_weak, R.string.uw_level_mid, R.string.uw_level_strong).map { getString(it) }[(prefs.uwStrength - 1).coerceIn(0, 2)]
        }"
        if (active) {
            b.startTitle.text = getString(R.string.continue_dive)
            b.startSub.text = getString(
                R.string.active_dive_fmt, DiveLog.time(prefs.diveStart),
                DiveLog.durationStr(this, System.currentTimeMillis() - prefs.diveStart),
            )
        } else {
            b.startTitle.text = getString(R.string.start_dive)
            b.startSub.text = summary
        }

        val last = DiveLog.all(this).firstOrNull()
        b.lastDive.visibility = if (last == null) View.GONE else View.VISIBLE
        last?.let {
            b.lastLabel.text = "${getString(R.string.last_dive)} · ${DiveLog.dateTime(it.start).substring(5, 10)}"
            b.lastTitle.text = it.title.ifEmpty { getString(R.string.untitled) }
            b.lastDuration.text = DiveLog.durationStr(this, it.end - it.start)
        }

        b.orientValue.text = orientShort[orientKeys.indexOf(prefs.orientation).coerceAtLeast(0)] + " ›"
        b.langValue.text = langNames[langTags.indexOf(currentLang()).coerceAtLeast(0)] + " ›"
    }
}
