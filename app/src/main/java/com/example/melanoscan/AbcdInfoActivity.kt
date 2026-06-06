package com.example.melanoscan

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AbcdInfoActivity : AppCompatActivity() {

    private val bgColor = Color.rgb(240, 249, 243)
    private val cardColor = Color.WHITE

    private val green = Color.rgb(67, 153, 75)
    private val deepGreen = Color.rgb(48, 120, 58)
    private val softGreen = Color.rgb(232, 247, 236)

    private val darkText = Color.rgb(34, 44, 58)
    private val subText = Color.rgb(110, 125, 118)
    private val borderColor = Color.rgb(207, 232, 214)

    private val danger = Color.rgb(239, 83, 80)
    private val warning = Color.rgb(245, 160, 45)
    private val purple = Color.rgb(120, 110, 230)
    private val blue = Color.rgb(70, 135, 190)
    private val teal = Color.rgb(55, 160, 150)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUi()
    }

    private fun setupUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dp(20), dp(24), dp(20), dp(28))
            clipToPadding = false
            clipChildren = false
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
            text = "ABCD 병변 관찰법"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            includeFontPadding = false
        }

        val subtitleText = TextView(this).apply {
            text = "병변의 모양·경계·색·크기 변화를 쉽게 살펴보는 기준"
            textSize = 13f
            setTextColor(subText)
            setPadding(0, dp(5), 0, 0)
        }

        titleBox.addView(titleText)
        titleBox.addView(subtitleText)

        val backText = TextView(this).apply {
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
        topRow.addView(backText)

        root.addView(topRow)

        val imageCard = makeCard().apply {
            addView(TextView(this@AbcdInfoActivity).apply {
                text = "ABCDE 관찰 예시"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            addView(TextView(this@AbcdInfoActivity).apply {
                text = "양성 병변과 의심 병변을 비교하면서 관찰 포인트를 확인할 수 있습니다."
                textSize = 13f
                setTextColor(subText)
                setPadding(0, dp(6), 0, dp(12))
            })

            val imageView = ImageView(this@AbcdInfoActivity).apply {
                setImageResource(R.drawable.abcd_ex2)
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                background = roundedBg(Color.rgb(248, 250, 249), 16)
            }

            addView(
                imageView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(420)
                )
            )
        }

        root.addView(imageCard)

        val summaryCard = makeCard().apply {
            background = roundedBg(Color.rgb(250, 254, 251), 20, borderColor, 1)

            val label = TextView(this@AbcdInfoActivity).apply {
                text = "핵심 요약"
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

            addView(TextView(this@AbcdInfoActivity).apply {
                text = "ABCD는 병변을 확정 진단하는 기준이 아니라, 점이나 병변을 관찰할 때 주의해서 볼 특징을 정리한 참고 기준입니다.\n병변이 비대칭이거나, 경계가 고르지 않거나,\n색이 다양하거나, 크기와 모양이 변하면 관찰이 필요합니다."
                textSize = 14f
                setTextColor(darkText)
                setPadding(0, dp(12), 0, 0)
                setLineSpacing(dp(2).toFloat(), 1.0f)
            })
        }

        root.addView(summaryCard)

        root.addView(sectionTitle("ABCD로 살펴볼 특징"))

        root.addView(
            makeAbcdFeatureCard(
                letter = "A",
                title = "비대칭성",
                subtitle = "Asymmetry",
                description = "병변을 가운데로 나눴을 때 양쪽 모양이 비슷하지 않고\n한쪽으로 치우쳐 보이면 주의해서 볼 수 있습니다.\n일반적인 점은 비교적 균형 잡힌 모양을 보이는 경우가\n많습니다.",
                accentColor = green
            )
        )

        root.addView(
            makeAbcdFeatureCard(
                letter = "B",
                title = "경계 형태",
                subtitle = "Border",
                description = "병변의 가장자리가 매끄럽지 않고 울퉁불퉁하거나,\n주변 피부로 번지는 것처럼 흐릿하게 보이면 관찰이 필요합니다. \n경계가 불규칙할수록 병변의 형태 변화를 확인하는 것이 좋습니다.",
                accentColor = blue
            )
        )

        root.addView(
            makeAbcdFeatureCard(
                letter = "C",
                title = "색상 다양성",
                subtitle = "Color",
                description = "한 병변 안에 연한 갈색, 진한 갈색, 검은색, 붉은색처럼\n여러 색이 섞여 보이면 주의가 필요합니다. 색이 고르지 않거나 일부 영역만 더 진해지는 변화도 관찰 대상입니다.",
                accentColor = warning
            )
        )

        root.addView(
            makeAbcdFeatureCard(
                letter = "D",
                title = "직경과 크기",
                subtitle = "Diameter",
                description = "병변이 눈에 띄게 크거나 시간이 지나며 커지는 경우에는 크기 변화를 기록하는 것이 도움이 됩니다.\nMelanoScan에서는 100원 동전을 기준으로 병변 크기를 mm 단위로 추적할 수 있습니다.",
                accentColor = purple
            )
        )

        root.addView(
            makeAbcdFeatureCard(
                letter = "E",
                title = "변화 여부",
                subtitle = "Evolution",
                description = "기존 점의 크기, 모양, 색이 변하거나 새 증상인 가려움,\n출혈, 딱지, 통증이 생기면 주의가 필요합니다. \n한 번의 사진보다 날짜별 기록을 비교하는 것이 중요합니다.",
                accentColor = teal
            )
        )

        val appGuideCard = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(dp(16), dp(15), dp(16), dp(15))
            background = roundedBg(Color.WHITE, 20, borderColor, 1)
            elevation = dp(2).toFloat()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(12)
            }
        }

        val guideIcon = TextView(this).apply {
            text = "✓"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = roundedBg(green, 100)
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
            text = "MelanoScan에서는 이렇게 활용합니다"
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            includeFontPadding = false
        }

        val guideDesc = TextView(this).apply {
            text = "1차 AI 분석에서는 ROI 내부 병변 mask를 이용해 A/B/C 보조 지표를 계산하고, 변화 추적에서는 100원 동전을 \n기준으로 D 크기 변화를 기록합니다. E 변화 여부는\n날짜별 측정 기록과 그래프를 통해 확인할 수 있습니다."
            textSize = 13f
            setTextColor(subText)
            setPadding(0, dp(6), 0, 0)
            setLineSpacing(dp(2).toFloat(), 1.0f)
        }

        guideTextBox.addView(guideTitle)
        guideTextBox.addView(guideDesc)

        appGuideCard.addView(
            guideIcon,
            LinearLayout.LayoutParams(dp(28), dp(28))
        )
        appGuideCard.addView(guideTextBox)

        root.addView(appGuideCard)

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
            text = "ABCD 관찰법은 확정 진단이 아니라 병변을 살펴볼 때 \n참고하는 기준입니다.\n의심되는 변화가 있거나 앱 분석 결과가 높게 나타나면\n피부과 전문의 상담을 권장합니다."
            textSize = 13f
            setTextColor(subText)
            setPadding(0, dp(6), 0, 0)
            setLineSpacing(dp(2).toFloat(), 1.0f)
        }

        cautionTextBox.addView(cautionTitle)
        cautionTextBox.addView(cautionDesc)

        cautionCard.addView(
            cautionIcon,
            LinearLayout.LayoutParams(dp(28), dp(28))
        )
        cautionCard.addView(cautionTextBox)

        root.addView(cautionCard)

        val scrollView = ScrollView(this).apply {
            addView(root)
        }

        setContentView(scrollView)
    }

    private fun sectionTitle(text: String): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            setPadding(0, dp(8), 0, dp(12))
            includeFontPadding = false
        }
    }

    private fun makeAbcdFeatureCard(
        letter: String,
        title: String,
        subtitle: String,
        description: String,
        accentColor: Int
    ): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(dp(15), dp(15), dp(15), dp(15))
            background = roundedBg(cardColor, 20, borderColor, 1)
            elevation = dp(2).toFloat()

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }
        }

        val letterCircle = TextView(this).apply {
            text = letter
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = roundedBg(accentColor, 100)
        }

        val textBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(12), 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val titleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val titleText = TextView(this).apply {
            text = title
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            includeFontPadding = false
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val subtitleBadge = TextView(this).apply {
            text = subtitle
            textSize = 11f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(accentColor)
            gravity = Gravity.CENTER
            background = roundedBg(Color.rgb(248, 250, 249), 20)
            setPadding(dp(8), dp(4), dp(8), dp(4))
            includeFontPadding = false
        }

        titleRow.addView(titleText)
        titleRow.addView(subtitleBadge)

        val descText = TextView(this).apply {
            text = description
            textSize = 13f
            setTextColor(subText)
            setPadding(0, dp(8), 0, 0)
            setLineSpacing(dp(2).toFloat(), 1.0f)
        }

        textBox.addView(titleRow)
        textBox.addView(descText)

        card.addView(
            letterCircle,
            LinearLayout.LayoutParams(dp(32), dp(32))
        )
        card.addView(textBox)

        return card
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