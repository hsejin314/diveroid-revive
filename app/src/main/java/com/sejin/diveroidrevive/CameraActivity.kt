package com.sejin.diveroidrevive

import android.Manifest
import android.animation.ValueAnimator
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.BatteryManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.MediaStore
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.util.Rational
import android.view.Gravity
import android.view.MotionEvent
import android.view.Surface
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
import com.sejin.diveroidrevive.databinding.ActivityCameraBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.min

private const val TAG = "CameraActivity"

enum class Lens(val label: Int) {
    ULTRA(R.string.lens_ultra), WIDE(R.string.lens_wide), TELE(R.string.lens_tele), SELFIE(R.string.lens_selfie)
}

class CameraActivity : AppCompatActivity() {
    private lateinit var b: ActivityCameraBinding
    private lateinit var prefs: Prefs
    private val main = Handler(Looper.getMainLooper())

    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private val wb = UnderwaterWb()

    private var lens = Lens.WIDE
    private var selectorIndex = -1          // -1 = 셀렉터 닫힘
    private var powerSave = false
    private var underwater = false
    private var detector: ButtonDetector? = null
    private lateinit var zones: Map<Btn, View>
    private lateinit var zoneLabels: Map<Btn, View>
    private var lastInput = SystemClock.elapsedRealtime()
    private var ticks = 0

    private val battery by lazy { getSystemService(BATTERY_SERVICE) as BatteryManager }
    private val clock = SimpleDateFormat("HH:mm", Locale.getDefault())

    private val closeSelector = Runnable { selectorIndex = -1; renderSelector() }
    private val hideStatus = Runnable { b.status.visibility = View.GONE }
    private val hideFocus = Runnable { b.focusBox.setBackgroundResource(R.drawable.focus_box) }
    private val tick = object : Runnable {
        override fun run() { onTick(); main.postDelayed(this, 1000) }
    }

    private val askCamera = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) startCamera() else { status(getString(R.string.cam_perm_needed)); main.postDelayed({ finish() }, 1500) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = Prefs(this)
        requestedOrientation = prefs.orientationFlag()
        super.onCreate(savedInstanceState)
        b = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(b.root)
        enterFullscreen(b.root)
        // 어떤 이유로 잠금이 걸려도 이 화면은 잠금 위에 계속 보이고, 꺼진 화면도 켠다
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        if (prefs.diveStart == 0L) {
            prefs.diveStart = System.currentTimeMillis()
            prefs.diveStartLoc = null
            prefs.diveStartPlace = null
            val app = applicationContext
            Gps.current(app) { loc ->
                loc ?: return@current
                val p = Prefs(app)
                p.diveStartLoc = Gps.fmt(loc)
                Gps.place(app, loc) { name -> p.diveStartPlace = name }   // 기록 제목 기본값(영어 지명)
            }
        }

        zones = mapOf(Btn.TOP to b.zoneTop, Btn.MID to b.zoneMid, Btn.BOTTOM to b.zoneBottom)
        zoneLabels = mapOf(Btn.TOP to b.zoneTopLabel, Btn.MID to b.zoneMidLabel, Btn.BOTTOM to b.zoneBottomLabel)
        b.root.post { layoutViews() }
        b.root.setOnTouchListener { v, e ->
            if (e.actionMasked == MotionEvent.ACTION_UP) v.performClick()
            if (powerSave) {
                if (e.actionMasked == MotionEvent.ACTION_DOWN) setPowerSave(false)
                true
            } else detector?.onTouch(e) ?: false
        }
        b.btnExit.setOnLongClickListener { endDive(); true }
        onBackPressedDispatcher.addCallback(this) { status(getString(R.string.exit_hint)) }

        renderMode()
        b.aspectLabel.text = prefs.aspect
        b.hudBattery.text = batteryStr()
        // 이름표: 첫 줄 기능, 둘째 줄(길게) 은 흐리게
        listOf(
            Triple(b.zoneTopLabel, R.string.zone_top, R.string.zone_top_hold),
            Triple(b.zoneMidLabel, R.string.zone_mid, R.string.zone_mid_hold),
            Triple(b.zoneBottomLabel, R.string.zone_bottom, R.string.zone_bottom_hold),
        ).forEach { (v, main, hold) ->
            val s = SpannableString("${getString(main)}\n${getString(hold)}")
            s.setSpan(ForegroundColorSpan(getColor(R.color.muted)), s.indexOf('\n'), s.length, 0)
            v.text = s
        }
        b.zoneMidLabel.setTextColor(getColor(android.R.color.white))

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        else askCamera.launch(Manifest.permission.CAMERA)
    }

    override fun onResume() {
        super.onResume()
        if (prefs.orientation == "auto") lockOrientation()
        main.post(tick)
    }

    override fun onPause() {
        super.onPause()
        main.removeCallbacks(tick)
    }

    // ── 레이아웃: 프리뷰 화면비 + 버튼 구역 ───────────────────────────────

    private fun layoutViews() {
        val w = b.root.width
        val h = b.root.height

        // 프리뷰(와 와이프 오버레이)를 설정 화면비로 레터박스. 보이는 영역 = 저장되는 영역
        val ar = prefs.aspectRational().toFloat()
        val (pw, ph) = if (w / h.toFloat() > ar) (h * ar).toInt() to h else w to (w / ar).toInt()
        for (v in listOf(b.preview, b.wipe)) v.layoutParams = FrameLayout.LayoutParams(pw, ph, Gravity.CENTER)

        detector = ButtonDetector(w, h, onShort = ::onShort, onLong = ::onLong, onPress = ::showPress)
        val pad = (30 * resources.displayMetrics.density).toInt()
        val gap = 8 * resources.displayMetrics.density
        val rects = mutableListOf<Rect>()
        zones.forEach { (btn, v) ->
            val c = ButtonDetector.center(btn, w, h)
            v.x = c.x - v.width / 2f
            v.y = c.y - v.height / 2f
            zoneLabels[btn]!!.let { l -> l.x = v.x - l.width - gap; l.y = c.y - l.height / 2f }
            // 우측 가장자리 뒤로가기 제스처가 버튼을 가로채지 않도록 (시스템 상한 200dp → 버튼당 ±30dp)
            rects += Rect((w * ButtonDetector.STRIP_X).toInt(), (c.y - pad).toInt(), w, (c.y + pad).toInt())
        }
        b.root.systemGestureExclusionRects = rects
    }

    // ── 버튼 ──────────────────────────────────────────────────────────────

    /** 눌림 피드백: 구역 원 강조 (진동 없음 — 촬영 흔들림) */
    private fun showPress(btn: Btn?) {
        zones.forEach { (k, v) -> v.setBackgroundResource(if (k == btn) R.drawable.zone_active else R.drawable.zone_idle) }
        if (btn != null) lastInput = SystemClock.elapsedRealtime()
    }

    private fun onShort(btn: Btn) = when (btn) {
        Btn.TOP -> cycleSelector()
        Btn.MID -> if (selectorIndex >= 0) confirmSelector() else takePhoto()
        Btn.BOTTOM -> focusCenter()
    }

    private fun onLong(btn: Btn) = when (btn) {
        Btn.TOP -> setPowerSave(true)
        Btn.MID -> toggleVideo()
        Btn.BOTTOM -> toggleUnderwater()
    }

    // ── 셀렉터 ────────────────────────────────────────────────────────────

    private fun cycleSelector() {
        selectorIndex = if (selectorIndex < 0) lens.ordinal else (selectorIndex + 1) % Lens.values().size
        renderSelector()
        main.removeCallbacks(closeSelector)
        main.postDelayed(closeSelector, 4000)
    }

    private fun confirmSelector() {
        main.removeCallbacks(closeSelector)
        val chosen = Lens.values()[selectorIndex]
        selectorIndex = -1
        renderSelector()
        if (chosen == lens) return
        val needRebind = (chosen == Lens.SELFIE) != (lens == Lens.SELFIE)
        if (needRebind && recording != null) { status(getString(R.string.no_switch_recording)); return }
        lens = chosen
        renderMode()
        status(getString(lens.label))
        if (needRebind) bind() else applyZoom()
    }

    private fun renderSelector() {
        if (selectorIndex < 0) { b.selector.visibility = View.GONE; return }
        b.selector.visibility = View.VISIBLE
        if (b.selector.childCount == 0) {
            val d = resources.displayMetrics.density
            Lens.values().forEach { l ->
                b.selector.addView(TextView(this).apply {
                    setText(l.label)
                    textSize = 20f
                    typeface = ResourcesCompat.getFont(context, R.font.rajdhani_semibold)
                    letterSpacing = 0.15f
                    isAllCaps = true
                    gravity = Gravity.CENTER
                    setPadding((28 * d).toInt(), (10 * d).toInt(), (28 * d).toInt(), (10 * d).toInt())
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                        .apply { topMargin = (4 * d).toInt() }
                })
            }
        }
        for (i in 0 until b.selector.childCount) {
            val v = b.selector.getChildAt(i) as TextView
            val sel = i == selectorIndex
            v.setBackgroundResource(if (sel) R.drawable.chip_fill else R.drawable.chip_dim)
            v.setTextColor(getColor(if (sel) R.color.bg else R.color.text))
        }
    }

    private fun renderMode() {
        b.modeLabel.setText(lens.label)
        b.underwaterLabel.text = if (underwater) uwOnText() else getString(R.string.uw_off)
        b.underwaterLabel.setBackgroundResource(if (underwater) R.drawable.chip_fill else R.drawable.chip_dim)
        b.underwaterLabel.setTextColor(getColor(if (underwater) R.color.bg else R.color.accent_dim))
    }

    private fun uwOnText(): String {
        val level = arrayOf(R.string.uw_level_weak, R.string.uw_level_mid, R.string.uw_level_strong)[(prefs.uwStrength - 1).coerceIn(0, 2)]
        return if (wb.manual) getString(R.string.uw_on_fmt, getString(level)) else getString(R.string.uw_on_simple)
    }

    // ── 주기 갱신: 시계·배터리·다이브 시간·자동 절전 ───────────────────────

    private fun batteryStr() = getString(R.string.bat_fmt, battery.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))

    private fun onTick() {
        ticks++
        if (powerSave) return
        b.hudTime.text = clock.format(Date())
        val s = (System.currentTimeMillis() - prefs.diveStart) / 1000
        b.hudDiveMain.text = if (s >= 3600) "%d:%02d".format(s / 3600, s / 60 % 60) else "${s / 60}"
        b.hudDiveSec.text = ":%02d".format(s % 60)
        if (ticks % 30 == 0) b.hudBattery.text = batteryStr()

        val idleMs = prefs.autoSleepMin * 60_000L
        if (idleMs > 0 && recording == null && SystemClock.elapsedRealtime() - lastInput > idleMs) setPowerSave(true)
    }

    // ── 카메라 ────────────────────────────────────────────────────────────

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({ provider = future.get(); bind() }, ContextCompat.getMainExecutor(this))
    }

    private fun bind() {
        val p = provider ?: return
        camera?.cameraInfo?.cameraState?.removeObservers(this)   // 재바인드 시 관찰자 중복 방지
        p.unbindAll()

        // 화면비: 4:3 이면 4:3 스트림, 그 외(16:9, 16:10)는 16:9 스트림. 16:10 은 ViewPort 로 추가 크롭
        val ar = prefs.aspectRational()
        val is43 = ar == Rational(4, 3)
        val rs = ResolutionSelector.Builder()
            .setAspectRatioStrategy(if (is43) AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY else AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
            .build()

        val previewBuilder = Preview.Builder().setResolutionSelector(rs)
        wb.attach(previewBuilder)   // AWB 자동값을 계속 받아두기 위한 캡처 콜백
        val preview = previewBuilder.build().also { it.setSurfaceProvider(b.preview.surfaceProvider) }
        val ic = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(rs)
            .build()
        val quality = if (prefs.video4k) Quality.UHD else Quality.FHD
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(quality, FallbackStrategy.lowerQualityOrHigherThan(Quality.FHD)))
            .setAspectRatio(if (is43) AspectRatio.RATIO_4_3 else AspectRatio.RATIO_16_9)
            .build()
        val vc = VideoCapture.withOutput(recorder)

        val rotation = b.preview.display?.rotation ?: Surface.ROTATION_90
        val group = UseCaseGroup.Builder()
            .addUseCase(preview).addUseCase(ic).addUseCase(vc)
            .setViewPort(ViewPort.Builder(ar, rotation).build())
            .build()
        val selector = if (lens == Lens.SELFIE) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

        try {
            camera = p.bindToLifecycle(this, selector, group)
        } catch (e: Exception) {
            Log.e(TAG, "bind failed", e)
            status(getString(R.string.cam_start_fail_fmt, e.message ?: ""))
            main.postDelayed({ if (!powerSave) bind() }, 2000)
            return
        }
        imageCapture = ic
        videoCapture = vc
        applyZoom()
        // 수중모드 켜진 채 재바인드(셀피 전환·절전 복귀)면 자동값 몇 프레임 받은 뒤 다시 적용
        if (underwater) main.postDelayed({ camera?.let { wb.apply(it, prefs.uwStrength); renderMode() } }, 700)

        // 원 앱의 "에러 나면 못 켬" 대응: 상태 에러 시 자동 재바인드
        camera?.cameraInfo?.cameraState?.observe(this) { st ->
            st.error?.let { err ->
                Log.w(TAG, "camera error ${err.code}")
                status(getString(R.string.cam_error_fmt, err.code))
                main.postDelayed({ if (!powerSave && recording == null) bind() }, 2000)
            }
        }
    }

    private fun applyZoom() {
        val cam = camera ?: return
        val zs = cam.cameraInfo.zoomState.value
        val ratio = when (lens) {
            Lens.ULTRA -> zs?.minZoomRatio ?: 1f
            Lens.TELE -> min(3f, zs?.maxZoomRatio ?: 3f)
            else -> 1f
        }
        cam.cameraControl.setZoomRatio(ratio)
    }

    private fun focusCenter() {
        val cam = camera ?: return
        val pt = b.preview.meteringPointFactory.createPoint(b.preview.width / 2f, b.preview.height / 2f)
        cam.cameraControl.startFocusAndMetering(
            FocusMeteringAction.Builder(pt).setAutoCancelDuration(4, TimeUnit.SECONDS).build()
        )
        b.focusBox.setBackgroundResource(R.drawable.focus_box_active)
        main.removeCallbacks(hideFocus)
        main.postDelayed(hideFocus, 800)
    }

    private fun stamp() = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private fun takePhoto() {
        val ic = imageCapture ?: return
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "DIVE_${stamp()}")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/DiveroidRevive")
        }
        val meta = ImageCapture.Metadata().apply { isReversedHorizontal = lens == Lens.SELFIE }
        val out = ImageCapture.OutputFileOptions
            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
            .setMetadata(meta).build()

        b.flash.alpha = 0.8f
        b.flash.animate().alpha(0f).setDuration(150).start()

        ic.takePicture(out, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(r: ImageCapture.OutputFileResults) = status(getString(R.string.photo_saved))
            override fun onError(e: ImageCaptureException) {
                Log.e(TAG, "photo failed", e)
                status(getString(R.string.photo_fail_fmt, e.message ?: ""))
            }
        })
    }

    private fun toggleVideo() {
        recording?.let { it.stop(); recording = null; return }
        val vc = videoCapture ?: return
        val cv = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "DIVE_${stamp()}")
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/DiveroidRevive")
        }
        val opts = MediaStoreOutputOptions.Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(cv).build()
        // 수중이라 오디오 없음 → RECORD_AUDIO 권한 불필요
        recording = vc.output.prepareRecording(this, opts).start(ContextCompat.getMainExecutor(this)) { ev ->
            when (ev) {
                is VideoRecordEvent.Start -> { b.recLabel.text = "REC"; b.recGroup.visibility = View.VISIBLE }
                is VideoRecordEvent.Status -> {
                    val s = TimeUnit.NANOSECONDS.toSeconds(ev.recordingStats.recordedDurationNanos)
                    b.recLabel.text = "REC %d:%02d".format(s / 60, s % 60)
                }
                is VideoRecordEvent.Finalize -> {
                    b.recGroup.visibility = View.GONE
                    recording = null
                    status(if (ev.hasError()) getString(R.string.video_fail_fmt, ev.error) else getString(R.string.video_saved))
                    if (powerSave) provider?.unbindAll()   // 절전 중 녹화가 끝났으면 이제 카메라도 끔
                }
                else -> Unit
            }
        }
    }

    // ── 절전 / 수중 / 종료 ────────────────────────────────────────────────

    /** OLED 완전 검정 + 밝기 최소 + (녹화 중이 아니면) 카메라 해제. 아무 터치로 복귀. 화면은 계속 켜져 있음. */
    private fun setPowerSave(on: Boolean) {
        if (powerSave == on) return
        powerSave = on
        b.black.visibility = if (on) View.VISIBLE else View.GONE
        b.btnExit.visibility = if (on) View.GONE else View.VISIBLE
        window.attributes = window.attributes.also {
            it.screenBrightness = if (on) 0.01f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        }
        if (on) {
            main.removeCallbacks(closeSelector); selectorIndex = -1; renderSelector()
            if (recording == null) provider?.unbindAll()
        } else {
            lastInput = SystemClock.elapsedRealtime()
            if (recording == null) bind()
        }
    }

    private fun toggleUnderwater() {
        val cam = camera ?: return
        underwater = !underwater
        wipeTransition()                       // 전환 직전 프레임을 잡아 두고
        if (underwater) wb.apply(cam, prefs.uwStrength) else wb.clear(cam)
        renderMode()
        status(if (underwater) uwOnText() else getString(R.string.uw_off))
    }

    /** 색보정 전/후를 왼쪽→오른쪽 와이프로 보여줌: 이전 프레임을 덮어 두고 걷어낸다 */
    private fun wipeTransition() {
        val bmp = b.preview.bitmap ?: return
        val w = b.wipe.width
        val h = b.wipe.height
        if (w == 0 || h == 0) return
        b.wipe.setImageBitmap(bmp)
        b.wipe.clipBounds = null
        b.wipe.visibility = View.VISIBLE
        ValueAnimator.ofInt(0, w).apply {
            startDelay = 250          // ISP 가 새 게인을 적용할 프레임 몇 장 여유
            duration = 2000
            addUpdateListener { b.wipe.clipBounds = Rect(it.animatedValue as Int, 0, w, h) }
            doOnEnd { b.wipe.visibility = View.GONE; b.wipe.setImageBitmap(null) }
            start()
        }
    }

    private fun endDive() {
        recording?.stop()
        DiveLog.finishDive(this, prefs)
        finish()
    }

    private fun status(msg: String) {
        b.status.text = msg
        b.status.visibility = View.VISIBLE
        main.removeCallbacks(hideStatus)
        main.postDelayed(hideStatus, 2000)
    }
}
