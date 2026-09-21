package com.example.fakegps

import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock

class MockLocation(context: Context) {
    private val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val provider = LocationManager.GPS_PROVIDER
    private var added = false

    fun start(lat: Double, lon: Double): Boolean = try {
        try { lm.removeTestProvider(provider) } catch (_: Exception) {}
        lm.addTestProvider(provider, false, false, false, false, true, true, true,
            android.location.Criteria.POWER_HIGH, android.location.Criteria.ACCURACY_FINE)
        added = true
        lm.setTestProviderEnabled(provider, true)
        update(lat, lon)
        true
    } catch (_: SecurityException) { false }
      catch (_: IllegalArgumentException) { false }

    fun update(lat: Double, lon: Double) {
        if (!added) return
        try {
            val l = Location(provider).apply {
                latitude = lat; longitude = lon
                accuracy = 5f
                time = System.currentTimeMillis()
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    verticalAccuracyMeters = 5f
                    speed = 0f
                    bearing = 0f
                }
            }
            lm.setTestProviderLocation(provider, l)
        } catch (_: Exception) {}
    }

    fun stop() {
        if (!added) return
        try { lm.setTestProviderEnabled(provider, false) } catch (_: Exception) {}
        try { lm.removeTestProvider(provider) } catch (_: Exception) {}
        added = false
    }
}
