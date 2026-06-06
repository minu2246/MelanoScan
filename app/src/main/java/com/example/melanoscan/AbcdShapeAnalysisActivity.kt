package com.example.melanoscan

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.max
import kotlin.math.min

class AbcdShapeAnalysisActivity : AppCompatActivity() {

    private val bgColor = Color.rgb(240, 249, 243)
    private val cardColor = Color.WHITE

    private val green = Color.rgb(67, 153, 75)
    private val softGreen = Color.rgb(232, 247, 236)

    private val darkText = Color.rgb(34, 44, 58)
    private val subText = Color.rgb(110, 125, 118)
    private val borderColor = Color.rgb(207, 232, 214)

    private val blue = Color.rgb(70, 135, 190)
    private val orange = Color.rgb(245, 160, 45)
    private val purple = Color.rgb(120, 110, 230)
    private val danger = Color.rgb(239, 83, 80)

    private lateinit var rootLayout: LinearLayout

    private var baseAOverlapDiffPercent: Float = -1f
    private var baseBBoundaryRatio: Float = -1f
    private var baseCColorRegionCount: Int = -1
    private var baseDDiameterMm: Float = -1f

    private var latestMeasuredDiameterMm: Float = -1f

    private var predClassForTracking: String = "unknown"
    private var melanomaProbForTracking: Float = -1f
    private var imagePathForTracking: String? = null
    private var maskImagePathForTracking: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        baseAOverlapDiffPercent = intent.getFloatExtra("a_overlap_diff_percent", -1f)
        baseBBoundaryRatio = intent.getFloatExtra("b_boundary_ratio", -1f)
        baseCColorRegionCount = intent.getIntExtra("c_color_region_count", -1)
        baseDDiameterMm = intent.getFloatExtra("d_diameter_mm", -1f)

        predClassForTracking = intent.getStringExtra("pred_class") ?: "unknown"
        melanomaProbForTracking = intent.getFloatExtra("melanoma_prob", -1f)
        imagePathForTracking = intent.getStringExtra("image_path")
        maskImagePathForTracking = intent.getStringExtra("mask_image_path")

        refreshLatestDiameter()
        renderScreen()
    }

    override fun onResume() {
        super.onResume()

        refreshLatestDiameter()

        if (::rootLayout.isInitialized) {
            renderScreen()
        }
    }

    private fun renderScreen() {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dp(20), dp(24), dp(20), dp(28))
            clipToPadding = false
            clipChildren = false
        }

        val displayDiameterMm = if (latestMeasuredDiameterMm > 0f) {
            latestMeasuredDiameterMm
        } else {
            baseDDiameterMm
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dp(18))
        }

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val titleText = TextView(this).apply {
            text = "병변 형태 비교 분석"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            includeFontPadding = false
        }

        val subtitleText = TextView(this).apply {
            text = "AI 분석 결과를 이해하기 위한 형태 비교 정보입니다."
            textSize = 13f
            setTextColor(subText)
            setPadding(0, dp(5), 0, 0)
        }

        titleBox.addView(titleText)
        titleBox.addView(subtitleText)

        val closeText = TextView(this).apply {
            text = "닫기 >"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener {
                finish()
            }
        }

        topRow.addView(titleBox)
        topRow.addView(closeText)

        rootLayout.addView(topRow)
        rootLayout.addView(makeIntroCard())

        rootLayout.addView(
            makeMetricCard(
                letter = "A",
                title = "형태 균형",
                leftLabel = "대칭에 가까움",
                rightLabel = "형태 차이 증가",
                valueText = if (baseAOverlapDiffPercent >= 0f) {
                    "겹치지 않는 영역: ${"%.0f".format(baseAOverlapDiffPercent)}%"
                } else {
                    "겹치지 않는 영역: 계산 안 됨"
                },
                positionRatio = normalizeA(baseAOverlapDiffPercent),
                description = "병변을 좌우 또는 상하로 나누어 비교했을 때 겹치지 않는 영역의 비율입니다.\n값이 작을수록 양쪽 형태가 더 비슷하고, 값이 커질수록 양쪽 형태 차이가 큰 형태입니다.\n이 값은 진단 기준이 아니라 병변 형태를 설명하기 위한 참고 지표입니다.",
                accentColor = green
            )
        )

        rootLayout.addView(
            makeMetricCard(
                letter = "B",
                title = "경계 형태",
                leftLabel = "원형에 가까움",
                rightLabel = "경계 길이 증가",
                valueText = if (baseBBoundaryRatio >= 0f) {
                    if (baseBBoundaryRatio >= 2.0f) {
                        "원 대비 경계 길이: 2.0배 이상"
                    } else {
                        "원 대비 경계 길이: ${"%.2f".format(baseBBoundaryRatio)}배"
                    }
                } else {
                    "원 대비 경계 길이: 계산 안 됨"
                },
                positionRatio = normalizeB(baseBBoundaryRatio),
                description = "같은 넓이의 원을 기준으로 병변 경계 길이를 비교한 값입니다.\n1.0배에 가까울수록 원형에 가깝고, 값이 커질수록 경계 길이가 더 긴 형태입니다.\n이 값은 진단 기준이 아니라 병변 형태를 설명하기 위한 참고 지표입니다.",
                accentColor = blue
            )
        )

        rootLayout.addView(
            makeMetricCard(
                letter = "C",
                title = "색상 분포",
                leftLabel = "균일한 색상",
                rightLabel = "색상 영역 증가",
                valueText = if (baseCColorRegionCount > 0) {
                    "대표 색상 영역: ${baseCColorRegionCount}개"
                } else {
                    "대표 색상 영역: 계산 안 됨"
                },
                positionRatio = normalizeC(baseCColorRegionCount),
                description = "병변 내부 색상을 유사한 색상끼리 묶어 구분한 결과입니다.\n영역 수가 적을수록 색상이 비교적 균일하게 계산되고, 영역 수가 많을수록 서로 다른 색상 영역이 더 많이 구분된다는 뜻입니다.\n이 값은 진단 기준이 아니라 병변 색상 분포를 설명하기 위한 참고 지표입니다.",
                accentColor = orange
            )
        )

        rootLayout.addView(
            makeMetricCard(
                letter = "D",
                title = "크기",
                leftLabel = "작은 크기",
                rightLabel = "크기 증가",
                valueText = if (displayDiameterMm >= 0f) {
                    "추정 직경: ${"%.1f".format(displayDiameterMm)} mm"
                } else {
                    "추정 직경: 기준 물체 없음"
                },
                positionRatio = normalizeD(displayDiameterMm),
                description = if (displayDiameterMm >= 0f) {
                    "100원 동전과 같은 기준 물체를 이용해 병변의 픽셀 크기를\n실제 mm 단위로 환산한 값입니다.\n크기 정보는 병변 상태를 이해하기 위한 참고 지표이며,\n크기만으로 악성 여부를 판단하지 않습니다."
                } else {
                    "현재 1차 분석 사진에는 기준 물체가 없어 실제 직경(mm)을 계산하지 않았습니다.\n크기 변화를 관리하려면 [크기 측정하기]를 눌러 100원 동전과 함께 촬영해주세요.\n크기 정보는 진단 기준이 아니라 병변 상태를 이해하기 위한 참고 지표입니다."
                },
                accentColor = purple,
                actionText = "크기 측정하기",
                onActionClick = {
                    openSizeMeasurement()
                }
            )
        )

        rootLayout.addView(makeCautionCard())

        val scrollView = ScrollView(this).apply {
            addView(rootLayout)
        }

        setContentView(scrollView)
    }

    private fun makeIntroCard(): LinearLayout {
        return makeCard().apply {
            background = roundedBg(Color.rgb(250, 254, 251), 20, borderColor, 1)

            val label = TextView(this@AbcdShapeAnalysisActivity).apply {
                text = "참고 안내"
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(green)
                background = roundedBg(softGreen, 20)
                setPadding(dp(10), dp(5), dp(10), dp(5))
                includeFontPadding = false
            }

            addView(
                label,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )

            addView(TextView(this@AbcdShapeAnalysisActivity).apply {
                text = "아래 항목은 진단 기준이 아니라, 촬영된 병변의 모양·경계·색상·크기를 계산한 참고 정보입니다.\nAI 분류 결과를 사용자가 이해하기 쉽도록 돕기 위한 형태 비교 지표입니다."
                textSize = 14f
                setTextColor(darkText)
                setPadding(0, dp(12), 0, 0)
                setLineSpacing(dp(3).toFloat(), 1.0f)
            })
        }
    }

    private fun makeMetricCard(
        letter: String,
        title: String,
        leftLabel: String,
        rightLabel: String,
        valueText: String,
        positionRatio: Float,
        description: String,
        accentColor: Int,
        actionText: String? = null,
        onActionClick: (() -> Unit)? = null
    ): LinearLayout {
        val card = makeCard()

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val letterCircle = TextView(this).apply {
            text = letter
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = roundedBg(accentColor, 100)
        }

        val titleText = TextView(this).apply {
            text = title
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            setPadding(dp(10), 0, 0, 0)
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        headerRow.addView(
            letterCircle,
            LinearLayout.LayoutParams(dp(32), dp(32))
        )
        headerRow.addView(titleText)

        if (actionText != null) {
            val actionButton = TextView(this).apply {
                text = actionText
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(accentColor)
                gravity = Gravity.CENTER
                background = roundedBg(Color.rgb(248, 250, 249), 20)
                setPadding(dp(10), dp(6), dp(10), dp(6))
                includeFontPadding = false
                setOnClickListener {
                    onActionClick?.invoke()
                }
            }

            headerRow.addView(actionButton)
        }

        card.addView(headerRow)

        val axisLabelRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(16), 0, dp(6))
        }

        axisLabelRow.addView(TextView(this).apply {
            text = leftLabel
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(subText)
            gravity = Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        })

        axisLabelRow.addView(TextView(this).apply {
            text = rightLabel
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(subText)
            gravity = Gravity.END
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        })

        card.addView(axisLabelRow)

        val axisView = ComparisonAxisView(this).apply {
            this.positionRatio = positionRatio
            this.accentColor = accentColor
        }

        card.addView(
            axisView,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(28)
            )
        )

        val valueBox = TextView(this).apply {
            text = valueText
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accentColor)
            background = roundedBg(Color.rgb(248, 250, 249), 14)
            setPadding(dp(12), dp(9), dp(12), dp(9))
            includeFontPadding = false
        }

        card.addView(
            valueBox,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(12)
            }
        )

        val descText = TextView(this).apply {
            text = description
            textSize = 13f
            setTextColor(subText)
            setPadding(0, dp(12), 0, 0)
            setLineSpacing(dp(3).toFloat(), 1.0f)
        }

        card.addView(descText)

        return card
    }

    private fun makeCautionCard(): LinearLayout {
        val cautionCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = roundedBg(Color.WHITE, 20, borderColor, 1)
            elevation = dp(2).toFloat()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(16)
            }
        }

        val cautionIcon = TextView(this).apply {
            text = "!"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = roundedBg(danger, 100)
        }

        val cautionTextBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val cautionTitle = TextView(this).apply {
            text = "주의"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            includeFontPadding = false
        }

        val cautionDesc = TextView(this).apply {
            text = "본 화면은 의료진의 확정 진단을 대신하지 않습니다.\n사진 조건, 조명, 초점, ROI 선택 범위, mask 품질에 따라 계산 결과가 달라질 수 있습니다."
            textSize = 13f
            setTextColor(subText)
            setPadding(0, dp(7), 0, 0)
            setLineSpacing(dp(3).toFloat(), 1.0f)
        }

        cautionTextBox.addView(cautionTitle)
        cautionTextBox.addView(cautionDesc)

        cautionCard.addView(
            cautionIcon,
            LinearLayout.LayoutParams(dp(28), dp(28))
        )
        cautionCard.addView(cautionTextBox)

        return cautionCard
    }

    private fun openSizeMeasurement() {
        val intent = Intent(this, LesionSelectActivity::class.java).apply {
            putExtra("from_abcd_size_measurement", true)
            putExtra("pred_class", predClassForTracking)
            putExtra("melanoma_prob", melanomaProbForTracking)
            putExtra("image_path", imagePathForTracking)
            putExtra("mask_image_path", maskImagePathForTracking)
        }

        startActivity(intent)
    }

    private fun refreshLatestDiameter() {
        val prefs = getSharedPreferences("abcd_measurement_cache", MODE_PRIVATE)
        latestMeasuredDiameterMm = prefs.getFloat("latest_diameter_mm", -1f)
    }

    private fun normalizeA(value: Float): Float {
        if (value < 0f) return 0.5f
        return clamp(value / 60f)
    }

    private fun normalizeB(value: Float): Float {
        if (value < 0f) return 0.5f
        return clamp((value - 1.0f) / 1.0f)
    }

    private fun normalizeC(value: Int): Float {
        if (value <= 0) return 0.5f
        return clamp((value - 1).toFloat() / 5f)
    }

    private fun normalizeD(value: Float): Float {
        if (value < 0f) return 0.5f
        return clamp(value / 12f)
    }

    private fun clamp(value: Float): Float {
        return max(0f, min(1f, value))
    }

    private fun makeCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBg(cardColor, 20, borderColor, 1)
            elevation = dp(3).toFloat()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        }
    }

    private fun roundedBg(
        color: Int,
        radiusDp: Int,
        strokeColor: Int? = null,
        strokeWidthDp: Int = 0
    ): GradientDrawable {
        return GradientDrawable().apply {
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
}

class ComparisonAxisView(context: Context) : View(context) {

    var positionRatio: Float = 0.5f
    var accentColor: Int = Color.rgb(67, 153, 75)

    private val linePaint = Paint().apply {
        color = Color.rgb(220, 230, 224)
        strokeWidth = 6f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val activeLinePaint = Paint().apply {
        strokeWidth = 6f
        isAntiAlias = true
        strokeCap = Paint.Cap.ROUND
    }

    private val dotPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val dotStrokePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        activeLinePaint.color = accentColor
        dotPaint.color = accentColor

        val centerY = height / 2f
        val startX = 8f
        val endX = width - 8f

        val clampedRatio = positionRatio.coerceIn(0f, 1f)
        val dotX = startX + (endX - startX) * clampedRatio

        canvas.drawLine(startX, centerY, endX, centerY, linePaint)
        canvas.drawLine(startX, centerY, dotX, centerY, activeLinePaint)

        canvas.drawCircle(dotX, centerY, 10f, dotPaint)
        canvas.drawCircle(dotX, centerY, 10f, dotStrokePaint)
    }
}