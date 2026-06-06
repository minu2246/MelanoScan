package com.example.melanoscan

import android.Manifest
import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class Search_Hospital : AppCompatActivity() {

    private val bgColor = Color.rgb(240, 249, 243)
    private val cardColor = Color.WHITE

    private val green = Color.rgb(67, 153, 75)
    private val deepGreen = Color.rgb(48, 120, 58)
    private val softGreen = Color.rgb(232, 247, 236)
    private val verySoftGreen = Color.rgb(246, 252, 248)

    private val darkText = Color.rgb(34, 44, 58)
    private val subText = Color.rgb(110, 125, 118)
    private val borderColor = Color.rgb(207, 232, 214)

    private lateinit var resultLayout: LinearLayout

    private val locationPermissionCode = 100

    // 여기에 본인 카카오 REST API 키 넣기
    private val kakaoRestApiKey = "42f288e282fbfefdca0b1edd3e9090f9"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUi()
    }

    private fun setupUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dp(22), dp(34), dp(22), dp(26))
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val backButton = TextView(this).apply {
            text = ""
            isClickable = true
            isFocusable = true

            setOnClickListener {
                finish()
            }
        }

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        val title = TextView(this).apply {
            text = "인근 병원 찾기"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            includeFontPadding = false
        }

        val sub = TextView(this).apply {
            text = "현재 위치 기준 가까운 피부과를 추천합니다"
            textSize = 13f
            setTextColor(subText)
            setPadding(0, dp(6), 0, 0)
            includeFontPadding = false
        }

        titleBox.addView(title)
        titleBox.addView(sub)

        topRow.addView(
            backButton,
            LinearLayout.LayoutParams(dp(44), dp(44))
        )

        topRow.addView(
            titleBox,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val infoCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = roundedBg(Color.WHITE, 22, borderColor, 1)
            elevation = dp(2).toFloat()
        }

        val infoIcon = TextView(this).apply {
            text = "🏥"
            textSize = 25f
            gravity = Gravity.CENTER
            background = roundedBg(softGreen, 100)
        }

        val infoTextBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        val infoTitle = TextView(this).apply {
            text = "가까운 피부과를 빠르게 확인"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(deepGreen)
            includeFontPadding = false
        }

        val infoSub = TextView(this).apply {
            text = "AI 분석 결과는 참고용이며, 정확한 진단은 전문의 상담이 필요합니다."
            textSize = 12.5f
            setTextColor(subText)
            setPadding(0, dp(6), 0, 0)
            setLineSpacing(dp(2).toFloat(), 1.0f)
        }

        infoTextBox.addView(infoTitle)
        infoTextBox.addView(infoSub)

        infoCard.addView(
            infoIcon,
            LinearLayout.LayoutParams(dp(48), dp(48))
        )

        infoCard.addView(
            infoTextBox,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        val searchButton = TextView(this).apply {
            text = "내 주변 피부과 검색하기"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBg(green, 18)
            elevation = dp(3).toFloat()
            isClickable = true
            isFocusable = true

            setOnClickListener {
                checkLocationPermissionAndSearch()
            }
        }

        resultLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(18), 0, 0)
        }

        root.addView(topRow)

        root.addView(
            infoCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(22)
            }
        )

        root.addView(
            searchButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
        )

        root.addView(resultLayout)

        val scrollView = ScrollView(this).apply {
            clipToPadding = false
            addView(root)
        }

        setContentView(scrollView)
    }

    private fun checkLocationPermissionAndSearch() {
        val finePermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val coarsePermission = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (
            finePermission != PackageManager.PERMISSION_GRANTED &&
            coarsePermission != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                locationPermissionCode
            )
        } else {
            getCurrentLocationAndSearch()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getCurrentLocationAndSearch() {
        resultLayout.removeAllViews()

        showLoadingCard(
            title = "현재 위치 확인 중",
            message = "GPS와 네트워크 위치를 확인하고 있습니다."
        )

        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->

            if (location == null) {
                resultLayout.removeAllViews()

                addStatusCard(
                    icon = "⚠️",
                    title = "위치를 가져올 수 없습니다",
                    message = "휴대폰 위치 기능을 켜고, 앱 위치 권한을 허용한 뒤 다시 시도해주세요.\n에뮬레이터라면 Location 좌표를 직접 설정해야 합니다."
                )

                return@addOnSuccessListener
            }

            resultLayout.removeAllViews()

            showLoadingCard(
                title = "피부과 검색 중",
                message = "현재 위치 기준으로 가까운 피부과를 불러오고 있습니다."
            )

            searchDermatologyByKakao(
                userLatitude = location.latitude,
                userLongitude = location.longitude
            )

        }.addOnFailureListener {
            resultLayout.removeAllViews()

            addStatusCard(
                icon = "⚠️",
                title = "위치 확인 실패",
                message = it.message ?: "알 수 없는 오류가 발생했습니다."
            )
        }
    }

    private fun searchDermatologyByKakao(
        userLatitude: Double,
        userLongitude: Double
    ) {
        thread {
            try {
                val query = java.net.URLEncoder.encode("피부과", "UTF-8")

                val urlText =
                    "https://dapi.kakao.com/v2/local/search/keyword.json" +
                            "?query=$query" +
                            "&x=$userLongitude" +
                            "&y=$userLatitude" +
                            "&radius=5000" +
                            "&sort=distance" +
                            "&size=15"

                val url = URL(urlText)
                val connection = url.openConnection() as HttpURLConnection

                connection.requestMethod = "GET"
                connection.setRequestProperty(
                    "Authorization",
                    "KakaoAK $kakaoRestApiKey"
                )

                val responseCode = connection.responseCode

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    val errorText = connection.errorStream?.bufferedReader()?.use {
                        it.readText()
                    } ?: "에러 내용 없음"

                    runOnUiThread {
                        resultLayout.removeAllViews()

                        addStatusCard(
                            icon = "⚠️",
                            title = "카카오 API 요청 실패",
                            message = "응답 코드: $responseCode\n\n$errorText"
                        )
                    }

                    connection.disconnect()
                    return@thread
                }

                val responseText = connection.inputStream.bufferedReader().use {
                    it.readText()
                }

                connection.disconnect()

                val jsonObject = JSONObject(responseText)
                val documents = jsonObject.getJSONArray("documents")

                runOnUiThread {
                    resultLayout.removeAllViews()

                    if (documents.length() == 0) {
                        addStatusCard(
                            icon = "😥",
                            title = "검색 결과 없음",
                            message = "근처 5km 안에서 피부과를 찾지 못했습니다."
                        )

                        return@runOnUiThread
                    }

                    addResultHeader(documents.length())

                    for (i in 0 until documents.length()) {
                        val item = documents.getJSONObject(i)

                        val name = item.optString("place_name", "병원명 없음")

                        val roadAddress = item.optString("road_address_name", "")
                        val addressName = item.optString("address_name", "주소 없음")

                        val address = if (roadAddress.isNotBlank()) {
                            roadAddress
                        } else {
                            addressName
                        }

                        val phone = item.optString("phone", "")

                        val hospitalLongitude = item.optString("x", "0").toDoubleOrNull() ?: 0.0
                        val hospitalLatitude = item.optString("y", "0").toDoubleOrNull() ?: 0.0

                        val kakaoDistance = item.optString("distance", "")

                        val finalDistanceText = if (kakaoDistance.isNotBlank()) {
                            formatDistance(kakaoDistance)
                        } else {
                            val calculatedDistance = calculateDistanceKm(
                                userLatitude,
                                userLongitude,
                                hospitalLatitude,
                                hospitalLongitude
                            )

                            String.format("%.1fkm", calculatedDistance)
                        }

                        addHospitalCard(
                            index = i + 1,
                            name = name,
                            address = address,
                            phone = phone,
                            distanceText = finalDistanceText,
                            hospitalLatitude = hospitalLatitude,
                            hospitalLongitude = hospitalLongitude
                        )
                    }
                }

            } catch (e: Exception) {
                runOnUiThread {
                    resultLayout.removeAllViews()

                    addStatusCard(
                        icon = "⚠️",
                        title = "오류가 발생했습니다",
                        message = e.message ?: "알 수 없는 오류입니다."
                    )
                }
            }
        }
    }

    private fun showLoadingCard(
        title: String,
        message: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(18), dp(24), dp(18), dp(24))
            background = roundedBg(Color.WHITE, 22, borderColor, 1)
            elevation = dp(3).toFloat()
        }

        val progressBar = ProgressBar(this).apply {
            isIndeterminate = true
            indeterminateTintList = ColorStateList.valueOf(green)
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(deepGreen)
            gravity = Gravity.CENTER
            setPadding(0, dp(16), 0, 0)
        }

        val messageView = TextView(this).apply {
            text = message
            textSize = 12.5f
            setTextColor(subText)
            gravity = Gravity.CENTER
            setPadding(0, dp(7), 0, 0)
            setLineSpacing(dp(2).toFloat(), 1.0f)
        }

        card.addView(
            progressBar,
            LinearLayout.LayoutParams(dp(54), dp(54))
        )

        card.addView(titleView)
        card.addView(messageView)

        resultLayout.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun addResultHeader(count: Int) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(14))
        }

        val title = TextView(this).apply {
            text = "가까운 피부과 " + count + "곳"
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            includeFontPadding = false
        }

        val sub = TextView(this).apply {
            text = "거리순으로 정렬되어 있습니다. 원하는 병원의 길찾기 방식을 선택하세요."
            textSize = 12.5f
            setTextColor(subText)
            setPadding(0, dp(7), 0, 0)
        }

        header.addView(title)
        header.addView(sub)

        resultLayout.addView(header)
    }

    private fun addHospitalCard(
        index: Int,
        name: String,
        address: String,
        phone: String,
        distanceText: String,
        hospitalLatitude: Double,
        hospitalLongitude: Double
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBg(cardColor, 22, borderColor, 1)
            elevation = dp(3).toFloat()
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val numberCircle = TextView(this).apply {
            text = index.toString()
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = roundedBg(green, 100)
        }

        val nameBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, dp(8), 0)
        }

        val nameText = TextView(this).apply {
            text = name
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            maxLines = 2
        }

        val categoryText = TextView(this).apply {
            text = "피부과 · 병원"
            textSize = 11.5f
            setTextColor(subText)
            setPadding(0, dp(4), 0, 0)
            includeFontPadding = false
        }

        nameBox.addView(nameText)
        nameBox.addView(categoryText)

        val distanceChip = TextView(this).apply {
            text = distanceText
            textSize = 12.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(deepGreen)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = roundedBg(softGreen, 100, borderColor, 1)
        }

        topRow.addView(
            numberCircle,
            LinearLayout.LayoutParams(dp(34), dp(34))
        )

        topRow.addView(
            nameBox,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        topRow.addView(
            distanceChip,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val addressRow = makeInfoLine(
            icon = "📍",
            textValue = address
        )

        val phoneRow = makeInfoLine(
            icon = "☎",
            textValue = if (phone.isBlank()) "전화번호 정보 없음" else phone
        )

        val divider = View(this).apply {
            setBackgroundColor(Color.rgb(232, 240, 235))
        }

        val mapButton = makeWideButton("카카오맵에서 위치 보기") {
            openKakaoMapLook(
                name = name,
                latitude = hospitalLatitude,
                longitude = hospitalLongitude
            )
        }

        val routeLabel = TextView(this).apply {
            text = "길찾기"
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            setPadding(0, dp(12), 0, dp(8))
        }

        val routeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val carButton = makeRouteButton("🚗 자동차") {
            openKakaoRoute(
                latitude = hospitalLatitude,
                longitude = hospitalLongitude,
                by = "car",
                placeName = name
            )
        }

        val publicButton = makeRouteButton("🚌 대중교통") {
            openKakaoRoute(
                latitude = hospitalLatitude,
                longitude = hospitalLongitude,
                by = "publictransit",
                placeName = name
            )
        }

        val walkButton = makeRouteButton("🚶 도보") {
            openKakaoRoute(
                latitude = hospitalLatitude,
                longitude = hospitalLongitude,
                by = "foot",
                placeName = name
            )
        }

        routeRow.addView(
            carButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = dp(4)
            }
        )

        routeRow.addView(
            publicButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
            }
        )

        routeRow.addView(
            walkButton,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dp(4)
            }
        )

        card.addView(topRow)

        card.addView(
            addressRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(14)
            }
        )

        card.addView(
            phoneRow,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(7)
            }
        )

        card.addView(
            divider,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            ).apply {
                topMargin = dp(14)
                bottomMargin = dp(14)
            }
        )

        card.addView(mapButton)
        card.addView(routeLabel)
        card.addView(routeRow)

        resultLayout.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        )
    }

    private fun makeInfoLine(
        icon: String,
        textValue: String
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val iconView = TextView(this@Search_Hospital).apply {
                text = icon
                textSize = 14f
                gravity = Gravity.CENTER
            }

            val textView = TextView(this@Search_Hospital).apply {
                text = textValue
                textSize = 13f
                setTextColor(subText)
                setPadding(dp(8), 0, 0, 0)
                setLineSpacing(dp(2).toFloat(), 1.0f)
            }

            addView(
                iconView,
                LinearLayout.LayoutParams(dp(22), dp(22))
            )

            addView(
                textView,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )
        }
    }

    private fun makeWideButton(
        textValue: String,
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(12), dp(12), dp(12), dp(12))
            background = roundedBg(green, 15)
            isClickable = true
            isFocusable = true

            setOnClickListener {
                onClick()
            }
        }
    }

    private fun makeRouteButton(
        textValue: String,
        onClick: () -> Unit
    ): TextView {
        return TextView(this).apply {
            text = textValue
            textSize = 11.5f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(deepGreen)
            gravity = Gravity.CENTER
            setPadding(dp(4), dp(10), dp(4), dp(10))
            background = roundedBg(verySoftGreen, 14, borderColor, 1)
            isClickable = true
            isFocusable = true
            maxLines = 1

            setOnClickListener {
                onClick()
            }
        }
    }

    private fun openKakaoMapLook(
        name: String,
        latitude: Double,
        longitude: Double
    ) {
        val appUri = Uri.parse(
            "kakaomap://look?p=$latitude,$longitude"
        )

        val webUri = Uri.parse(
            "https://map.kakao.com/link/map/${Uri.encode(name)},$latitude,$longitude"
        )

        openKakaoOrWeb(appUri, webUri)
    }

    private fun openKakaoRoute(
        latitude: Double,
        longitude: Double,
        by: String,
        placeName: String
    ) {
        val appUri = Uri.parse(
            "kakaomap://route?ep=$latitude,$longitude&by=$by"
        )

        val webUri = Uri.parse(
            "https://map.kakao.com/link/to/${Uri.encode(placeName)},$latitude,$longitude"
        )

        openKakaoOrWeb(appUri, webUri)
    }

    private fun openKakaoOrWeb(
        appUri: Uri,
        webUri: Uri
    ) {
        val appIntent = Intent(Intent.ACTION_VIEW, appUri)

        try {
            startActivity(appIntent)
        } catch (e: ActivityNotFoundException) {
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            startActivity(webIntent)
        } catch (e: Exception) {
            val webIntent = Intent(Intent.ACTION_VIEW, webUri)
            startActivity(webIntent)
        }
    }

    private fun addStatusCard(
        icon: String,
        title: String,
        message: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBg(Color.WHITE, 20, borderColor, 1)
            elevation = dp(2).toFloat()
        }

        val iconView = TextView(this).apply {
            text = icon
            textSize = 24f
            gravity = Gravity.CENTER
            background = roundedBg(softGreen, 100)
        }

        val textBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            includeFontPadding = false
        }

        val messageView = TextView(this).apply {
            text = message
            textSize = 12.5f
            setTextColor(subText)
            setPadding(0, dp(6), 0, 0)
            setLineSpacing(dp(2).toFloat(), 1.0f)
        }

        textBox.addView(titleView)
        textBox.addView(messageView)

        card.addView(
            iconView,
            LinearLayout.LayoutParams(dp(46), dp(46))
        )

        card.addView(
            textBox,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        resultLayout.addView(
            card,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
    }

    private fun formatDistance(distanceMeter: String): String {
        val meter = distanceMeter.toDoubleOrNull() ?: return "거리 정보 없음"

        return if (meter < 1000) {
            "${meter.toInt()}m"
        } else {
            String.format("%.1fkm", meter / 1000)
        }
    }

    private fun calculateDistanceKm(
        userLat: Double,
        userLng: Double,
        hospitalLat: Double,
        hospitalLng: Double
    ): Double {
        val earthRadius = 6371.0

        val dLat = Math.toRadians(hospitalLat - userLat)
        val dLng = Math.toRadians(hospitalLng - userLng)

        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(userLat)) *
                cos(Math.toRadians(hospitalLat)) *
                sin(dLng / 2).pow(2.0)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    private fun roundedBg(
        color: Int,
        radiusDp: Int,
        strokeColor: Int? = null,
        strokeWidthDp: Int = 0
    ): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()

            if (strokeColor != null && strokeWidthDp > 0) {
                setStroke(dp(strokeWidthDp), strokeColor)
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == locationPermissionCode) {
            if (
                grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                getCurrentLocationAndSearch()
            } else {
                Toast.makeText(
                    this,
                    "위치 권한이 필요합니다.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}