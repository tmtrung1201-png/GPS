package com.example.fakegps

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

class MainActivity : Activity() {

    private lateinit var web: WebView
    private lateinit var status: TextView
    private lateinit var coord: TextView
    private lateinit var startButton: Button
    private lateinit var nameInput: EditText
    private lateinit var latInput: EditText
    private lateinit var lonInput: EditText
    private lateinit var listBox: LinearLayout

    private var selectedLat = 10.762622
    private var selectedLon = 106.660172
    private var running = false

    private val handler = Handler(Looper.getMainLooper())

    private val mock by lazy {
        MockLocation(this)
    }

    private val prefs by lazy {
        getSharedPreferences("places", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestLocationPermission()
        buildUi()
        loadPlaces()
    }

    private fun requestLocationPermission() {
        if (
            android.os.Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                10
            )
        }
    }

    private fun buildUi() {

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(
                Color.rgb(247, 243, 251)
            )
        }

        val title = TextView(this).apply {
            text = "Fake GPS"
            textSize = 22f
            setTextColor(
                Color.rgb(40, 34, 53)
            )
            setPadding(20, 18, 20, 10)
            setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )
        }

        root.addView(title)

        // MAP

        web = WebView(this).apply {

            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_DEFAULT

            webViewClient = WebViewClient()

            addJavascriptInterface(
                MapBridge(),
                "Android"
            )

            loadUrl(
                "file:///android_asset/map.html"
            )
        }

        root.addView(
            web,
            LinearLayout.LayoutParams(
                -1,
                0,
                1f
            )
        )

        // PANEL

        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 10, 16, 10)
            setBackgroundColor(
                Color.rgb(242, 234, 251)
            )
        }

        coord = TextView(this).apply {
            text = "Chạm bản đồ để chọn vị trí"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.DKGRAY)
            setPadding(0, 6, 0, 8)
        }

        panel.addView(coord)

        // DEVELOPER SETTINGS

        val devButton = Button(this).apply {
            text = "Developer Settings"

            setOnClickListener {
                openDeveloperSettings()
            }
        }

        panel.addView(devButton)

        // NAME + SAVE

        val fields = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        nameInput = EditText(this).apply {
            hint = "Tên vị trí"
            setSingleLine(true)
        }

        fields.addView(
            nameInput,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        val saveButton = Button(this).apply {
            text = "Lưu"

            setOnClickListener {
                savePlace()
            }
        }

        fields.addView(
            saveButton,
            LinearLayout.LayoutParams(
                -2,
                -2
            )
        )

        panel.addView(fields)

        // LATITUDE + LONGITUDE

        val coords = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        latInput = EditText(this).apply {
            hint = "Latitude"

            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED

            setSingleLine(true)
        }

        lonInput = EditText(this).apply {
            hint = "Longitude"

            inputType =
                android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED

            setSingleLine(true)
        }

        coords.addView(
            latInput,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        coords.addView(
            lonInput,
            LinearLayout.LayoutParams(
                0,
                -2,
                1f
            )
        )

        panel.addView(coords)

        // START MOCK

        startButton = Button(this).apply {
            text = "▶  Bắt đầu Mock"

            setOnClickListener {
                toggleMock()
            }
        }

        panel.addView(startButton)

        // STATUS

        status = TextView(this).apply {
            text = "Trạng thái: Đang tắt"
            textSize = 14f
            setTextColor(Color.DKGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 4, 0, 4)
        }

        panel.addView(status)

        // SAVED LOCATIONS

        val listTitle = TextView(this).apply {
            text = "Vị trí đã lưu"
            textSize = 17f
            setTypeface(
                null,
                android.graphics.Typeface.BOLD
            )
            setPadding(4, 8, 4, 4)
        }

        panel.addView(listTitle)

        listBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        panel.addView(listBox)

        root.addView(
            panel,
            LinearLayout.LayoutParams(
                -1,
                -2
            )
        )

        setContentView(root)
    }

    private fun openDeveloperSettings() {

        try {
            startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS
                )
            )
        } catch (_: Exception) {
            startActivity(
                Intent(
                    Settings.ACTION_SETTINGS
                )
            )
        }

        Toast.makeText(
            this,
            "Vào Select mock location app và chọn Fake GPS",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun setSelected(
        lat: Double,
        lon: Double
    ) {

        selectedLat = lat
        selectedLon = lon

        latInput.setText(
            String.format(
                Locale.US,
                "%.6f",
                lat
            )
        )

        lonInput.setText(
            String.format(
                Locale.US,
                "%.6f",
                lon
            )
        )

        coord.text = String.format(
            Locale.US,
            "Vị trí đã chọn: %.6f, %.6f",
            lat,
            lon
        )
    }

    private fun savePlace() {

        val name =
            nameInput.text
                .toString()
                .trim()
                .ifEmpty {
                    "Vị trí ${System.currentTimeMillis() % 10000}"
                }

        val lat =
            latInput.text
                .toString()
                .toDoubleOrNull()
                ?: selectedLat

        val lon =
            lonInput.text
                .toString()
                .toDoubleOrNull()
                ?: selectedLon

        if (lat !in -90.0..90.0 ||
            lon !in -180.0..180.0
        ) {
            Toast.makeText(
                this,
                "Latitude/Longitude không hợp lệ",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        selectedLat = lat
        selectedLon = lon

        val arr = JSONArray(
            prefs.getString(
                "items",
                "[]"
            )
        )

        arr.put(
            JSONObject().apply {
                put("name", name)
                put("lat", lat)
                put("lon", lon)
            }
        )

        prefs.edit()
            .putString(
                "items",
                arr.toString()
            )
            .apply()

        loadPlaces()

        Toast.makeText(
            this,
            "Đã lưu $name",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun loadPlaces() {

        if (!::listBox.isInitialized) {
            return
        }

        listBox.removeAllViews()

        val arr = JSONArray(
            prefs.getString(
                "items",
                "[]"
            )
        )

        for (i in 0 until arr.length()) {

            val index = i
            val obj = arr.getJSONObject(i)

            val lat = obj.getDouble("lat")
            val lon = obj.getDouble("lon")

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(4, 3, 4, 3)
            }

            // Đổi tên biến thành placeText để không
            // xung đột với Button.text

            val placeText = TextView(this).apply {

                text = String.format(
                    Locale.US,
                    "%s\n%.6f, %.6f",
                    obj.getString("name"),
                    lat,
                    lon
                )

                textSize = 14f

                setTextColor(
                    Color.rgb(
                        50,
                        45,
                        60
                    )
                )
            }

            row.addView(
                placeText,
                LinearLayout.LayoutParams(
                    0,
                    -2,
                    1f
                )
            )

            // PLAY

            val playButton = Button(this).apply {

                setText("▶")

                setOnClickListener {

                    setSelected(
                        lat,
                        lon
                    )

                    web.evaluateJavascript(
                        "goTo($lat,$lon)",
                        null
                    )

                    startMockAt(
                        lat,
                        lon
                    )
                }
            }

            row.addView(playButton)

            // DELETE

            val deleteButton = Button(this).apply {

                setText("X")

                setOnClickListener {
                    deletePlace(index)
                }
            }

            row.addView(deleteButton)

            listBox.addView(row)
        }
    }

    private fun deletePlace(
        index: Int
    ) {

        val arr = JSONArray(
            prefs.getString(
                "items",
                "[]"
            )
        )

        val out = JSONArray()

        for (i in 0 until arr.length()) {

            if (i != index) {
                out.put(
                    arr.get(i)
                )
            }
        }

        prefs.edit()
            .putString(
                "items",
                out.toString()
            )
            .apply()

        loadPlaces()
    }

    private fun toggleMock() {

        if (running) {

            stopMock()

        } else {

            val lat =
                latInput.text
                    .toString()
                    .toDoubleOrNull()
                    ?: selectedLat

            val lon =
                lonInput.text
                    .toString()
                    .toDoubleOrNull()
                    ?: selectedLon

            startMockAt(
                lat,
                lon
            )
        }
    }

    private fun startMockAt(
        lat: Double,
        lon: Double
    ) {

        if (
            lat !in -90.0..90.0 ||
            lon !in -180.0..180.0
        ) {

            Toast.makeText(
                this,
                "Latitude/Longitude không hợp lệ",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        selectedLat = lat
        selectedLon = lon

        latInput.setText(
            String.format(
                Locale.US,
                "%.6f",
                lat
            )
        )

        lonInput.setText(
            String.format(
                Locale.US,
                "%.6f",
                lon
            )
        )

        if (running) {

            mock.update(
                lat,
                lon
            )

            status.text = String.format(
                Locale.US,
                "Trạng thái: ĐANG BẬT  •  %.6f, %.6f",
                lat,
                lon
            )

            return
        }

        if (mock.start(lat, lon)) {

            running = true

            startButton.text =
                "■  Dừng Mock"

            status.text = String.format(
                Locale.US,
                "Trạng thái: ĐANG BẬT  •  %.6f, %.6f",
                lat,
                lon
            )

            handler.post(
                object : Runnable {

                    override fun run() {

                        if (running) {

                            mock.update(
                                selectedLat,
                                selectedLon
                            )

                            handler.postDelayed(
                                this,
                                1000
                            )
                        }
                    }
                }
            )

        } else {

            Toast.makeText(
                this,
                "Không thể bật mock. Hãy chọn app này trong Developer options → Select mock location app.",
                Toast.LENGTH_LONG
            ).show()

            openDeveloperSettings()
        }
    }

    private fun stopMock() {

        running = false

        handler.removeCallbacksAndMessages(
            null
        )

        mock.stop()

        startButton.text =
            "▶  Bắt đầu Mock"

        status.text =
            "Trạng thái: Đang tắt"
    }

    override fun onDestroy() {

        stopMock()

        if (::web.isInitialized) {
            web.destroy()
        }

        super.onDestroy()
    }

    inner class MapBridge {

        @JavascriptInterface
        fun onMapClick(
            lat: Double,
            lon: Double
        ) {

            runOnUiThread {

                setSelected(
                    lat,
                    lon
                )
            }
        }
    }
}
