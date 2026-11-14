package com.sejin.diveroidrevive

import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.ColorSpaceTransform
import android.hardware.camera2.params.RggbChannelVector
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.Preview
import kotlin.math.max

/**
 * 수중 색보정 = 카메라 ISP 의 화이트밸런스 게인을 직접 조정.
 * GPU 셰이더 없이 프리뷰·사진·영상에 동일하게 적용되고 전력 비용 0.
 *
 * 방식: AWB 자동일 때 결과 메타데이터에서 게인/변환행렬을 계속 받아두었다가,
 * ON 시 AWB 를 끄고 그 값을 고정한 채 빨강 게인을 키우고 파랑 게인을 줄인다
 * (물속에서 먼저 사라지는 빨강을 되살리는 것 = 다이브로이드의 "청색 보정").
 * 기기가 수동 후처리를 지원하지 않으면 AWB 프리셋(그늘)으로 간이 보정.
 */
@OptIn(ExperimentalCamera2Interop::class)
class UnderwaterWb {
    @Volatile private var autoGains: RggbChannelVector? = null
    @Volatile private var autoTransform: ColorSpaceTransform? = null
    @Volatile var enabled = false
        private set
    /** 마지막 apply 가 수동 게인이었는지(false = WB 프리셋 간이 보정) */
    var manual = false
        private set

    private val callback = object : CameraCaptureSession.CaptureCallback() {
        override fun onCaptureCompleted(s: CameraCaptureSession, req: CaptureRequest, res: TotalCaptureResult) {
            if (enabled) return   // 수동 중엔 우리가 준 값이 돌아오므로 자동값 갱신 안 함
            res.get(CaptureResult.COLOR_CORRECTION_GAINS)?.let { autoGains = it }
            res.get(CaptureResult.COLOR_CORRECTION_TRANSFORM)?.let { autoTransform = it }
        }
    }

    fun attach(builder: Preview.Builder) {
        Camera2Interop.Extender(builder).setSessionCaptureCallback(callback)
    }

    private fun manualSupported(info: CameraInfo): Boolean {
        val c = Camera2CameraInfo.from(info)
        val caps = c.getCameraCharacteristic(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES) ?: return false
        val modes = c.getCameraCharacteristic(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES) ?: return false
        return caps.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING) &&
            modes.contains(CameraMetadata.CONTROL_AWB_MODE_OFF)
    }

    /** @param strength 1(약) 2(중) 3(강) */
    fun apply(camera: Camera, strength: Int) {
        val i = (strength - 1).coerceIn(0, 2)
        val g = autoGains
        val t = autoTransform
        val b = CaptureRequestOptions.Builder()
        manual = manualSupported(camera.cameraInfo) && g != null && t != null
        if (manual && g != null && t != null) {
            val red = floatArrayOf(1.4f, 1.8f, 2.4f)[i]
            val blue = floatArrayOf(1.10f, 1.20f, 1.35f)[i]
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)
                .setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_MODE, CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
                .setCaptureRequestOption(CaptureRequest.COLOR_CORRECTION_TRANSFORM, t)
                .setCaptureRequestOption(
                    CaptureRequest.COLOR_CORRECTION_GAINS,
                    RggbChannelVector(g.red * red, g.greenEven, g.greenOdd, max(1f, g.blue / blue)),
                )
        } else {
            b.setCaptureRequestOption(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_SHADE)
        }
        enabled = true
        Camera2CameraControl.from(camera.cameraControl).setCaptureRequestOptions(b.build())
    }

    fun clear(camera: Camera) {
        enabled = false
        Camera2CameraControl.from(camera.cameraControl).clearCaptureRequestOptions()
    }
}
