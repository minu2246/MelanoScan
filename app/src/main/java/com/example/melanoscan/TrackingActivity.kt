package com.example.melanoscan

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.tensorflow.lite.Interpreter
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.ArrayDeque
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sqrt

class TrackingActivity : AppCompatActivity() {

    private lateinit var imageView: RoiOverlayImageView
    private lateinit var imageGuideTextView: TextView
    private lateinit var resultTextView: TextView
    private lateinit var roiSelectButton: Button

    private lateinit var coinInterpreter: Interpreter
    private lateinit var db: AppDatabase

    private val coinInputSize = 640
    private val coinConfThreshold = 0.20f

    private val aLowThreshold = 0.30
    private val aHighThreshold = 0.50

    private val bLowThreshold = 0.25
    private val bHighThreshold = 0.45

    private val cLowThreshold = 0.30
    private val cHighThreshold = 0.60

    private var currentBitmap: Bitmap? = null
    private var currentCoinResult: CoinDetectionResult? = null
    private var currentRoiRect: RectF? = null
    private var currentImageName: String? = null
    private var currentMeasurementImagePath: String? = null

    private var initialPredClass: String? = null
    private var initialMelanomaProb: Float = -1f

    private var currentLesionId: Long = -1L
    private var currentLesionName: String? = null

    private var cameraImageUri: Uri? = null

    private val bgColor = Color.rgb(246, 250, 247)
    private val cardColor = Color.WHITE
    private val green = Color.rgb(67, 153, 75)
    private val darkText = Color.rgb(35, 45, 55)
    private val subText = Color.rgb(110, 120, 125)
    private val lightGreen = Color.rgb(237, 248, 241)

    private val trackingImagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleTrackingImage(uri)
        }
    }

    private val trackingCameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = cameraImageUri
            if (uri != null) {
                handleTrackingImage(uri)
            } else {
                setStatus("촬영 이미지를 불러오지 못했습니다.")
            }
        } else {
            setStatus("카메라 촬영이 취소되었습니다.")
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            openCamera()
        } else {
            setStatus("카메라 권한이 필요합니다. 갤러리에서 사진을 선택할 수도 있습니다.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            val modelBuffer = loadModelFile("coin_yolo_v3_640_float32.tflite")
            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }

            coinInterpreter = Interpreter(modelBuffer, options)
            db = AppDatabase.getDatabase(applicationContext)

        } catch (e: Exception) {
            showSimpleError("동전 모델 또는 DB 로드 실패\n\n${e.message}\n\n${e.stackTraceToString()}")
            return
        }

        setupUi()
    }

    private fun setupUi() {
        val predClass = intent.getStringExtra("pred_class") ?: "unknown"
        val melanomaProb = intent.getFloatExtra("melanoma_prob", -1f)

        currentLesionId = intent.getLongExtra("lesion_id", -1L)
        currentLesionName = intent.getStringExtra("lesion_name") ?: "이름 없는 병변"

        initialPredClass = predClass
        initialMelanomaProb = melanomaProb

        val previousResultText = if (melanomaProb >= 0f) {
            "이전 AI 결과: $predClass · melanoma ${"%.4f".format(melanomaProb)}"
        } else {
            "분석 방식: 직접 추적"
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }

        val titleTextView = TextView(this).apply {
            text = "병변 크기 측정"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            setPadding(0, 0, 0, dp(6))
        }

        val guideTextView = TextView(this).apply {
            text = "병변과 100원 동전이 함께 보이는 사진을 촬영하거나 선택한 뒤, 병변 주변을 드래그해주세요."
            textSize = 14f
            setTextColor(subText)
            setPadding(0, 0, 0, dp(18))
        }

        val lesionInfoCard = makeCard().apply {
            addView(TextView(this@TrackingActivity).apply {
                text = "추적 병변"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            addView(TextView(this@TrackingActivity).apply {
                text = currentLesionName ?: "이름 없는 병변"
                textSize = 22f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(green)
                setPadding(0, dp(8), 0, dp(4))
            })

            addView(TextView(this@TrackingActivity).apply {
                text = previousResultText
                textSize = 13f
                setTextColor(subText)
            })
        }

        val inputCard = makeCard().apply {
            addView(TextView(this@TrackingActivity).apply {
                text = "측정 사진 입력"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            addView(TextView(this@TrackingActivity).apply {
                text = "병변 옆에 100원 동전을 놓고 같은 피부 평면에서 촬영해주세요."
                textSize = 13f
                setTextColor(subText)
                setPadding(0, dp(6), 0, dp(14))
            })

            val buttonRow = LinearLayout(this@TrackingActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val cameraButton = Button(this@TrackingActivity).apply {
                text = "카메라로 촬영"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = roundedBg(green, 14)
                setOnClickListener {
                    requestCameraOrOpen()
                }
            }

            val galleryButton = Button(this@TrackingActivity).apply {
                text = "갤러리에서 선택"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(green)
                background = roundedBg(lightGreen, 14)
                setOnClickListener {
                    trackingImagePickerLauncher.launch("image/*")
                }
            }

            buttonRow.addView(
                cameraButton,
                LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                    marginEnd = dp(6)
                }
            )

            buttonRow.addView(
                galleryButton,
                LinearLayout.LayoutParams(0, dp(52), 1f).apply {
                    marginStart = dp(6)
                }
            )

            addView(buttonRow)
        }

        val imageCard = makeCard().apply {
            addView(TextView(this@TrackingActivity).apply {
                text = "측정 이미지"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            imageGuideTextView = TextView(this@TrackingActivity).apply {
                text = "사진을 선택하면 동전 검출 결과와 안내가 표시됩니다."
                textSize = 13f
                setTextColor(subText)
                setPadding(0, dp(6), 0, dp(12))
            }

            addView(imageGuideTextView)

            imageView = RoiOverlayImageView(this@TrackingActivity).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                minimumHeight = dp(240)
                background = roundedBg(Color.rgb(235, 238, 236), 16)

                onRoiChanged = { roi ->
                    currentRoiRect = roi
                    updateResultText()
                }

                onRoiFinished = {
                    roiSelectButton.text = "병변 ROI 다시 선택"
                    showConfirmRoiDialog()
                }
            }

            addView(
                imageView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(260)
                ).apply {
                    topMargin = dp(10)
                }
            )
        }

        val roiCard = makeCard().apply {
            addView(TextView(this@TrackingActivity).apply {
                text = "ROI 선택"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            addView(TextView(this@TrackingActivity).apply {
                text = "동전이 검출된 뒤, 병변이 완전히 포함되도록 주변 피부를 조금 포함해 드래그해주세요."
                textSize = 13f
                setTextColor(subText)
                setPadding(0, dp(6), 0, dp(14))
            })

            roiSelectButton = Button(this@TrackingActivity).apply {
                text = "병변 ROI 선택 시작"
                isEnabled = false
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = roundedBg(Color.rgb(170, 180, 175), 14)

                setOnClickListener {
                    if (currentBitmap == null || currentCoinResult == null) {
                        setStatus("먼저 병변과 100원 동전이 함께 보이는 사진을 선택하고 동전 검출을 완료해야 합니다.")
                        return@setOnClickListener
                    }

                    imageView.clearRoi()
                    imageView.clearLesionMaskOverlay()
                    currentRoiRect = null

                    imageView.roiEditEnabled = true
                    text = "이미지 위에서 병변 주변을 드래그하세요"

                    setStatus("ROI 선택 모드입니다. 이미지 위에서 병변 주변을 드래그해주세요.")
                }
            }

            addView(
                roiSelectButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52)
                )
            )
        }

        resultTextView = TextView(this).apply {
            visibility = View.GONE
        }

        rootLayout.addView(titleTextView)
        rootLayout.addView(guideTextView)
        rootLayout.addView(lesionInfoCard)
        rootLayout.addView(inputCard)
        rootLayout.addView(imageCard)
        rootLayout.addView(roiCard)
        rootLayout.addView(resultTextView)

        val scrollView = ScrollView(this).apply {
            addView(rootLayout)
        }

        setContentView(scrollView)
    }

    private fun requestCameraOrOpen() {
        val permission = Manifest.permission.CAMERA

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openCamera()
        } else {
            cameraPermissionLauncher.launch(permission)
        }
    }

    private fun openCamera() {
        try {
            val dir = File(cacheDir, "camera_images")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val file = File(dir, "tracking_camera_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            cameraImageUri = uri
            trackingCameraLauncher.launch(uri)

        } catch (e: Exception) {
            setStatus("카메라 실행 실패\n${e.message}")
        }
    }

    private fun handleTrackingImage(uri: Uri) {
        try {
            val bitmap = uriToBitmap(uri)
            val imageName = getFileName(uri) ?: "selected tracking image"

            currentBitmap = bitmap
            currentImageName = imageName
            currentMeasurementImagePath = saveMeasurementImageToInternalStorage(bitmap)
            currentRoiRect = null

            imageView.clearRoi()
            imageView.clearLesionMaskOverlay()

            val coinResult = detectCoin(bitmap)
            currentCoinResult = coinResult

            roiSelectButton.isEnabled = coinResult != null
            roiSelectButton.text = "병변 ROI 선택 시작"

            imageView.setImageBitmap(bitmap)

            if (coinResult != null) {
                imageView.setCoinBox(
                    RectF(
                        coinResult.x1,
                        coinResult.y1,
                        coinResult.x2,
                        coinResult.y2
                    )
                )

                roiSelectButton.setTextColor(Color.WHITE)
                roiSelectButton.background = roundedBg(green, 14)

                setStatus(
                    """
                    동전 검출 완료

                    100원 동전이 인식되었습니다.
                    [병변 ROI 선택 시작]을 누른 뒤 병변 주변을 드래그하세요.
                    """.trimIndent()
                )
            } else {
                imageView.setCoinBox(null)

                roiSelectButton.setTextColor(Color.WHITE)
                roiSelectButton.background = roundedBg(Color.rgb(170, 180, 175), 14)

                setStatus(
                    """
                    동전 검출 실패

                    100원 동전이 잘 보이도록 다시 촬영하거나 다른 사진을 선택해주세요.
                    """.trimIndent()
                )
            }

        } catch (e: Exception) {
            setStatus("이미지 분석 실패\n\n${e.message}")
        }
    }

    private fun showConfirmRoiDialog() {
        val roi = currentRoiRect

        if (roi == null || roi.width() < 5f || roi.height() < 5f) {
            AlertDialog.Builder(this)
                .setTitle("ROI 선택 오류")
                .setMessage("선택한 영역이 너무 작습니다. 병변 주변을 다시 드래그해주세요.")
                .setPositiveButton("다시 선택") { _, _ ->
                    restartRoiSelection()
                }
                .show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("병변 ROI 확정")
            .setMessage("선택한 영역을 병변 분석 영역으로 사용할까요?")
            .setPositiveButton("분석하기") { _, _ ->
                confirmCurrentRoiAndCalculateSize()
            }
            .setNegativeButton("다시 선택") { _, _ ->
                restartRoiSelection()
            }
            .show()
    }

    private fun confirmCurrentRoiAndCalculateSize() {
        val bitmap = currentBitmap
        val coin = currentCoinResult
        val roi = currentRoiRect

        if (bitmap == null || coin == null || roi == null) {
            setStatus("이미지, 동전 또는 병변 ROI 정보가 없습니다. 다시 선택해주세요.")
            return
        }

        roiSelectButton.text = "병변 ROI 다시 선택"
        imageView.roiEditEnabled = false

        try {
            val sizeResult = calculateLesionSizeFromRoi(
                bitmap = bitmap,
                roi = roi,
                coin = coin
            )

            val abcResult = calculateAbcFeatures(
                bitmap = bitmap,
                lesionPoints = sizeResult.lesionPoints
            )

            imageView.setLesionMaskOverlay(
                points = sizeResult.lesionPoints,
                axisX1 = sizeResult.axisX1,
                axisY1 = sizeResult.axisY1,
                axisX2 = sizeResult.axisX2,
                axisY2 = sizeResult.axisY2,
                eqDiameterMm = sizeResult.eqDiameterMm,
                longestAxisMm = sizeResult.longestAxisMm
            )

            setStatus(
                """
                측정 완료

                최장축: ${"%.2f".format(sizeResult.longestAxisMm)} mm
                병변 기록이 저장되고 있습니다.
                """.trimIndent()
            )

            saveMeasurementToDb(
                coin = coin,
                roi = roi,
                sizeResult = sizeResult
            )

        } catch (e: Exception) {
            setStatus("병변 크기 계산 실패\n\n${e.message}\n\nROI를 다시 선택해주세요.")
        }
    }

    private fun saveMeasurementToDb(
        coin: CoinDetectionResult,
        roi: RectF,
        sizeResult: LesionSizeResult
    ) {
        if (currentLesionId <= 0L) {
            runOnUiThread {
                setStatus("DB 저장 실패\n\n병변 ID가 없습니다.")
            }
            return
        }

        val entity = LesionMeasurementEntity(
            lesionId = currentLesionId,

            createdAt = System.currentTimeMillis(),

            imageName = currentImageName,
            measurementImagePath = currentMeasurementImagePath,

            predClass = initialPredClass,
            melanomaProb = initialMelanomaProb,

            coinPx = coin.coinPx,
            coinRealMm = 24.0,

            roiX1 = roi.left,
            roiY1 = roi.top,
            roiX2 = roi.right,
            roiY2 = roi.bottom,
            roiWidthPx = roi.width(),
            roiHeightPx = roi.height(),

            lesionAreaPx = sizeResult.lesionAreaPx,
            lesionEqDiameterPx = sizeResult.eqDiameterPx,
            lesionLongestAxisPx = sizeResult.longestAxisPx,

            lesionEqDiameterMm = sizeResult.eqDiameterMm,
            lesionLongestAxisMm = sizeResult.longestAxisMm
        )

        Thread {
            try {
                val savedId = db.lesionMeasurementDao().insert(entity)
                val totalCount = db.lesionMeasurementDao().countByLesionId(currentLesionId)

                saveLatestDiameterForAbcd(sizeResult.longestAxisMm.toFloat())

                runOnUiThread {
                    setStatus(
                        """
                측정 저장 완료

                병변 이름: ${currentLesionName ?: "unknown"}
                최장축: ${"%.2f".format(sizeResult.longestAxisMm)} mm
                저장 ID: $savedId
                해당 병변 기록 수: $totalCount 회
                """.trimIndent()
                    )
                }
            } catch (e: Exception) {
                runOnUiThread {
                    setStatus(
                        """
                        DB 저장 실패

                        ${e.message}
                        """.trimIndent()
                    )
                }
            }
        }.start()
    }

    private fun restartRoiSelection() {
        imageView.clearRoi()
        imageView.clearLesionMaskOverlay()
        currentRoiRect = null

        imageView.roiEditEnabled = true
        roiSelectButton.text = "이미지 위에서 병변 주변을 드래그하세요"

        setStatus("ROI를 다시 선택합니다. 병변 주변을 드래그해주세요.")
    }

    private fun updateResultText() {
        val coin = currentCoinResult
        val roi = currentRoiRect

        setStatus(
            buildString {
                if (coin != null) {

                    appendLine("병변 ROI 선택 중입니다.")
                    appendLine("병변이 박스 안에 충분히 포함되도록 조정해주세요.")
                } else {
                    appendLine("동전 검출 실패")
                    appendLine("100원 동전이 잘 보이도록 다시 촬영해주세요.")
                }

                if (roi == null) {
                    appendLine()
                    appendLine("[병변 ROI 선택 시작] 버튼을 눌러 병변 주변을 드래그해주세요.")
                }
            }.trim()
        )
    }

    private fun setStatus(message: String) {
        imageGuideTextView.text = message
    }

    private fun makeCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBg(cardColor, 18)
            elevation = dp(3).toFloat()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }
    }

    private fun roundedBg(color: Int, radiusDp: Int): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
    private fun saveLatestDiameterForAbcd(diameterMm: Float) {
        getSharedPreferences("abcd_measurement_cache", MODE_PRIVATE)
            .edit()
            .putFloat("latest_diameter_mm", diameterMm)
            .putLong("latest_diameter_saved_at", System.currentTimeMillis())
            .apply()
    }
    private fun calculateLesionSizeFromRoi(
        bitmap: Bitmap,
        roi: RectF,
        coin: CoinDetectionResult
    ): LesionSizeResult {
        val lesionPoints = extractLesionPointsFromRoi(
            bitmap = bitmap,
            roi = roi,
            percentile = 25f
        )

        if (lesionPoints.isEmpty()) {
            throw IllegalStateException("ROI 내부에서 병변 후보 픽셀을 찾지 못했습니다.")
        }

        val lesionAreaPx = lesionPoints.size
        val eqDiameterPx = sqrt(4.0 * lesionAreaPx.toDouble() / PI)

        val axisResult = calculateLongestAxis(lesionPoints)
        val longestAxisPx = axisResult.lengthPx

        val coinRealMm = 24.0
        val coinPx = coin.coinPx.toDouble()

        val eqDiameterMm = eqDiameterPx / coinPx * coinRealMm
        val longestAxisMm = longestAxisPx / coinPx * coinRealMm

        return LesionSizeResult(
            lesionAreaPx = lesionAreaPx,
            eqDiameterPx = eqDiameterPx,
            longestAxisPx = longestAxisPx,
            eqDiameterMm = eqDiameterMm,
            longestAxisMm = longestAxisMm,
            lesionPoints = lesionPoints,
            axisX1 = axisResult.x1,
            axisY1 = axisResult.y1,
            axisX2 = axisResult.x2,
            axisY2 = axisResult.y2
        )
    }

    private fun calculateAbcFeatures(
        bitmap: Bitmap,
        lesionPoints: List<Pair<Int, Int>>
    ): AbcFeatureResult {
        val aScore = calculateAsymmetryScore(lesionPoints)
        val bScore = calculateBorderIrregularityScore(lesionPoints)
        val cScore = calculateColorDiversityScore(bitmap, lesionPoints)

        return AbcFeatureResult(
            aScore = aScore,
            aLevel = levelFromScore(aScore, aLowThreshold, aHighThreshold),
            bScore = bScore,
            bLevel = levelFromScore(bScore, bLowThreshold, bHighThreshold),
            cScore = cScore,
            cLevel = levelFromScore(cScore, cLowThreshold, cHighThreshold)
        )
    }

    private fun calculateAsymmetryScore(points: List<Pair<Int, Int>>): Double {
        if (points.size < 10) return 0.0

        val minX = points.minOf { it.first }
        val maxX = points.maxOf { it.first }
        val minY = points.minOf { it.second }
        val maxY = points.maxOf { it.second }

        val w = maxX - minX + 1
        val h = maxY - minY + 1

        if (w <= 2 || h <= 2) return 0.0

        val mask = Array(h) { BooleanArray(w) }

        for (p in points) {
            val x = p.first - minX
            val y = p.second - minY

            if (x in 0 until w && y in 0 until h) {
                mask[y][x] = true
            }
        }

        val horizontalScore = flippedIouDifference(mask, flipHorizontal = true)
        val verticalScore = flippedIouDifference(mask, flipHorizontal = false)

        return maxOf(horizontalScore, verticalScore).coerceIn(0.0, 1.0)
    }

    private fun flippedIouDifference(
        mask: Array<BooleanArray>,
        flipHorizontal: Boolean
    ): Double {
        val h = mask.size
        val w = mask[0].size

        var intersection = 0
        var union = 0

        for (y in 0 until h) {
            for (x in 0 until w) {
                val original = mask[y][x]

                val fx: Int
                val fy: Int

                if (flipHorizontal) {
                    fx = w - 1 - x
                    fy = y
                } else {
                    fx = x
                    fy = h - 1 - y
                }

                val flipped = mask[fy][fx]

                if (original && flipped) intersection++
                if (original || flipped) union++
            }
        }

        if (union == 0) return 0.0

        val iou = intersection.toDouble() / union.toDouble()
        return 1.0 - iou
    }

    private fun calculateBorderIrregularityScore(points: List<Pair<Int, Int>>): Double {
        if (points.size < 10) return 0.0

        val minX = points.minOf { it.first }
        val maxX = points.maxOf { it.first }
        val minY = points.minOf { it.second }
        val maxY = points.maxOf { it.second }

        val w = maxX - minX + 1
        val h = maxY - minY + 1

        if (w <= 2 || h <= 2) return 0.0

        val mask = Array(h) { BooleanArray(w) }

        for (p in points) {
            val x = p.first - minX
            val y = p.second - minY

            if (x in 0 until w && y in 0 until h) {
                mask[y][x] = true
            }
        }

        val area = points.size.toDouble()
        var perimeter = 0.0

        val dirs = arrayOf(
            Pair(1, 0),
            Pair(-1, 0),
            Pair(0, 1),
            Pair(0, -1)
        )

        for (y in 0 until h) {
            for (x in 0 until w) {
                if (!mask[y][x]) continue

                for (d in dirs) {
                    val nx = x + d.first
                    val ny = y + d.second

                    if (nx < 0 || nx >= w || ny < 0 || ny >= h || !mask[ny][nx]) {
                        perimeter += 1.0
                    }
                }
            }
        }

        if (perimeter <= 0.0) return 0.0

        val circularity = (4.0 * PI * area) / (perimeter * perimeter)
        val bScore = 1.0 - circularity.coerceIn(0.0, 1.0)

        return bScore.coerceIn(0.0, 1.0)
    }

    private fun calculateColorDiversityScore(
        bitmap: Bitmap,
        points: List<Pair<Int, Int>>
    ): Double {
        if (points.size < 10) return 0.0

        val hsv = FloatArray(3)

        var sinSum = 0.0
        var cosSum = 0.0

        val sValues = mutableListOf<Double>()
        val vValues = mutableListOf<Double>()

        val sampleLimit = 5000
        val step = if (points.size > sampleLimit) {
            points.size / sampleLimit
        } else {
            1
        }

        var index = 0
        while (index < points.size) {
            val p = points[index]
            val x = p.first.coerceIn(0, bitmap.width - 1)
            val y = p.second.coerceIn(0, bitmap.height - 1)

            val pixel = bitmap.getPixel(x, y)

            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            Color.RGBToHSV(r, g, b, hsv)

            val hueDegree = hsv[0].toDouble()
            val hueRad = Math.toRadians(hueDegree)

            sinSum += sin(hueRad)
            cosSum += cos(hueRad)

            sValues.add(hsv[1].toDouble())
            vValues.add(hsv[2].toDouble())

            index += step
        }

        val n = sValues.size
        if (n == 0) return 0.0

        val meanSin = sinSum / n.toDouble()
        val meanCos = cosSum / n.toDouble()

        val hueConsistency = sqrt(meanSin * meanSin + meanCos * meanCos)
        val hueDiversity = 1.0 - hueConsistency.coerceIn(0.0, 1.0)

        val sStd = standardDeviation(sValues)
        val vStd = standardDeviation(vValues)

        val sNorm = (sStd / 0.5).coerceIn(0.0, 1.0)
        val vNorm = (vStd / 0.5).coerceIn(0.0, 1.0)

        val cScore = 0.4 * hueDiversity + 0.3 * sNorm + 0.3 * vNorm

        return cScore.coerceIn(0.0, 1.0)
    }

    private fun standardDeviation(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0

        val mean = values.average()
        val variance = values.map {
            val diff = it - mean
            diff * diff
        }.average()

        return sqrt(variance)
    }

    private fun levelFromScore(
        score: Double,
        lowThreshold: Double,
        highThreshold: Double
    ): String {
        return when {
            score < lowThreshold -> "낮음"
            score < highThreshold -> "보통"
            else -> "높음"
        }
    }

    private fun extractLesionPointsFromRoi(
        bitmap: Bitmap,
        roi: RectF,
        percentile: Float
    ): List<Pair<Int, Int>> {
        val x1 = roi.left.toInt().coerceIn(0, bitmap.width - 1)
        val y1 = roi.top.toInt().coerceIn(0, bitmap.height - 1)
        val x2 = roi.right.toInt().coerceIn(0, bitmap.width)
        val y2 = roi.bottom.toInt().coerceIn(0, bitmap.height)

        val w = x2 - x1
        val h = y2 - y1

        if (w <= 2 || h <= 2) {
            throw IllegalArgumentException("ROI가 너무 작습니다.")
        }

        val grayValues = IntArray(w * h)
        var idx = 0

        for (yy in y1 until y2) {
            for (xx in x1 until x2) {
                val pixel = bitmap.getPixel(xx, yy)

                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                val gray = (0.299f * r + 0.587f * g + 0.114f * b).toInt()
                grayValues[idx++] = gray
            }
        }

        val threshold = percentileValue(grayValues, percentile)

        val binary = Array(h) { BooleanArray(w) }

        idx = 0
        for (row in 0 until h) {
            for (col in 0 until w) {
                binary[row][col] = grayValues[idx++] < threshold
            }
        }

        val largestComponent = keepLargestComponent(binary)

        val lesionPoints = mutableListOf<Pair<Int, Int>>()

        for (row in 0 until h) {
            for (col in 0 until w) {
                if (largestComponent[row][col]) {
                    lesionPoints.add(Pair(x1 + col, y1 + row))
                }
            }
        }

        return lesionPoints
    }

    private fun percentileValue(values: IntArray, percentile: Float): Int {
        if (values.isEmpty()) return 0

        val sorted = values.copyOf()
        sorted.sort()

        val p = percentile.coerceIn(0f, 100f)
        val index = ((p / 100f) * (sorted.size - 1)).toInt()

        return sorted[index]
    }

    private fun keepLargestComponent(binary: Array<BooleanArray>): Array<BooleanArray> {
        val h = binary.size
        val w = binary[0].size

        val visited = Array(h) { BooleanArray(w) }
        val largest = mutableListOf<Pair<Int, Int>>()

        val dirs = arrayOf(
            Pair(1, 0),
            Pair(-1, 0),
            Pair(0, 1),
            Pair(0, -1)
        )

        for (row in 0 until h) {
            for (col in 0 until w) {
                if (!binary[row][col] || visited[row][col]) continue

                val component = mutableListOf<Pair<Int, Int>>()
                val queue: ArrayDeque<Pair<Int, Int>> = ArrayDeque()

                visited[row][col] = true
                queue.add(Pair(row, col))

                while (!queue.isEmpty()) {
                    val current = queue.removeFirst()
                    component.add(current)

                    for (d in dirs) {
                        val nr = current.first + d.first
                        val nc = current.second + d.second

                        if (nr < 0 || nr >= h || nc < 0 || nc >= w) continue
                        if (visited[nr][nc]) continue
                        if (!binary[nr][nc]) continue

                        visited[nr][nc] = true
                        queue.add(Pair(nr, nc))
                    }
                }

                if (component.size > largest.size) {
                    largest.clear()
                    largest.addAll(component)
                }
            }
        }

        val result = Array(h) { BooleanArray(w) }

        for (p in largest) {
            result[p.first][p.second] = true
        }

        return result
    }

    private fun calculateLongestAxis(points: List<Pair<Int, Int>>): AxisResult {
        if (points.size < 2) {
            val p = points.firstOrNull() ?: Pair(0, 0)
            return AxisResult(
                lengthPx = 0.0,
                x1 = p.first.toFloat(),
                y1 = p.second.toFloat(),
                x2 = p.first.toFloat(),
                y2 = p.second.toFloat()
            )
        }

        val sampled = if (points.size > 1000) {
            val step = points.size.toDouble() / 1000.0
            List(1000) { i ->
                points[(i * step).toInt().coerceIn(0, points.size - 1)]
            }
        } else {
            points
        }

        var maxDistSq = 0.0
        var bestP1 = sampled[0]
        var bestP2 = sampled[1]

        for (i in sampled.indices) {
            val p1 = sampled[i]

            for (j in i + 1 until sampled.size) {
                val p2 = sampled[j]

                val dx = (p1.first - p2.first).toDouble()
                val dy = (p1.second - p2.second).toDouble()
                val distSq = dx * dx + dy * dy

                if (distSq > maxDistSq) {
                    maxDistSq = distSq
                    bestP1 = p1
                    bestP2 = p2
                }
            }
        }

        return AxisResult(
            lengthPx = sqrt(maxDistSq),
            x1 = bestP1.first.toFloat(),
            y1 = bestP1.second.toFloat(),
            x2 = bestP2.first.toFloat(),
            y2 = bestP2.second.toFloat()
        )
    }

    private fun detectCoin(bitmap: Bitmap): CoinDetectionResult? {
        val inputBuffer = bitmapToYoloInputBuffer(bitmap)

        val output = Array(1) { Array(300) { FloatArray(6) } }

        coinInterpreter.run(inputBuffer, output)

        var bestConf = -1f
        var bestBox: FloatArray? = null

        for (i in 0 until 300) {
            val det = output[0][i]

            val x1Norm = det[0]
            val y1Norm = det[1]
            val x2Norm = det[2]
            val y2Norm = det[3]
            val conf = det[4]
            val cls = det[5]

            if (conf > bestConf && conf >= coinConfThreshold && cls == 0f) {
                bestConf = conf
                bestBox = floatArrayOf(x1Norm, y1Norm, x2Norm, y2Norm, conf, cls)
            }
        }

        val box = bestBox ?: return null

        val imgW = bitmap.width.toFloat()
        val imgH = bitmap.height.toFloat()

        val x1 = (box[0] * imgW).coerceIn(0f, imgW)
        val y1 = (box[1] * imgH).coerceIn(0f, imgH)
        val x2 = (box[2] * imgW).coerceIn(0f, imgW)
        val y2 = (box[3] * imgH).coerceIn(0f, imgH)

        val w = x2 - x1
        val h = y2 - y1

        if (w <= 0f || h <= 0f) {
            return null
        }

        val coinPx = (w + h) / 2f

        return CoinDetectionResult(
            x1 = x1,
            y1 = y1,
            x2 = x2,
            y2 = y2,
            width = w,
            height = h,
            coinPx = coinPx,
            confidence = box[4],
            classId = box[5]
        )
    }

    private fun bitmapToYoloInputBuffer(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(
            bitmap.copy(Bitmap.Config.ARGB_8888, true),
            coinInputSize,
            coinInputSize,
            true
        )

        val byteBuffer = ByteBuffer.allocateDirect(1 * coinInputSize * coinInputSize * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(coinInputSize * coinInputSize)
        resized.getPixels(pixels, 0, coinInputSize, 0, 0, coinInputSize, coinInputSize)

        var pixelIndex = 0

        for (y in 0 until coinInputSize) {
            for (x in 0 until coinInputSize) {
                val pixel = pixels[pixelIndex++]

                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f

                byteBuffer.putFloat(r)
                byteBuffer.putFloat(g)
                byteBuffer.putFloat(b)
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    private fun uriToBitmap(uri: Uri): Bitmap {
        contentResolver.openInputStream(uri).use { inputStream ->
            if (inputStream == null) {
                throw IllegalArgumentException("이미지를 열 수 없습니다.")
            }

            return BitmapFactory.decodeStream(inputStream)
                ?: throw IllegalArgumentException("Bitmap 변환 실패")
        }
    }

    private fun saveMeasurementImageToInternalStorage(bitmap: Bitmap): String? {
        return try {
            val dir = File(filesDir, "measurement_images")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val fileName = "measurement_${System.currentTimeMillis()}.jpg"
            val file = File(dir, fileName)

            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null

        if (uri.scheme == "content") {
            val cursor = contentResolver.query(uri, null, null, null, null)

            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        result = it.getString(nameIndex)
                    }
                }
            }
        }

        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/')
            if (cut != null && cut != -1) {
                result = result?.substring(cut + 1)
            }
        }

        return result
    }

    private fun loadModelFile(assetName: String): ByteBuffer {
        val fileDescriptor = assets.openFd(assetName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel

        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength

        return fileChannel.map(
            FileChannel.MapMode.READ_ONLY,
            startOffset,
            declaredLength
        )
    }

    private fun showSimpleError(message: String) {
        val textView = TextView(this).apply {
            textSize = 16f
            setPadding(32, 32, 32, 32)
            text = message
        }

        setContentView(textView)
    }

    data class CoinDetectionResult(
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float,
        val width: Float,
        val height: Float,
        val coinPx: Float,
        val confidence: Float,
        val classId: Float
    )

    data class LesionSizeResult(
        val lesionAreaPx: Int,
        val eqDiameterPx: Double,
        val longestAxisPx: Double,
        val eqDiameterMm: Double,
        val longestAxisMm: Double,
        val lesionPoints: List<Pair<Int, Int>>,
        val axisX1: Float,
        val axisY1: Float,
        val axisX2: Float,
        val axisY2: Float
    )

    data class AxisResult(
        val lengthPx: Double,
        val x1: Float,
        val y1: Float,
        val x2: Float,
        val y2: Float
    )

    data class AbcFeatureResult(
        val aScore: Double,
        val aLevel: String,
        val bScore: Double,
        val bLevel: String,
        val cScore: Double,
        val cLevel: String
    )

    override fun onDestroy() {
        super.onDestroy()

        if (::coinInterpreter.isInitialized) {
            coinInterpreter.close()
        }
    }
}

class RoiOverlayImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatImageView(context, attrs) {

    var onRoiChanged: ((RectF) -> Unit)? = null
    var onRoiFinished: (() -> Unit)? = null

    var roiEditEnabled: Boolean = false

    private var coinBoxBitmap: RectF? = null
    private var roiBitmap: RectF? = null

    private var lesionPointsBitmap: List<Pair<Int, Int>> = emptyList()
    private var axisLineBitmap: FloatArray? = null
    private var longestAxisMmText: Double? = null

    private var startBitmapX = 0f
    private var startBitmapY = 0f

    private val inverseMatrix = Matrix()

    private val coinPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val roiPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val roiFillPaint = Paint().apply {
        color = Color.argb(60, 0, 255, 0)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val lesionMaskPaint = Paint().apply {
        color = Color.argb(120, 0, 120, 255)
        style = Paint.Style.FILL
        strokeWidth = 4f
        isAntiAlias = false
    }

    private val axisPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 7f
        isAntiAlias = true
    }

    private val axisTextPaint = Paint().apply {
        color = Color.YELLOW
        textSize = 42f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val roiTextPaint = Paint().apply {
        color = Color.GREEN
        textSize = 40f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    init {
        isClickable = true
        isFocusable = true
    }

    fun setCoinBox(box: RectF?) {
        coinBoxBitmap = box
        invalidate()
    }

    fun clearRoi() {
        roiBitmap = null
        roiEditEnabled = false
        invalidate()
    }

    fun setLesionMaskOverlay(
        points: List<Pair<Int, Int>>,
        axisX1: Float,
        axisY1: Float,
        axisX2: Float,
        axisY2: Float,
        eqDiameterMm: Double,
        longestAxisMm: Double
    ) {
        lesionPointsBitmap = points
        axisLineBitmap = floatArrayOf(axisX1, axisY1, axisX2, axisY2)
        longestAxisMmText = longestAxisMm
        invalidate()
    }

    fun clearLesionMaskOverlay() {
        lesionPointsBitmap = emptyList()
        axisLineBitmap = null
        longestAxisMmText = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        coinBoxBitmap?.let { box ->
            val viewBox = mapBitmapRectToViewRect(box)
            canvas.drawRect(viewBox, coinPaint)
        }

        roiBitmap?.let { roi ->
            val viewRoi = mapBitmapRectToViewRect(roi)

            canvas.drawRect(viewRoi, roiFillPaint)
            canvas.drawRect(viewRoi, roiPaint)

            val textY = if (viewRoi.top - 12f > 40f) {
                viewRoi.top - 12f
            } else {
                viewRoi.bottom + 48f
            }

            canvas.drawText("lesion ROI", viewRoi.left, textY, roiTextPaint)
        }

        drawLesionMask(canvas)
        drawAxisAndSizeText(canvas)
    }

    private fun drawLesionMask(canvas: Canvas) {
        if (lesionPointsBitmap.isEmpty()) return

        val maxDrawPoints = 6000
        val step = if (lesionPointsBitmap.size > maxDrawPoints) {
            lesionPointsBitmap.size / maxDrawPoints
        } else {
            1
        }

        var i = 0
        while (i < lesionPointsBitmap.size) {
            val p = lesionPointsBitmap[i]
            val mapped = mapBitmapPointToViewPoint(p.first.toFloat(), p.second.toFloat())

            canvas.drawCircle(mapped[0], mapped[1], 3.0f, lesionMaskPaint)

            i += step
        }
    }

    private fun drawAxisAndSizeText(canvas: Canvas) {
        val axis = axisLineBitmap ?: return

        val p1 = mapBitmapPointToViewPoint(axis[0], axis[1])
        val p2 = mapBitmapPointToViewPoint(axis[2], axis[3])

        canvas.drawLine(p1[0], p1[1], p2[0], p2[1], axisPaint)

        val textX = minOf(p1[0], p2[0])
        val textY = maxOf(p1[1], p2[1]) + 48f

        val longest = longestAxisMmText

        if (longest != null) {
            canvas.drawText("최장축: ${"%.2f".format(longest)} mm", textX, textY, axisTextPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!roiEditEnabled) {
            return false
        }

        val drawable = drawable ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                parent?.requestDisallowInterceptTouchEvent(true)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        imageMatrix.invert(inverseMatrix)

        val touchPoint = floatArrayOf(event.x, event.y)
        inverseMatrix.mapPoints(touchPoint)

        val bitmapX = touchPoint[0].coerceIn(0f, drawable.intrinsicWidth.toFloat())
        val bitmapY = touchPoint[1].coerceIn(0f, drawable.intrinsicHeight.toFloat())

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startBitmapX = bitmapX
                startBitmapY = bitmapY

                roiBitmap = RectF(
                    startBitmapX,
                    startBitmapY,
                    bitmapX,
                    bitmapY
                )

                invalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                updateRoi(bitmapX, bitmapY)

                roiBitmap?.let {
                    onRoiChanged?.invoke(it)
                }

                invalidate()
                return true
            }

            MotionEvent.ACTION_UP -> {
                updateRoi(bitmapX, bitmapY)

                roiBitmap?.let {
                    onRoiChanged?.invoke(it)
                }

                roiEditEnabled = false
                parent?.requestDisallowInterceptTouchEvent(false)
                onRoiFinished?.invoke()

                invalidate()
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                roiEditEnabled = false
                parent?.requestDisallowInterceptTouchEvent(false)
                onRoiFinished?.invoke()
                return true
            }
        }

        return true
    }

    private fun updateRoi(currentBitmapX: Float, currentBitmapY: Float) {
        val left = minOf(startBitmapX, currentBitmapX)
        val top = minOf(startBitmapY, currentBitmapY)
        val right = maxOf(startBitmapX, currentBitmapX)
        val bottom = maxOf(startBitmapY, currentBitmapY)

        roiBitmap = RectF(left, top, right, bottom)
    }

    private fun mapBitmapRectToViewRect(bitmapRect: RectF): RectF {
        val mapped = RectF(bitmapRect)
        imageMatrix.mapRect(mapped)
        return mapped
    }

    private fun mapBitmapPointToViewPoint(x: Float, y: Float): FloatArray {
        val point = floatArrayOf(x, y)
        imageMatrix.mapPoints(point)
        return point
    }
}