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

class MelanomaInfoActivity : AppCompatActivity() {

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
            text = "흑색종 위험 신호"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            includeFontPadding = false
        }

        val subtitleText = TextView(this).apply {
            text = "외형과 증상으로 확인할 수 있는 주의 특징"
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
            addView(TextView(this@MelanomaInfoActivity).apply {
                text = "흑색종 예시 이미지"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            addView(TextView(this@MelanomaInfoActivity).apply {
                text = "색과 경계가 고르지 않게 보이는 병변 예시입니다."
                textSize = 13f
                setTextColor(subText)
                setPadding(0, dp(6), 0, dp(12))
            })

            val imageView = ImageView(this@MelanomaInfoActivity).apply {
                setImageResource(R.drawable.melanoma_image)
                scaleType = ImageView.ScaleType.CENTER_CROP
                background = roundedBg(Color.rgb(235, 238, 236), 16)
                clipToOutline = true
            }

            addView(
                imageView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(220)
                )
            )
        }

        root.addView(imageCard)

        val summaryCard = makeCard().apply {
            background = roundedBg(Color.rgb(250, 254, 251), 20, borderColor, 1)

            val label = TextView(this@MelanomaInfoActivity).apply {
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

            addView(TextView(this@MelanomaInfoActivity).apply {
                text = "흑색종은 처음에는 평범한 점처럼 보일 수 있지만, \n시간이 지나면서 크기·색·모양이 변하거나 다른 점들과\n 다르게 보일 수 있습니다."
                textSize = 14f
                setTextColor(darkText)
                setPadding(0, dp(12), 0, 0)
                setLineSpacing(dp(2).toFloat(), 1.0f)
            })
        }

        root.addView(summaryCard)

        root.addView(sectionTitle("주의해서 볼 특징"))

        root.addView(
            makeWarningFeatureCard(
                number = "1",
                title = "새로 생겼거나 갑자기 눈에 띄는 점",
                description = "이전에는 없던 점이 새로 생겼거나, 기존 점과 다르게\n 갑자기 눈에 띄기 시작했다면 관찰이 필요합니다.",
                accentColor = green
            )
        )

        root.addView(
            makeWarningFeatureCard(
                number = "2",
                title = "여러 색이 섞여 보이는 점",
                description = "한 병변 안에 연한 갈색, 진한 갈색, 검은색, 붉은색처럼\n 여러 색이 섞여 보이면 주의가 필요합니다.",
                accentColor = warning
            )
        )

        root.addView(
            makeWarningFeatureCard(
                number = "3",
                title = "경계가 흐릿하거나 번져 보이는 점",
                description = "점의 가장자리가 깔끔하지 않고 모양이 고르지 \n 않게 보이는 경우 관찰이 필요합니다.",
                accentColor = blue
            )
        )

        root.addView(
            makeWarningFeatureCard(
                number = "4",
                title = "점점 커지거나 모양이 변하는 점",
                description = "기존 점의 크기, 색, 모양이 예전과 달라졌다면 단순한\n 변화로 넘기지 않는 것이 좋습니다.",
                accentColor = purple
            )
        )

        root.addView(
            makeWarningFeatureCard(
                number = "5",
                title = "가렵거나 피가 나는 점",
                description = "가려움, 출혈, 딱지, 통증, 잘 낫지 않는 상처 같은 변화가 있다면 피부과 상담을 권장합니다.",
                accentColor = danger
            )
        )

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
                topMargin = dp(6)
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
            text = "이 내용은 자가 진단이 아니라 피부 병변을 관찰할 때\n 참고할 수 있는 정보입니다. 의심되는 변화가 있거나\n 앱 분석 결과가 높게 나타나면 피부과 전문의 상담을 권장합니다."
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

    private fun makeWarningFeatureCard(
        number: String,
        title: String,
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

        val numberCircle = TextView(this).apply {
            text = number
            textSize = 14f
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

        val titleText = TextView(this).apply {
            text = title
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            includeFontPadding = false
        }

        val descText = TextView(this).apply {
            text = description
            textSize = 13f
            setTextColor(subText)
            setPadding(0, dp(7), 0, 0)
            setLineSpacing(dp(2).toFloat(), 1.0f)
        }

        textBox.addView(titleText)
        textBox.addView(descText)

        card.addView(
            numberCircle,
            LinearLayout.LayoutParams(dp(30), dp(30))
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