package com.example.melanoscan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
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

class AiAnalysisActivity : AppCompatActivity() {

    private lateinit var interpreter: Interpreter

    private lateinit var rootLayout: LinearLayout
    private lateinit var imageView: RoiOverlayImageView
    private lateinit var imageGuideTextView: TextView
    private lateinit var roiSelectButton: Button

    private var currentBitmap: Bitmap? = null
    private var currentImageName: String? = null
    private var currentRoiRect: RectF? = null
    private var cameraImageUri: Uri? = null

    private val classNames = arrayOf(
        "benign_nevus",
        "melanoma",
        "seborrheic_keratosis"
    )

    private val inputSize = 320

    private val mean = floatArrayOf(0.485f, 0.456f, 0.406f)
    private val std = floatArrayOf(0.229f, 0.224f, 0.225f)

    // 앱 내부 보조 기준값. 임상 확정 기준이 아님.
    private val aLowThreshold = 0.30
    private val aHighThreshold = 0.50

    private val bLowThreshold = 0.25
    private val bHighThreshold = 0.45

    private val cLowThreshold = 0.30
    private val cHighThreshold = 0.60

    private val bgColor = Color.rgb(246, 250, 247)
    private val cardColor = Color.WHITE
    private val green = Color.rgb(67, 153, 75)
    private val darkText = Color.rgb(35, 45, 55)
    private val subText = Color.rgb(110, 120, 125)
    private val lightGreen = Color.rgb(237, 248, 241)

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            handleSelectedImage(uri)
        }
    }

    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val uri = cameraImageUri
            if (uri != null) {
                handleSelectedImage(uri)
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
            val modelBuffer = loadModelFile("skin_classifier_efficientnetv2s_320_float32.tflite")

            val options = Interpreter.Options().apply {
                setNumThreads(4)
            }

            interpreter = Interpreter(modelBuffer, options)

        } catch (e: Exception) {
            showError("모델 로드 실패", e)
            return
        }

        setupUi()
    }

    private fun setupUi() {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }

        val titleTextView = TextView(this).apply {
            text = "AI 분석"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            setPadding(0, 0, 0, dp(6))
        }

        val guideTextView = TextView(this).apply {
            text = "병변 사진을 촬영하거나 갤러리에서 선택한 뒤, 병변 주변을 드래그해주세요."
            textSize = 14f
            setTextColor(subText)
            setPadding(0, 0, 0, dp(18))
        }

        val inputCard = makeCard().apply {
            addView(TextView(this@AiAnalysisActivity).apply {
                text = "사진 입력"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            addView(TextView(this@AiAnalysisActivity).apply {
                text = "병변이 선명하게 보이도록 촬영하거나 기존 사진을 선택해주세요."
                textSize = 13f
                setTextColor(subText)
                setPadding(0, dp(6), 0, dp(14))
            })

            val buttonRow = LinearLayout(this@AiAnalysisActivity).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val cameraButton = Button(this@AiAnalysisActivity).apply {
                text = "카메라로 촬영"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = roundedBg(green, 14)
                setOnClickListener {
                    requestCameraOrOpen()
                }
            }

            val galleryButton = Button(this@AiAnalysisActivity).apply {
                text = "갤러리에서 선택"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(green)
                background = roundedBg(lightGreen, 14)
                setOnClickListener {
                    galleryLauncher.launch("image/*")
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
            addView(TextView(this@AiAnalysisActivity).apply {
                text = "분석 이미지"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            imageGuideTextView = TextView(this@AiAnalysisActivity).apply {
                text = "사진을 선택하면 여기에 표시됩니다."
                textSize = 13f
                setTextColor(subText)
                setPadding(0, dp(6), 0, dp(12))
            }

            addView(imageGuideTextView)

            imageView = RoiOverlayImageView(this@AiAnalysisActivity).apply {
                adjustViewBounds = true
                scaleType = ImageView.ScaleType.FIT_CENTER
                minimumHeight = dp(240)
                background = roundedBg(Color.rgb(235, 238, 236), 16)

                onRoiChanged = { roi ->
                    currentRoiRect = roi
                    setStatus("병변 ROI 선택 중.. 병변이 박스 안에 포함되도록 조정해주세요.")
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
            addView(TextView(this@AiAnalysisActivity).apply {
                text = "ROI 선택"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            addView(TextView(this@AiAnalysisActivity).apply {
                text = "병변이 완전히 포함되도록 주변 피부를 조금 포함해 드래그해주세요."
                textSize = 13f
                setTextColor(subText)
                setPadding(0, dp(6), 0, dp(14))
            })

            roiSelectButton = Button(this@AiAnalysisActivity).apply {
                text = "병변 ROI 선택 시작"
                isEnabled = false
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = roundedBg(Color.rgb(170, 180, 175), 14)

                setOnClickListener {
                    if (currentBitmap == null) {
                        setStatus("먼저 병변 사진을 촬영하거나 선택해주세요.")
                        return@setOnClickListener
                    }

                    imageView.clearRoi()
                    imageView.clearLesionMaskOverlay()
                    currentRoiRect = null

                    imageView.roiEditEnabled = true
                    text = "이미지 위에서 병변 주변을 드래그하세요"

                    setStatus("이미지 위에서 병변 주변을 드래그하세요.")
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

        rootLayout.addView(titleTextView)
        rootLayout.addView(guideTextView)
        rootLayout.addView(inputCard)
        rootLayout.addView(imageCard)
        rootLayout.addView(roiCard)

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

            val file = File(dir, "analysis_camera_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )

            cameraImageUri = uri
            cameraLauncher.launch(uri)

        } catch (e: Exception) {
            setStatus("카메라 실행 실패\n${e.message}")
        }
    }

    private fun handleSelectedImage(uri: Uri) {
        try {
            val bitmap = uriToBitmap(uri)
            val imageName = getFileName(uri) ?: "selected image"

            currentBitmap = bitmap
            currentImageName = imageName
            currentRoiRect = null

            imageView.clearRoi()
            imageView.clearLesionMaskOverlay()
            imageView.setCoinBox(null)
            imageView.setImageBitmap(bitmap)

            roiSelectButton.isEnabled = true
            roiSelectButton.text = "병변 ROI 선택 시작"
            roiSelectButton.setTextColor(Color.WHITE)
            roiSelectButton.background = roundedBg(green, 14)

            setStatus(
                """
                이미지 선택 완료

                [병변 ROI 선택 시작]을 누른 뒤 병변 주변을 드래그하세요.
                ROI를 확정하면 AI 분석 결과 화면으로 이동합니다.
                """.trimIndent()
            )

        } catch (e: Exception) {
            showError("이미지 로드 실패", e)
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
            .setMessage("선택한 영역을 AI 분석 영역으로 사용할까요?")
            .setPositiveButton("분석하기") { _, _ ->
                confirmRoiAndRunAnalysis()
            }
            .setNegativeButton("다시 선택") { _, _ ->
                restartRoiSelection()
            }
            .show()
    }

    private fun restartRoiSelection() {
        imageView.clearRoi()
        imageView.clearLesionMaskOverlay()
        currentRoiRect = null

        imageView.roiEditEnabled = true
        roiSelectButton.text = "이미지 위에서 병변 주변을 드래그하세요"

        setStatus("ROI를 다시 선택합니다. 병변 주변을 드래그해주세요.")
    }

    private fun confirmRoiAndRunAnalysis() {
        val bitmap = currentBitmap
        val roi = currentRoiRect

        if (bitmap == null || roi == null) {
            setStatus("이미지 또는 ROI 정보가 없습니다. 다시 선택해주세요.")
            return
        }

        imageView.roiEditEnabled = false
        roiSelectButton.text = "병변 ROI 다시 선택"

        try {
            setStatus("AI 분석 중입니다. 잠시만 기다려주세요.")

            val roiBitmap = cropBitmapByRoi(bitmap, roi)

            val lesionPoints = extractLesionPointsFromRoi(
                bitmap = bitmap,
                roi = roi,
                percentile = 25f
            )

            if (lesionPoints.isEmpty()) {
                setStatus("ROI 내부에서 병변 후보 영역을 찾지 못했습니다. ROI를 다시 선택해주세요.")
                return
            }

            val result = classify(roiBitmap)
            val abcResult = calculateAbcFeatures(
                bitmap = bitmap,
                lesionPoints = lesionPoints
            )

            openAiResultActivity(
                bitmap = bitmap,
                lesionPoints = lesionPoints,
                roi = roi,
                result = result,
                abcResult = abcResult
            )

        } catch (e: Exception) {
            setStatus("AI 분석 실패\n\n${e.message}\n\nROI를 다시 선택해주세요.")
        }
    }

    private fun openAiResultActivity(
        bitmap: Bitmap,
        lesionPoints: List<Pair<Int, Int>>,
        roi: RectF,
        result: ClassificationResult,
        abcResult: AbcFeatureResult
    ) {
        val imagePath = saveAnalysisImageToInternalStorage(bitmap)
        val maskImagePath = saveMaskOverlayImageToInternalStorage(
            bitmap = bitmap,
            lesionPoints = lesionPoints,
            roi = roi
        )

        val melanomaProb = result.probs[1]

        val aOverlapDiffPercent = (abcResult.aScore * 100.0).toFloat()
        val bBoundaryRatio = calculateBoundaryRatioForDisplay(lesionPoints).toFloat()
        val cColorRegionCount = estimateColorRegionCountForDisplay(abcResult.cScore)
        val dDiameterMm = -1f

        val intent = Intent(this@AiAnalysisActivity, AiResultActivity::class.java).apply {
            putExtra("image_path", imagePath)
            putExtra("mask_image_path", maskImagePath)
            putExtra("pred_class", result.predClass)
            putExtra("melanoma_prob", melanomaProb)
            putExtra("probs", result.probs)
            putExtra("summary_text", buildShortSummary(result, abcResult))
            putExtra("next_step_text", getNextStepText(result, abcResult))
            putExtra("abcd_detail_text", buildAbcdDetailText(result, abcResult))

            putExtra("a_overlap_diff_percent", aOverlapDiffPercent)
            putExtra("b_boundary_ratio", bBoundaryRatio)
            putExtra("c_color_region_count", cColorRegionCount)
            putExtra("d_diameter_mm", dDiameterMm)
        }

        startActivity(intent)
    }

    private fun saveAnalysisImageToInternalStorage(bitmap: Bitmap): String? {
        return try {
            val dir = File(filesDir, "analysis_images")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val fileName = "analysis_${System.currentTimeMillis()}.jpg"
            val file = File(dir, fileName)

            FileOutputStream(file).use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            }

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun saveMaskOverlayImageToInternalStorage(
        bitmap: Bitmap,
        lesionPoints: List<Pair<Int, Int>>,
        roi: RectF
    ): String? {
        return try {
            val overlayBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
            val canvas = Canvas(overlayBitmap)

            val maskPaint = Paint().apply {
                color = Color.argb(120, 0, 180, 90)
                style = Paint.Style.FILL
                isAntiAlias = false
            }

            val roiPaint = Paint().apply {
                color = Color.rgb(0, 200, 90)
                style = Paint.Style.STROKE
                strokeWidth = 6f
                isAntiAlias = true
            }

            val roiTextPaint = Paint().apply {
                color = Color.rgb(0, 180, 90)
                textSize = 42f
                typeface = Typeface.DEFAULT_BOLD
                isAntiAlias = true
            }

            val maxDrawPoints = 12000
            val step = if (lesionPoints.size > maxDrawPoints) {
                lesionPoints.size / maxDrawPoints
            } else {
                1
            }

            var i = 0
            while (i < lesionPoints.size) {
                val p = lesionPoints[i]

                canvas.drawCircle(
                    p.first.toFloat(),
                    p.second.toFloat(),
                    3.5f,
                    maskPaint
                )

                i += step
            }

            canvas.drawRect(roi, roiPaint)

            val textY = if (roi.top - 12f > 40f) {
                roi.top - 12f
            } else {
                roi.bottom + 48f
            }

            canvas.drawText("AI 병변 영역", roi.left, textY, roiTextPaint)

            val dir = File(filesDir, "analysis_images")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val fileName = "analysis_mask_${System.currentTimeMillis()}.jpg"
            val file = File(dir, fileName)

            FileOutputStream(file).use { outputStream ->
                overlayBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }

            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun buildShortSummary(
        result: ClassificationResult,
        abcResult: AbcFeatureResult
    ): String {
        val topIndex = result.probs.indices.maxByOrNull { result.probs[it] } ?: result.predIndex
        val topClassName = classNames[topIndex]

        return when (topClassName) {
            "melanoma" -> {
                "AI 예측에서 흑색종 후보가\n가장 높게 계산되었습니다.\n병변 형태 비교 정보도 함께 확인해보세요."
            }

            "benign_nevus" -> {
                "현재 사진 기준으로 모반\n가능성이 높게 계산되었습니다. 병변의 형태·경계·색상 분포를 참고 정보로 확인할 수 있습니다."
            }

            "seborrheic_keratosis" -> {
                "현재 사진 기준으로 지루성각화증 가능성이 높게 계산되었습니다. 병변 형태 비교 정보도 함께 확인해보세요."
            }

            else -> {
                "AI 분석 결과와 병변 형태 비교 정보를 함께 확인해보세요."
            }
        }
    }

    private fun getNextStepText(
        result: ClassificationResult,
        abcResult: AbcFeatureResult
    ): String {
        val topIndex = result.probs.indices.maxByOrNull { result.probs[it] } ?: result.predIndex
        val topClassName = classNames[topIndex]

        val highCount = listOf(
            abcResult.aLevel,
            abcResult.bLevel,
            abcResult.cLevel
        ).count { it == "높음" }

        return when {
            topClassName == "melanoma" -> {
                "피부과 전문의 상담을 권장합니다. 병변 변화 추적 기능으로 크기 변화도 기록할 수 있습니다."
            }

            highCount >= 2 -> {
                "일부 보조 지표에서 주의 특징이 관찰되었습니다. 변화 추적 또는 진료를 권장합니다."
            }

            else -> {
                "현재 위험도는 높지 않지만, 병변이 커지거나 색이 변하면 변화 추적 또는 진료를 권장합니다."
            }
        }
    }

    private fun buildAbcdDetailText(
        result: ClassificationResult,
        abcResult: AbcFeatureResult
    ): String {
        return buildString {
            appendLine("ABCD 보조 분석")
            appendLine()
            appendLine("A. 비대칭성")
            appendLine(buildAsymmetryText(abcResult))
            appendLine()
            appendLine("B. 경계")
            appendLine(buildBorderText(abcResult))
            appendLine()
            appendLine("C. 색상")
            appendLine(buildColorText(abcResult))
            appendLine()
            appendLine("D. 직경")
            appendLine("현재 1차 사진에는 기준 물체가 없어 실제 직경(mm)은 계산하지 않았습니다.")
            appendLine("크기 변화를 관리하려면 병변 변화 추적에서 100원 동전과 함께 촬영해주세요.")
            appendLine()
            appendLine("종합 설명")
            appendLine(buildOverallAbcText(result, abcResult))
            appendLine()
            appendLine("주의")
            appendLine("본 결과는 의료진의 확정 진단이 아니며, 사진 기반 이미지 분석에 따른 참고용 결과입니다.")
            appendLine("사진 조건, 조명, 초점, ROI 선택 범위, mask 품질에 따라 결과가 달라질 수 있습니다.")
        }
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
    private fun calculateBoundaryRatioForDisplay(points: List<Pair<Int, Int>>): Double {
        if (points.size < 10) return -1.0

        val minX = points.minOf { it.first }
        val maxX = points.maxOf { it.first }
        val minY = points.minOf { it.second }
        val maxY = points.maxOf { it.second }

        val w = maxX - minX + 1
        val h = maxY - minY + 1

        if (w <= 2 || h <= 2) return -1.0

        val mask = Array(h) { BooleanArray(w) }

        for (p in points) {
            val x = p.first - minX
            val y = p.second - minY

            if (x in 0 until w && y in 0 until h) {
                mask[y][x] = true
            }
        }

        var area = 0.0
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

                area += 1.0

                for (d in dirs) {
                    val nx = x + d.first
                    val ny = y + d.second

                    if (nx < 0 || nx >= w || ny < 0 || ny >= h || !mask[ny][nx]) {
                        perimeter += 1.0
                    }
                }
            }
        }

        if (area <= 0.0 || perimeter <= 0.0) return -1.0

        val sameAreaCirclePerimeter = 2.0 * sqrt(PI * area)

        if (sameAreaCirclePerimeter <= 0.0) return -1.0

        return (perimeter / sameAreaCirclePerimeter).coerceAtLeast(1.0)
    }

    private fun estimateColorRegionCountForDisplay(cScore: Double): Int {
        if (cScore.isNaN()) return -1

        val normalized = cScore.coerceIn(0.0, 1.0)

        return when {
            normalized < 0.18 -> 1
            normalized < 0.35 -> 2
            normalized < 0.52 -> 3
            normalized < 0.70 -> 4
            normalized < 0.86 -> 5
            else -> 6
        }
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

    private fun buildAsymmetryText(result: AbcFeatureResult): String {
        return when (result.aLevel) {
            "높음" -> "병변의 좌우 또는 상하 형태 차이가 비교적 크게 나타났습니다. 비대칭성이 두드러지는 병변은 추가 관찰이 필요할 수 있습니다."
            "보통" -> "병변 형태에서 일부 비대칭 경향이 관찰됩니다. 사진 조건과 ROI 선택 범위에 따라 달라질 수 있습니다."
            else -> "현재 선택한 이미지 기준으로 뚜렷한 비대칭 특징은 높게 나타나지 않았습니다."
        }
    }

    private fun buildBorderText(result: AbcFeatureResult): String {
        return when (result.bLevel) {
            "높음" -> "병변의 경계가 비교적 불규칙하게 나타났습니다. 경계가 고르지 않은 병변은 변화 여부를 함께 관찰하는 것이 좋습니다."
            "보통" -> "병변 경계에서 약간의 불규칙성이 관찰됩니다. mask 품질이나 조명 조건에 따라 결과가 달라질 수 있습니다."
            else -> "현재 선택한 이미지 기준으로 경계 불규칙성이 높게 나타나지는 않았습니다."
        }
    }

    private fun buildColorText(result: AbcFeatureResult): String {
        return when (result.cLevel) {
            "높음" -> "병변 내부의 색상 차이가 비교적 크게 나타났습니다. 어두운 영역과 밝은 영역이 함께 관찰될 수 있어 주의가 필요합니다."
            "보통" -> "병변 내부 색상에서 일부 차이가 관찰됩니다. 조명, 그림자, 카메라 화이트밸런스의 영향을 받을 수 있습니다."
            else -> "현재 선택한 이미지 기준으로 색상 다양성이 높게 나타나지는 않았습니다."
        }
    }

    private fun buildOverallAbcText(
        classificationResult: ClassificationResult,
        abcResult: AbcFeatureResult
    ): String {
        val topIndex = classificationResult.probs.indices.maxByOrNull {
            classificationResult.probs[it]
        } ?: classificationResult.predIndex

        val topClassName = classNames[topIndex]

        val highCount = listOf(
            abcResult.aLevel,
            abcResult.bLevel,
            abcResult.cLevel
        ).count { it == "높음" }

        return when {
            topClassName == "melanoma" && highCount >= 2 -> {
                "AI 예측에서 흑색종 가능성이 높게 나타났고, ABC 보조 지표에서도 주의 특징이 함께 관찰되었습니다. 피부과 전문의 상담을 권장합니다."
            }

            topClassName == "melanoma" -> {
                "AI 예측에서 흑색종 가능성이 높게 나타났습니다. ABC 보조 지표와 관계없이 피부과 전문의 상담을 권장합니다."
            }

            highCount >= 2 -> {
                "AI 예측 결과와 별도로, 선택된 병변 mask 기반 보조 지표에서 주의 특징이 일부 관찰되었습니다. 크기나 색 변화가 있다면 진료 또는 추적 관찰을 권장합니다."
            }

            highCount == 1 -> {
                "일부 이미지 보조 지표에서 주의 특징이 관찰되었습니다. 다만 사진 조건이나 mask 품질에 따라 결과가 달라질 수 있습니다."
            }

            else -> {
                "현재 사진 기준으로 비대칭성, 경계 불규칙성, 색상 다양성 지표가 높게 나타나지 않았습니다."
            }
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

    private fun cropBitmapByRoi(bitmap: Bitmap, roi: RectF): Bitmap {
        val x1 = roi.left.toInt().coerceIn(0, bitmap.width - 1)
        val y1 = roi.top.toInt().coerceIn(0, bitmap.height - 1)
        val x2 = roi.right.toInt().coerceIn(0, bitmap.width)
        val y2 = roi.bottom.toInt().coerceIn(0, bitmap.height)

        val w = x2 - x1
        val h = y2 - y1

        if (w <= 2 || h <= 2) {
            throw IllegalArgumentException("ROI가 너무 작습니다.")
        }

        return Bitmap.createBitmap(bitmap, x1, y1, w, h)
    }

    private fun classify(bitmap: Bitmap): ClassificationResult {
        val inputBuffer = bitmapToInputBuffer(bitmap)
        val output = Array(1) { FloatArray(3) }

        interpreter.run(inputBuffer, output)

        val logits = output[0]
        val probs = softmax(logits)

        var maxIdx = 0
        for (i in probs.indices) {
            if (probs[i] > probs[maxIdx]) {
                maxIdx = i
            }
        }

        return ClassificationResult(
            predIndex = maxIdx,
            predClass = classNames[maxIdx],
            probs = probs,
            logits = logits
        )
    }

    private fun bitmapToInputBuffer(bitmap: Bitmap): ByteBuffer {
        val resized = Bitmap.createScaledBitmap(
            bitmap.copy(Bitmap.Config.ARGB_8888, true),
            inputSize,
            inputSize,
            true
        )

        val byteBuffer = ByteBuffer.allocateDirect(1 * inputSize * inputSize * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(inputSize * inputSize)
        resized.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        var pixelIndex = 0

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = pixels[pixelIndex++]

                val r = ((pixel shr 16) and 0xFF) / 255.0f
                val g = ((pixel shr 8) and 0xFF) / 255.0f
                val b = (pixel and 0xFF) / 255.0f

                val rn = (r - mean[0]) / std[0]
                val gn = (g - mean[1]) / std[1]
                val bn = (b - mean[2]) / std[2]

                byteBuffer.putFloat(rn)
                byteBuffer.putFloat(gn)
                byteBuffer.putFloat(bn)
            }
        }

        byteBuffer.rewind()
        return byteBuffer
    }

    private fun softmax(logits: FloatArray): FloatArray {
        val maxLogit = logits.maxOrNull() ?: 0f

        val exps = logits.map {
            exp((it - maxLogit).toDouble())
        }

        val sum = exps.sum()

        return exps.map {
            (it / sum).toFloat()
        }.toFloatArray()
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

    private fun showError(title: String, e: Exception) {
        val textView = TextView(this).apply {
            textSize = 16f
            setPadding(32, 32, 32, 32)
            text = "$title\n\n${e.message}\n\n${e.stackTraceToString()}"
        }

        setContentView(textView)
    }

    data class ClassificationResult(
        val predIndex: Int,
        val predClass: String,
        val probs: FloatArray,
        val logits: FloatArray
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

        if (::interpreter.isInitialized) {
            interpreter.close()
        }
    }
}