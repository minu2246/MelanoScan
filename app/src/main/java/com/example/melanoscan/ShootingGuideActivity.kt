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

class ShootingGuideActivity : AppCompatActivity() {

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
    private val blue = Color.rgb(70, 135, 190)
    private val purple = Color.rgb(120, 110, 230)

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
            text = "촬영 가이드"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            includeFontPadding = false
        }

        val subtitleText = TextView(this).apply {
            text = "AI 분석과 크기 측정을 위한 사진 촬영 방법"
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

        root.addView(makeIntroCard())

        root.addView(
            makeGuideImageCard(
                title = "1. 병변만 촬영할 때",
                subtitle = "AI 분석용 사진은 병변이 선명하게 보이는 것이 가장 중요합니다.",
                imageResId = R.drawable.camera1
            )
        )

        root.addView(sectionTitle("병변만 촬영할 때 확인할 점"))

        root.addView(
            makeGuideStepCard(
                number = "1",
                title = "병변이 화면 중앙에 오도록 촬영",
                description = "병변이 너무 작게 보이지 않도록\n화면 중앙에 맞춰 촬영하세요.\n주변 피부는 약간 포함하는 것이 좋습니다.",
                accentColor = green
            )
        )

        root.addView(
            makeGuideStepCard(
                number = "2",
                title = "초점이 병변에 맞도록 하기",
                description = "사진이 흐리면 AI 분석과 mask 추정이\n불안정해질 수 있습니다.\n촬영 전 병변 부위를 터치해 초점을 맞추세요.",
                accentColor = blue
            )
        )

        root.addView(
            makeGuideStepCard(
                number = "3",
                title = "빛 반사와 그림자 줄이기",
                description = "강한 조명 반사나 손가락 그림자는\n색상 분석을 왜곡할 수 있습니다.\n밝고 균일한 조명에서 촬영하세요.",
                accentColor = warning
            )
        )

        root.addView(
            makeGuideStepCard(
                number = "4",
                title = "털이나 이물질이 가리지 않게 하기",
                description = "병변 위를 털, 옷, 손가락, 먼지가 가리면\n경계와 색상 분석이 달라질 수 있습니다.",
                accentColor = purple
            )
        )

        root.addView(
            makeGuideImageCard(
                title = "2. 변화 추적용 촬영",
                subtitle = "크기 측정은 병변 옆에 100원 동전이 함께 보여야 합니다.",
                imageResId = R.drawable.camera2
            )
        )

        root.addView(sectionTitle("병변 변화 추적 촬영 시 확인할 점"))

        root.addView(
            makeGuideStepCard(
                number = "1",
                title = "병변 옆에 100원 동전 놓기",
                description = "동전은 실제 크기를 계산하기 위한 기준입니다.\n동전 전체가 보이도록 병변 옆에 두고 촬영하세요.",
                accentColor = green
            )
        )

        root.addView(
            makeGuideStepCard(
                number = "2",
                title = "병변과 동전은 같은 평면에 두기",
                description = "병변과 동전의 높이가 다르면\n크기 계산에 오차가 생길 수 있습니다.\n가능한 한 같은 피부 표면 위에 두세요.",
                accentColor = blue
            )
        )

        root.addView(
            makeGuideStepCard(
                number = "3",
                title = "카메라를 너무 기울이지 않기",
                description = "비스듬히 찍으면 동전과 병변의\n픽셀 크기가 왜곡될 수 있습니다.\n정면에서 촬영하는 것이 좋습니다.",
                accentColor = warning
            )
        )

        root.addView(
            makeGuideStepCard(
                number = "4",
                title = "동전이 잘리지 않게 촬영",
                description = "동전이 일부 잘리거나 흐리면\n동전 검출이 실패할 수 있습니다.\n동전 전체가 선명하게 보이도록 촬영하세요.",
                accentColor = purple
            )
        )

        root.addView(makeCautionCard())

        val scrollView = ScrollView(this).apply {
            addView(root)
        }

        setContentView(scrollView)
    }

    private fun makeIntroCard(): LinearLayout {
        return makeCard().apply {
            background = roundedBg(Color.rgb(250, 254, 251), 20, borderColor, 1)

            val label = TextView(this@ShootingGuideActivity).apply {
                text = "촬영 전 확인"
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

            addView(TextView(this@ShootingGuideActivity).apply {
                text = "사진 품질은 AI 분석 결과와 병변 크기 측정에 직접적인 영향을 줍니다. 병변이 선명하게 보이고, 조명과 초점이 안정적인 사진을 사용하는 것이 좋습니다."
                textSize = 14f
                setTextColor(darkText)
                setPadding(0, dp(12), 0, 0)
                setLineSpacing(dp(2).toFloat(), 1.0f)
            })
        }
    }

    private fun makeGuideImageCard(
        title: String,
        subtitle: String,
        imageResId: Int
    ): LinearLayout {
        return makeCard().apply {
            addView(TextView(this@ShootingGuideActivity).apply {
                text = title
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
                includeFontPadding = false
            })

            addView(TextView(this@ShootingGuideActivity).apply {
                text = subtitle
                textSize = 13f
                setTextColor(subText)
                setPadding(0, dp(6), 0, dp(12))
                setLineSpacing(dp(2).toFloat(), 1.0f)
            })

            val imageView = RoundedImageView(this@ShootingGuideActivity).apply {
                setImageResource(imageResId)
                scaleType = ImageView.ScaleType.CENTER_CROP
                radiusPx = dp(16).toFloat()
                background = roundedBg(Color.rgb(248, 250, 249), 16)
                setPadding(0, 0, 0, 0)
            }
            addView(
                imageView,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(250)
                )
            )
        }
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

    private fun makeGuideStepCard(
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
            textSize = 12.5f
            setTextColor(subText)
            setPadding(0, dp(7), 0, 0)
            setLineSpacing(dp(3).toFloat(), 1.0f)
            includeFontPadding = true
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
                topMargin = dp(4)
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
            text = "촬영 가이드는 분석 품질을 높이기 위한 안내입니다.\n사진 조건, 조명, 초점, ROI 선택 범위,\n동전 배치에 따라 결과가 달라질 수 있습니다."
            textSize = 12.5f
            setTextColor(subText)
            setPadding(0, dp(7), 0, 0)
            setLineSpacing(dp(3).toFloat(), 1.0f)
            includeFontPadding = true
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