package com.example.melanoscan

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class AiResultActivity : AppCompatActivity() {

    private val bgColor = Color.rgb(246, 250, 247)
    private val cardColor = Color.WHITE

    private val green = Color.rgb(67, 153, 75)
    private val darkText = Color.rgb(35, 45, 55)
    private val subText = Color.rgb(115, 125, 130)

    private val danger = Color.rgb(235, 80, 80)
    private val warning = Color.rgb(245, 160, 45)
    private val safe = Color.rgb(60, 160, 85)

    private val lightGreen = Color.rgb(237, 248, 241)
    private val lightRed = Color.rgb(255, 240, 240)
    private val lightYellow = Color.rgb(255, 248, 232)

    private val classNames = arrayOf(
        "benign_nevus",
        "melanoma",
        "seborrheic_keratosis"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val imagePath = intent.getStringExtra("image_path")
        val maskImagePath = intent.getStringExtra("mask_image_path")

        val predClass = intent.getStringExtra("pred_class") ?: "unknown"
        val melanomaProb = intent.getFloatExtra("melanoma_prob", -1f)
        val probs = intent.getFloatArrayExtra("probs") ?: floatArrayOf(0f, 0f, 0f)

        val summaryText = intent.getStringExtra("summary_text")
            ?: "AI 분석 요약을 생성하지 못했습니다."

        val nextStepText = intent.getStringExtra("next_step_text")
            ?: "피부 변화가 있으면 진료를 권장합니다."

        val aOverlapDiffPercent = intent.getFloatExtra("a_overlap_diff_percent", -1f)
        val bBoundaryRatio = intent.getFloatExtra("b_boundary_ratio", -1f)
        val cColorRegionCount = intent.getIntExtra("c_color_region_count", -1)
        val dDiameterMm = intent.getFloatExtra("d_diameter_mm", -1f)

        setupUi(
            imagePath = imagePath,
            maskImagePath = maskImagePath,
            predClass = predClass,
            melanomaProb = melanomaProb,
            probs = probs,
            summaryText = summaryText,
            nextStepText = nextStepText,
            aOverlapDiffPercent = aOverlapDiffPercent,
            bBoundaryRatio = bBoundaryRatio,
            cColorRegionCount = cColorRegionCount,
            dDiameterMm = dDiameterMm
        )
    }

    private fun setupUi(
        imagePath: String?,
        maskImagePath: String?,
        predClass: String,
        melanomaProb: Float,
        probs: FloatArray,
        summaryText: String,
        nextStepText: String,
        aOverlapDiffPercent: Float,
        bBoundaryRatio: Float,
        cColorRegionCount: Int,
        dDiameterMm: Float
    ) {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dp(18), dp(24), dp(18), dp(28))
        }

        val topIndex = probs.indices.maxByOrNull { probs[it] } ?: 0
        val topClassName = classNames.getOrElse(topIndex) { predClass }
        val topProb = probs.getOrElse(topIndex) { 0f }

        val resultColor = getPredictionColor(topClassName)
        val resultTitle = getPredictionTitle(topClassName)
        val resultLevel = getPredictionLevel(topClassName, topProb)
        val resultBg = getPredictionBackground(topClassName)

        val topLabel = TextView(this).apply {
            text = "AI 분석 결과 ✨"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            gravity = Gravity.CENTER
        }

        val title = TextView(this).apply {
            text = resultTitle
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(resultColor)
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(4))
        }

        val subtitle = TextView(this).apply {
            text = if (topClassName == "melanoma") {
                "정확한 진단을 위해 피부과 전문의 상담을 권장합니다."
            } else {
                "AI 분석 결과는 참고용이며, 변화가 있으면 진료를 권장합니다."
            }
            textSize = 13f
            setTextColor(subText)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(18))
        }

        val imageAndRiskCard = makeCard().apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val imageBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1.25f
            )
        }

        val lesionImageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.CENTER_CROP
            adjustViewBounds = true
            background = roundedBg(Color.rgb(235, 235, 235), 14)
        }

        lateinit var captureTab: TextView
        lateinit var maskTab: TextView

        fun setCaptureTabSelected() {
            captureTab.setTextColor(green)
            captureTab.background = roundedBg(Color.WHITE, 18)

            maskTab.setTextColor(subText)
            maskTab.background = roundedBg(Color.TRANSPARENT, 18)
        }

        fun setMaskTabSelected() {
            captureTab.setTextColor(subText)
            captureTab.background = roundedBg(Color.TRANSPARENT, 18)

            maskTab.setTextColor(green)
            maskTab.background = roundedBg(Color.WHITE, 18)
        }

        fun showCaptureImage() {
            if (!imagePath.isNullOrBlank()) {
                val file = File(imagePath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    lesionImageView.setImageBitmap(bitmap)
                }
            }
            setCaptureTabSelected()
        }

        fun showMaskImage() {
            if (!maskImagePath.isNullOrBlank()) {
                val file = File(maskImagePath)
                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    lesionImageView.setImageBitmap(bitmap)
                }
            }
            setMaskTabSelected()
        }

        val imageToggleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            background = roundedBg(Color.rgb(241, 245, 243), 22)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }

        captureTab = TextView(this).apply {
            text = "촬영 이미지"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setOnClickListener {
                showCaptureImage()
            }
        }

        maskTab = TextView(this).apply {
            text = "AI 병변 영역"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(dp(8), dp(6), dp(8), dp(6))
            setOnClickListener {
                showMaskImage()
            }
        }

        imageToggleContainer.addView(
            captureTab,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        imageToggleContainer.addView(
            maskTab,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        )

        imageBox.addView(
            imageToggleContainer,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(10)
            }
        )

        imageBox.addView(
            lesionImageView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(190)
            )
        )

        showCaptureImage()

        val resultBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.9f
            )
        }

        val probTitle = TextView(this).apply {
            text = "${classToKorean(topClassName)} 가능성"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
        }

        val probBig = TextView(this).apply {
            text = "${"%.1f".format(topProb * 100f)}%"
            textSize = 38f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(resultColor)
            setPadding(0, dp(8), 0, dp(6))
        }

        val probBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = (topProb * 1000).toInt().coerceIn(0, 1000)
            progressTintList = ColorStateList.valueOf(resultColor)
            progressBackgroundTintList = ColorStateList.valueOf(Color.rgb(235, 235, 235))
        }

        val resultBadge = TextView(this).apply {
            text = resultLevel
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(resultColor)
            gravity = Gravity.CENTER
            background = roundedBg(resultBg, 14)
            setPadding(dp(10), dp(14), dp(10), dp(14))
        }

        val warningSmall = TextView(this).apply {
            text = "ⓘ AI는 의료 전문가를 대체하지 않습니다."
            textSize = 11f
            setTextColor(subText)
            gravity = Gravity.CENTER
            background = roundedBg(Color.rgb(248, 250, 249), 12)
            setPadding(dp(8), dp(10), dp(8), dp(10))
        }

        resultBox.addView(probTitle)
        resultBox.addView(probBig)

        resultBox.addView(
            probBar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(9)
            )
        )

        resultBox.addView(
            resultBadge,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
            }
        )

        resultBox.addView(
            warningSmall,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        )

        imageAndRiskCard.addView(imageBox)
        imageAndRiskCard.addView(resultBox)

        val candidatesCard = makeCard().apply {
            addView(
                makeRowTitle(
                    left = "감별 병변 후보",
                    right = "자세히 보기 >",
                    onRightClick = {
                        showCandidateDetailDialog(probs)
                    }
                )
            )

            val sorted = probs.indices.sortedByDescending { probs[it] }

            sorted.forEachIndexed { rank, classIndex ->
                val className = classNames.getOrElse(classIndex) { "unknown" }
                val prob = probs.getOrElse(classIndex) { 0f }

                addView(
                    makeCandidateRow(
                        rank = rank + 1,
                        className = className,
                        prob = prob
                    )
                )
            }
        }

        val bottomRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val summaryCard = makeSmallInfoCard(
            title = "AI 분석 요약",
            icon = "💡",
            content = summaryText,
            buttonText = "자세한 분석 보기 >",
            onClick = {
                openAbcdShapeAnalysis(
                    aOverlapDiffPercent = aOverlapDiffPercent,
                    bBoundaryRatio = bBoundaryRatio,
                    cColorRegionCount = cColorRegionCount,
                    dDiameterMm = dDiameterMm,
                    predClass = predClass,
                    melanomaProb = melanomaProb,
                    imagePath = imagePath,
                    maskImagePath = maskImagePath
                )
            }
        )

        val nextStepCard = makeSmallInfoCard(
            title = "다음 단계 권장",
            icon = "🏥",
            content = nextStepText,
            buttonText = null,
            onClick = null
        )

        bottomRow.addView(summaryCard)
        bottomRow.addView(nextStepCard)

        val trackingButton = Button(this).apply {
            text = "변화 추적에 등록하기"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = roundedBg(green, 16)
            elevation = dp(3).toFloat()
            setPadding(0, dp(12), 0, dp(12))

            setOnClickListener {
                val intent = Intent(this@AiResultActivity, LesionSelectActivity::class.java).apply {
                    putExtra("pred_class", predClass)
                    putExtra("melanoma_prob", melanomaProb)
                    putExtra("image_path", imagePath)
                    putExtra("mask_image_path", maskImagePath)
                }
                startActivity(intent)
            }
        }

        root.addView(topLabel)
        root.addView(title)
        root.addView(subtitle)
        root.addView(imageAndRiskCard)
        root.addView(candidatesCard)
        root.addView(bottomRow)

        root.addView(
            trackingButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply {
                topMargin = dp(6)
                bottomMargin = dp(12)
            }
        )

        val scrollView = ScrollView(this).apply {
            addView(root)
        }

        setContentView(scrollView)
    }

    private fun openAbcdShapeAnalysis(
        aOverlapDiffPercent: Float,
        bBoundaryRatio: Float,
        cColorRegionCount: Int,
        dDiameterMm: Float,
        predClass: String,
        melanomaProb: Float,
        imagePath: String?,
        maskImagePath: String?
    ) {
        val intent = Intent(this, AbcdShapeAnalysisActivity::class.java).apply {
            putExtra("a_overlap_diff_percent", aOverlapDiffPercent)
            putExtra("b_boundary_ratio", bBoundaryRatio)
            putExtra("c_color_region_count", cColorRegionCount)
            putExtra("d_diameter_mm", dDiameterMm)

            putExtra("pred_class", predClass)
            putExtra("melanoma_prob", melanomaProb)
            putExtra("image_path", imagePath)
            putExtra("mask_image_path", maskImagePath)
        }

        startActivity(intent)
    }

    private fun makeRowTitle(
        left: String,
        right: String?,
        onRightClick: (() -> Unit)?
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(10))

            addView(TextView(this@AiResultActivity).apply {
                text = left
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            })

            if (right != null) {
                addView(TextView(this@AiResultActivity).apply {
                    text = right
                    textSize = 13f
                    setTextColor(green)
                    setOnClickListener {
                        onRightClick?.invoke()
                    }
                })
            }
        }
    }

    private fun makeCandidateRow(
        rank: Int,
        className: String,
        prob: Float
    ): LinearLayout {
        val accentColor = getPredictionColor(className)

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(7), 0, dp(7))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val rankCircle = TextView(this).apply {
            text = rank.toString()
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = roundedBg(
                when (rank) {
                    1 -> accentColor
                    2 -> Color.rgb(95, 130, 185)
                    else -> Color.rgb(150, 160, 170)
                },
                100
            )
        }

        val name = TextView(this).apply {
            text = "${classToKorean(className)}  ${classToEnglish(className)}"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            setPadding(dp(10), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val percent = TextView(this).apply {
            text = "${"%.1f".format(prob * 100f)}%"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
        }

        top.addView(
            rankCircle,
            LinearLayout.LayoutParams(dp(22), dp(22))
        )
        top.addView(name)
        top.addView(percent)

        val bar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = (prob * 1000).toInt().coerceIn(0, 1000)
            progressTintList = ColorStateList.valueOf(
                if (rank == 1) accentColor else Color.rgb(180, 190, 200)
            )
            progressBackgroundTintList = ColorStateList.valueOf(Color.rgb(235, 238, 240))
        }

        row.addView(top)

        row.addView(
            bar,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(7)
            ).apply {
                topMargin = dp(6)
                leftMargin = dp(32)
            }
        )

        return row
    }

    private fun makeSmallInfoCard(
        title: String,
        icon: String,
        content: String,
        buttonText: String?,
        onClick: (() -> Unit)?
    ): LinearLayout {
        return makeCard().apply {
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = dp(6)
                marginStart = dp(6)
            }

            addView(TextView(this@AiResultActivity).apply {
                text = "$icon  $title"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(green)
            })

            addView(TextView(this@AiResultActivity).apply {
                text = content
                textSize = 12f
                setTextColor(darkText)
                setPadding(0, dp(10), 0, dp(8))
                setLineSpacing(2f, 1.0f)
            })

            if (buttonText != null) {
                addView(TextView(this@AiResultActivity).apply {
                    text = buttonText
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(green)
                    setOnClickListener {
                        onClick?.invoke()
                    }
                })
            }
        }
    }

    private fun showCandidateDetailDialog(probs: FloatArray) {
        val dialogRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(8))
            background = roundedBg(Color.rgb(250, 253, 251), 18)
        }

        val titleText = TextView(this).apply {
            text = "감별 병변 후보"
            textSize = 21f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
        }

        val subtitleText = TextView(this).apply {
            text = "AI가 예측한 병변 후보를 확률 순서로 정리했습니다."
            textSize = 13f
            setTextColor(subText)
            setPadding(0, dp(5), 0, dp(14))
        }

        dialogRoot.addView(titleText)
        dialogRoot.addView(subtitleText)

        val sorted = probs.indices.sortedByDescending { probs[it] }

        sorted.forEachIndexed { rank, classIndex ->
            val className = classNames.getOrElse(classIndex) { "unknown" }
            val prob = probs.getOrElse(classIndex) { 0f }

            dialogRoot.addView(
                makeCandidateDetailCard(
                    rank = rank + 1,
                    className = className,
                    prob = prob
                )
            )
        }

        val guideCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(Color.rgb(248, 250, 249), 16)
            setPadding(dp(14), dp(12), dp(14), dp(12))

            addView(TextView(this@AiResultActivity).apply {
                text = "안내"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(subText)
            })

            addView(TextView(this@AiResultActivity).apply {
                text = "확률은 AI 모델이 사진에서 관찰한 패턴을 바탕으로 계산한 참고용 결과입니다. 실제 진단은 피부과 전문의의 진료가 필요합니다."
                textSize = 12f
                setTextColor(subText)
                setPadding(0, dp(6), 0, 0)
                setLineSpacing(2f, 1.0f)
            })
        }

        dialogRoot.addView(
            guideCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(6)
            }
        )

        val scrollView = ScrollView(this).apply {
            addView(dialogRoot)
        }

        AlertDialog.Builder(this)
            .setView(scrollView)
            .setPositiveButton("확인", null)
            .show()
    }

    private fun makeCandidateDetailCard(
        rank: Int,
        className: String,
        prob: Float
    ): LinearLayout {
        val accentColor = getPredictionColor(className)

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(Color.WHITE, 16)
            setPadding(dp(14), dp(13), dp(14), dp(13))
            elevation = dp(1).toFloat()
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val rankCircle = TextView(this).apply {
            text = rank.toString()
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            background = roundedBg(accentColor, 100)
        }

        val nameBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val nameText = TextView(this).apply {
            text = classToKorean(className)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
        }

        val englishText = TextView(this).apply {
            text = classToEnglish(className)
            textSize = 12f
            setTextColor(subText)
            setPadding(0, dp(2), 0, 0)
        }

        nameBox.addView(nameText)
        nameBox.addView(englishText)

        val percentText = TextView(this).apply {
            text = "${"%.1f".format(prob * 100f)}%"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accentColor)
        }

        headerRow.addView(
            rankCircle,
            LinearLayout.LayoutParams(dp(26), dp(26))
        )
        headerRow.addView(nameBox)
        headerRow.addView(percentText)

        val descriptionText = TextView(this).apply {
            text = getCandidateDescription(className, prob)
            textSize = 13f
            setTextColor(subText)
            setPadding(0, dp(10), 0, 0)
            setLineSpacing(3f, 1.0f)
        }

        card.addView(headerRow)
        card.addView(descriptionText)

        card.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            bottomMargin = dp(10)
        }

        return card
    }

    private fun getCandidateDescription(className: String, prob: Float): String {
        val probText = "${"%.1f".format(prob * 100f)}%"

        return when (className) {
            "benign_nevus" -> {
                "일반 점 또는 모반 가능성이 $probText 로 계산되었습니다. 대체로 양성 병변에서 관찰되는 패턴과 유사하다는 의미입니다."
            }

            "melanoma" -> {
                "흑색종 가능성이 $probText 로 계산되었습니다. 이 값이 높게 나타나거나 병변 변화가 있다면 피부과 전문의 상담을 권장합니다."
            }

            "seborrheic_keratosis" -> {
                "지루성각화증 가능성이 $probText 로 계산되었습니다. 표면이 거칠거나 각질성 특징을 보이는 양성 병변에서 자주 고려되는 후보입니다."
            }

            else -> {
                "이 병변 후보의 예측 확률은 $probText 입니다."
            }
        }
    }

    private fun getPredictionTitle(className: String): String {
        return when (className) {
            "melanoma" -> "흑색종 가능성이 높습니다"
            "benign_nevus" -> "모반 가능성이 높게 나타났습니다"
            "seborrheic_keratosis" -> "지루성각화증 가능성이 높게 나타났습니다"
            else -> "병변 분석 결과입니다"
        }
    }

    private fun getPredictionColor(className: String): Int {
        return when (className) {
            "melanoma" -> danger
            "seborrheic_keratosis" -> warning
            "benign_nevus" -> safe
            else -> darkText
        }
    }

    private fun getPredictionBackground(className: String): Int {
        return when (className) {
            "melanoma" -> lightRed
            "seborrheic_keratosis" -> lightYellow
            "benign_nevus" -> lightGreen
            else -> Color.rgb(245, 245, 245)
        }
    }

    private fun getPredictionLevel(className: String, prob: Float): String {
        return when (className) {
            "melanoma" -> {
                when {
                    prob >= 0.70f -> "⚠  위험 단계\n높음"
                    prob >= 0.40f -> "⚠  위험 단계\n주의"
                    else -> "위험 단계\n낮음"
                }
            }

            "benign_nevus" -> {
                "예측 결과\n일반 점 가능성 높음"
            }

            "seborrheic_keratosis" -> {
                "예측 결과\n지루성각화증 가능성 높음"
            }

            else -> {
                "예측 결과\n확인 필요"
            }
        }
    }

    private fun classToKorean(className: String): String {
        return when (className) {
            "benign_nevus" -> "모반"
            "melanoma" -> "흑색종"
            "seborrheic_keratosis" -> "지루성각화증"
            else -> className
        }
    }

    private fun classToEnglish(className: String): String {
        return when (className) {
            "benign_nevus" -> "Nevus"
            "melanoma" -> "Melanoma"
            "seborrheic_keratosis" -> "Seborrheic keratosis"
            else -> ""
        }
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
}