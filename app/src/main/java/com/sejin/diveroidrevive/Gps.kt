package com.sejin.diveroidrevive

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.Locale

/** 위치 1회 취득 + 역지오코딩. Play Services 없이 LocationManager/Geocoder 만. 실패는 전부 null. */
object Gps {
    fun granted(c: Context) =
        ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun current(c: Context, cb: (Location?) -> Unit) {
        if (!granted(c)) { cb(null); return }
        val lm = c.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            Build.VERSION.SDK_INT >= 31 && lm.isProviderEnabled(LocationManager.FUSED_PROVIDER) -> LocationManager.FUSED_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> { cb(null); return }
        }
        // getCurrentLocation 은 내부 타임아웃(~30s) 후 null → 마지막 알려진 위치로 대체
        lm.getCurrentLocation(provider, null, ContextCompat.getMainExecutor(c)) { loc ->
            cb(loc ?: lm.getLastKnownLocation(provider))
        }
    }

    fun fmt(l: Location) = String.format(Locale.US, "%.5f,%.5f", l.latitude, l.longitude)

    /**
     * 좌표 → 가장 구체적인 지명 하나 (영어). 예: "Hallim-eup", "Koh Tao".
     * Geocoder 는 네트워크를 쓰므로 백그라운드 스레드에서, 결과는 메인 스레드로.
     */
    fun place(c: Context, l: Location, cb: (String?) -> Unit) {
        if (!Geocoder.isPresent()) { cb(null); return }
        Thread {
            val name = try {
                @Suppress("DEPRECATION")
                Geocoder(c, Locale.ENGLISH).getFromLocation(l.latitude, l.longitude, 1)?.firstOrNull()?.let { a ->
                    a.subLocality ?: a.locality ?: a.subAdminArea ?: a.adminArea ?: a.countryName
                }
            } catch (e: Exception) {
                null
            }
            Handler(Looper.getMainLooper()).post { cb(name) }
        }.start()
    }
}
