package com.example.melanoscan

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView

class MainActivity : AppCompatActivity() {

    private val bgColor = Color.rgb(240, 249, 243)
    private val cardColor = Color.WHITE

    private val green = Color.rgb(67, 153, 75)
    private val deepGreen = Color.rgb(48, 120, 58)
    private val softGreen = Color.rgb(232, 247, 236)
    private val verySoftGreen = Color.rgb(246, 252, 248)

    private val darkText = Color.rgb(34, 44, 58)
    private val subText = Color.rgb(110, 125, 118)
    private val borderColor = Color.rgb(207, 232, 214)

    private val danger = Color.rgb(239, 83, 80)
    private val purple = Color.rgb(120, 110, 230)
    private val yellow = Color.rgb(190, 150, 35)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("risk_check_prefs", Context.MODE_PRIVATE)
        val alreadyChecked = prefs.getBoolean("risk_checked", false)

        if (alreadyChecked) {
            setupHomeUi()
        } else {
            setupRiskCheckUi()
        }
    }

    private fun setupRiskCheckUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dp(20), dp(26), dp(20), dp(30))
        }

        val title = TextView(this).apply {
            text = "흑색종 위험요인 체크"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(deepGreen)
            gravity = Gravity.CENTER
            includeFontPadding = false
        }

        val description = TextView(this).apply {
            text = "이 설문은 진단이 아니라 흑색종 위험요인을 확인하기 위한 참고용입니다."
            textSize = 13.5f
            setTextColor(subText)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(18))
        }

        root.addView(title)
        root.addView(description)

        val ageCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = roundedBg(Color.WHITE, 20, borderColor, 1)
            elevation = dp(2).toFloat()
        }

        val ageLabel = TextView(this).apply {
            text = "나이 입력"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
        }

        val ageInput = EditText(this).apply {
            hint = "예: 23"
            textSize = 16f
            inputType = InputType.TYPE_CLASS_NUMBER
            setSingleLine(true)
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = roundedBg(verySoftGreen, 14, borderColor, 1)
        }

        ageCard.addView(ageLabel)

        ageCard.addView(
            ageInput,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                topMargin = dp(10)
            }
        )

        root.addView(
            ageCard,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(14)
            }
        )

        val cbLightSkin = makeRiskCheckBox("피부가 밝고 햇빛에 쉽게 타는 편인가요?")
        val cbFreckles = makeRiskCheckBox("주근깨가 잘 생기는 편인가요?")
        val cbManyMoles = makeRiskCheckBox("양쪽 팔에 점이 20개보다 많나요?")
        val cbSunburn = makeRiskCheckBox("어릴 때 또는 청소년기에 물집이 생길 정도로 심한 햇볕 화상을 입은 적이 있나요?")
        val cbUvCountry = makeRiskCheckBox("자외선이 강한 지역이나 국가에서 1년 이상 산 적이 있나요?")
        val cbPersonalHistory = makeRiskCheckBox("본인이 과거에 흑색종 진단을 받은 적이 있나요?")
        val cbFamilyHistory = makeRiskCheckBox("부모, 형제, 자녀 중 흑색종 진단을 받은 사람이 있나요?")

        root.addView(cbLightSkin)
        root.addView(cbFreckles)
        root.addView(cbManyMoles)
        root.addView(cbSunburn)
        root.addView(cbUvCountry)
        root.addView(cbPersonalHistory)
        root.addView(cbFamilyHistory)

        val resultCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = roundedBg(softGreen, 20, borderColor, 1)
            elevation = dp(2).toFloat()
        }

        val resultTitle = TextView(this).apply {
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(deepGreen)
            gravity = Gravity.CENTER
        }

        val resultDetail = TextView(this).apply {
            textSize = 13.5f
            setTextColor(darkText)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(16))
        }

        val goMainButton = Button(this).apply {
            text = "메인화면으로 이동"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = roundedBg(deepGreen, 16)

            setOnClickListener {
                getSharedPreferences("risk_check_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("risk_checked", true)
                    .apply()

                setupHomeUi()
            }
        }

        resultCard.addView(resultTitle)
        resultCard.addView(resultDetail)

        resultCard.addView(
            goMainButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            )
        )

        val checkButton = Button(this).apply {
            text = "결과 확인하기"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = roundedBg(green, 16)

            setOnClickListener {
                val age = ageInput.text.toString().toIntOrNull() ?: 0

                val riskCount = listOf(
                    cbLightSkin.isChecked,
                    cbFreckles.isChecked,
                    cbManyMoles.isChecked,
                    cbSunburn.isChecked,
                    cbUvCountry.isChecked,
                    cbPersonalHistory.isChecked,
                    cbFamilyHistory.isChecked
                ).count { it }

                val isHighRisk =
                    riskCount >= 3 ||
                            (age < 60 && cbManyMoles.isChecked) ||
                            (age >= 60 && cbFreckles.isChecked) ||
                            cbPersonalHistory.isChecked ||
                            cbFamilyHistory.isChecked

                if (isHighRisk) {
                    resultTitle.text = "고위험군 기준에 해당합니다"
                    resultDetail.text =
                        "입력한 정보가 흑색종 위험요인 기준에 해당합니다. 이 결과는 진단이 아니며, 피부과 전문의 상담 또는 정기적인 피부 검진을 권장합니다."
                } else {
                    resultTitle.text = "일반 위험군으로 분류됩니다"
                    resultDetail.text =
                        "현재 입력한 정보만으로는 고위험군 기준에 해당하지 않습니다. 다만 점의 크기, 색, 모양 변화가 있거나 출혈·가려움이 있다면 피부과 상담을 권장합니다."
                }

                resultCard.visibility = View.VISIBLE
            }
        }

        root.addView(
            checkButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            ).apply {
                topMargin = dp(14)
                bottomMargin = dp(16)
            }
        )

        root.addView(resultCard)

        val scrollView = ScrollView(this).apply {
            addView(root)
        }

        setContentView(scrollView)
    }

    private fun makeRiskCheckBox(text: String): CheckBox {
        return CheckBox(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(darkText)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            buttonTintList = android.content.res.ColorStateList.valueOf(green)
            background = roundedBg(Color.WHITE, 18, borderColor, 1)
            gravity = Gravity.CENTER_VERTICAL

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(8)
            }
        }
    }

    private fun setupHomeUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dp(20), dp(24), dp(20), dp(28))
            clipToPadding = false
            clipChildren = false
        }

        val topHeaderRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val greeting = TextView(this).apply {
            text = "안녕하세요"
            textSize = 12f
            setTextColor(Color.rgb(125, 160, 135))
            includeFontPadding = false
        }

        val title = TextView(this).apply {
            text = "Melano Scan"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            includeFontPadding = false
            setPadding(0, dp(2), 0, 0)
        }

        titleBox.addView(greeting)
        titleBox.addView(title)

        val logo = ImageView(this).apply {
            setImageResource(R.drawable.aida_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = false
        }

        topHeaderRow.addView(titleBox)

        topHeaderRow.addView(
            logo,
            LinearLayout.LayoutParams(
                dp(130),
                dp(70)
            )
        )

        val heroCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(22), dp(22), dp(16), dp(22))
            background = roundedBg(
                color = Color.WHITE,
                radiusDp = 24,
                strokeColor = borderColor,
                strokeWidthDp = 1
            )
            elevation = dp(3).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(18)
                bottomMargin = dp(18)
            }
        }

        val heroTextBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val heroTitle = TextView(this).apply {
            text = "사진 한 번으로\n피부 위험 체크"
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(deepGreen)
            includeFontPadding = true
            setLineSpacing(dp(2).toFloat(), 1.0f)
        }

        val heroSub = TextView(this).apply {
            text = "AI가 병변 특징을 빠르게 분석합니다"
            textSize = 14f
            setTextColor(subText)
            setPadding(0, dp(10), 0, 0)
        }

        heroTextBox.addView(heroTitle)
        heroTextBox.addView(heroSub)

        val appLogo = ImageView(this).apply {
            setImageResource(R.drawable.app_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = false
        }

        heroCard.addView(heroTextBox)

        heroCard.addView(
            appLogo,
            LinearLayout.LayoutParams(
                dp(114),
                dp(114)
            ).apply {
                marginStart = dp(10)
            }
        )

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            clipToPadding = false
            clipChildren = false
            setPadding(0, 0, 0, dp(8))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val analyzeCard = makeActionCard(
            icon = "📷",
            title = "촬영하기",
            sub = "병변 사진 분석",
            onClick = {
                startActivity(Intent(this@MainActivity, AiAnalysisActivity::class.java))
            }
        )

        val trackingCard = makeActionCard(
            icon = "📅",
            title = "병변 변화 추적",
            sub = "크기 변화 기록",
            onClick = {
                startActivity(Intent(this@MainActivity, HistoryActivity::class.java))
            }
        )

        val hospitalCard = makeActionCard(
            icon = "🏥",
            title = "인근 병원 찾기",
            sub = "가까운 피부과 추천",
            onClick = {
                startActivity(Intent(this@MainActivity, Search_Hospital::class.java))
            }
        )

        actionRow.addView(
            analyzeCard,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = dp(4)
                bottomMargin = dp(10)
            }
        )

        actionRow.addView(
            trackingCard,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dp(4)
                marginEnd = dp(4)
                bottomMargin = dp(10)
            }
        )

        actionRow.addView(
            hospitalCard,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dp(4)
                bottomMargin = dp(10)
            }
        )

        val contentTitleRow = makeTitleRow(
            title = "피부건강 콘텐츠 카드",
            right = ""
        )

        val contentScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            clipToPadding = false
            setPadding(0, 0, dp(10), dp(5))
        }

        val contentRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            clipToPadding = false
            clipChildren = false
        }

        contentRow.addView(
            makeContentCard(
                imageResId = R.drawable.mela,
                imageText = "피부 병변",
                imageBg = Color.rgb(247, 235, 225),
                badge = "위험신호",
                badgeColor = danger,
                title = "흑색종, 이런 점은\n위험할 수 있어요",
                sub = "증상과 외형적 특징 알아보기",
                imageScaleType = ImageView.ScaleType.CENTER_CROP,
                onClick = {
                    startActivity(Intent(this@MainActivity, MelanomaInfoActivity::class.java))
                }
            )
        )

        contentRow.addView(
            makeContentCard(
                imageResId = R.drawable.search,
                imageText = "ABCD",
                imageBg = Color.rgb(236, 238, 252),
                badge = "자가진단",
                badgeColor = purple,
                title = "ABCD 기준으로\n병변 특징 살펴보기",
                sub = "비대칭·경계·색·크기 확인",
                imageScaleType = ImageView.ScaleType.CENTER_CROP,
                onClick = {
                    startActivity(Intent(this@MainActivity, AbcdInfoActivity::class.java))
                }
            )
        )

        contentRow.addView(
            makeContentCard(
                imageResId = R.drawable.camera,
                imageText = "촬영 가이드",
                imageBg = Color.rgb(248, 242, 220),
                badge = "촬영 가이드",
                badgeColor = yellow,
                title = "이렇게 촬영하면\n더 정확해요",
                sub = "초점과 동전 배치 확인",
                imageScaleType = ImageView.ScaleType.CENTER_CROP,
                onClick = {
                    startActivity(Intent(this@MainActivity, ShootingGuideActivity::class.java))
                }
            )
        )

        contentScroll.addView(contentRow)

        val guideCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = roundedBg(
                color = Color.rgb(250, 254, 251),
                radiusDp = 20,
                strokeColor = borderColor,
                strokeWidthDp = 1
            )
            elevation = dp(2).toFloat()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(10)
                bottomMargin = dp(18)
            }
        }

        val guideIcon = TextView(this).apply {
            text = "💡"
            textSize = 24f
            gravity = Gravity.CENTER
            background = roundedBg(softGreen, 100)
        }

        val guideTextBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val guideTitle = TextView(this).apply {
            text = "분석 결과는 참고용입니다"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            includeFontPadding = false
        }

        val guideSub = TextView(this).apply {
            text = "병변이 커지거나 색이 변하면 전문의 상담을 권장합니다."
            textSize = 12f
            setTextColor(subText)
            setPadding(0, dp(5), 0, 0)
        }

        guideTextBox.addView(guideTitle)
        guideTextBox.addView(guideSub)

        guideCard.addView(
            guideIcon,
            LinearLayout.LayoutParams(dp(42), dp(42))
        )

        guideCard.addView(guideTextBox)

        val surveyRetryWrapper = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, dp(2), 0, 0)
        }

        val surveyRetryButton = TextView(this).apply {
            text = "위험요인 설문 다시하기"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(deepGreen)
            gravity = Gravity.CENTER
            setPadding(dp(14), dp(9), dp(14), dp(9))
            background = roundedBg(
                color = Color.WHITE,
                radiusDp = 100,
                strokeColor = borderColor,
                strokeWidthDp = 1
            )
            elevation = dp(2).toFloat()
            isClickable = true
            isFocusable = true

            setOnClickListener {
                getSharedPreferences("risk_check_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("risk_checked", false)
                    .apply()

                setupRiskCheckUi()
            }
        }

        surveyRetryWrapper.addView(
            surveyRetryButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        root.addView(topHeaderRow)
        root.addView(heroCard)
        root.addView(actionRow)
        root.addView(contentTitleRow)
        root.addView(contentScroll)
        root.addView(guideCard)
        root.addView(surveyRetryWrapper)

        val scrollView = ScrollView(this).apply {
            clipToPadding = false
            clipChildren = false
            addView(root)
        }

        setContentView(scrollView)
    }

    private fun makeTitleRow(
        title: String,
        right: String
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(18), 0, dp(12))

            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 20f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
                includeFontPadding = false
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            })

            addView(TextView(this@MainActivity).apply {
                text = right
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(green)
            })
        }
    }

    private fun makeActionCard(
        icon: String,
        title: String,
        sub: String,
        onClick: () -> Unit
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(16), dp(10), dp(16))
            background = roundedBg(
                color = Color.WHITE,
                radiusDp = 20,
                strokeColor = borderColor,
                strokeWidthDp = 1
            )
            elevation = dp(4).toFloat()
            isClickable = true
            isFocusable = true
            minimumHeight = dp(132)
            setOnClickListener { onClick() }

            val iconCircle = TextView(this@MainActivity).apply {
                text = icon
                textSize = 28f
                gravity = Gravity.CENTER
                background = roundedBg(verySoftGreen, 100, borderColor, 1)
                includeFontPadding = true
            }

            val titleText = TextView(this@MainActivity).apply {
                text = title
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
                gravity = Gravity.CENTER
                setPadding(0, dp(10), 0, 0)
                includeFontPadding = false
                maxLines = 2
            }

            val subTextView = TextView(this@MainActivity).apply {
                text = sub
                textSize = 10f
                setTextColor(subText)
                gravity = Gravity.CENTER
                setPadding(0, dp(5), 0, 0)
                includeFontPadding = false
                maxLines = 2
            }

            addView(
                iconCircle,
                LinearLayout.LayoutParams(dp(50), dp(50))
            )

            addView(titleText)
            addView(subTextView)
        }
    }

    private fun makeContentCard(
        imageResId: Int? = null,
        imageText: String,
        imageBg: Int,
        badge: String,
        badgeColor: Int,
        title: String,
        sub: String,
        imageScaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_CROP,
        onClick: (() -> Unit)? = null
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(10), dp(10), dp(10), dp(10))
            background = roundedBg(Color.WHITE, 18, borderColor, 1)
            elevation = dp(3).toFloat()
            clipToPadding = false
            clipChildren = false
            isClickable = onClick != null
            isFocusable = onClick != null

            if (onClick != null) {
                setOnClickListener {
                    onClick()
                }
            }

            layoutParams = LinearLayout.LayoutParams(
                dp(154),
                dp(265)
            ).apply {
                marginEnd = dp(12)
                bottomMargin = dp(6)
            }

            val imageBox = if (imageResId != null) {
                RoundedImageView(this@MainActivity).apply {
                    setImageResource(imageResId)
                    scaleType = imageScaleType
                    radiusPx = dp(14).toFloat()
                    background = roundedBg(imageBg, 14)
                    setPadding(0, 0, 0, 0)
                }
            } else {
                TextView(this@MainActivity).apply {
                    text = imageText
                    gravity = Gravity.CENTER
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(Color.rgb(105, 95, 80))
                    background = roundedBg(imageBg, 14)
                }
            }

            val badgeView = TextView(this@MainActivity).apply {
                text = badge
                textSize = 10f
                setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD
                gravity = Gravity.CENTER
                background = roundedBg(badgeColor, 20)
                setPadding(dp(7), dp(3), dp(7), dp(3))
                includeFontPadding = false
            }

            val titleView = TextView(this@MainActivity).apply {
                text = title
                textSize = 12.5f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
                setPadding(0, dp(8), 0, 0)
                setLineSpacing(dp(1).toFloat(), 1.0f)
                includeFontPadding = false
            }

            val subView = TextView(this@MainActivity).apply {
                text = sub
                textSize = 10.5f
                setTextColor(subText)
                setPadding(0, dp(6), 0, 0)
                maxLines = 2
                includeFontPadding = false
            }

            addView(
                imageBox,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(122)
                )
            )

            addView(
                badgeView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(8)
                }
            )

            addView(titleView)
            addView(subView)
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

class RoundedImageView(context: Context) : AppCompatImageView(context) {

    var radiusPx: Float = 0f

    private val clipPath = Path()
    private val rect = RectF()

    override fun onDraw(canvas: Canvas) {
        rect.set(0f, 0f, width.toFloat(), height.toFloat())

        clipPath.reset()
        clipPath.addRoundRect(
            rect,
            radiusPx,
            radiusPx,
            Path.Direction.CW
        )

        canvas.save()
        canvas.clipPath(clipPath)
        super.onDraw(canvas)
        canvas.restore()
    }
}