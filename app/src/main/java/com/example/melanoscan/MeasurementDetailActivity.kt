package com.example.melanoscan

import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MeasurementDetailActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var rootLayout: LinearLayout

    private val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)

    private val bgColor = Color.rgb(246, 250, 247)
    private val cardColor = Color.WHITE
    private val green = Color.rgb(67, 153, 75)
    private val lightGreen = Color.rgb(237, 248, 241)
    private val darkText = Color.rgb(35, 45, 55)
    private val subText = Color.rgb(110, 120, 125)
    private val borderColor = Color.rgb(225, 235, 228)
    private val danger = Color.rgb(235, 80, 80)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = AppDatabase.getDatabase(applicationContext)

        setupUi()

        val measurementId = intent.getLongExtra("measurement_id", -1L)

        if (measurementId <= 0L) {
            showError("측정 기록 ID가 없습니다.")
            return
        }

        loadMeasurement(measurementId)
    }

    private fun setupUi() {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }

        val scrollView = ScrollView(this).apply {
            addView(rootLayout)
        }

        setContentView(scrollView)
    }

    private fun loadMeasurement(measurementId: Long) {
        Thread {
            val record = db.lesionMeasurementDao().getById(measurementId)

            runOnUiThread {
                if (record == null) {
                    showError("측정 기록을 찾을 수 없습니다.")
                } else {
                    renderMeasurement(record)
                }
            }
        }.start()
    }

    private fun renderMeasurement(record: LesionMeasurementEntity) {
        rootLayout.removeAllViews()

        val titleRow = LinearLayout(this).apply {
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

        val titleTextView = TextView(this).apply {
            text = "측정 상세"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
        }

        val subtitleTextView = TextView(this).apply {
            text = "저장된 측정 사진과 결과를 확인합니다."
            textSize = 13f
            setTextColor(subText)
            setPadding(0, dp(4), 0, 0)
        }

        titleBox.addView(titleTextView)
        titleBox.addView(subtitleTextView)

        val backTextView = TextView(this).apply {
            text = "목록 >"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(8), dp(10), dp(8))
            setOnClickListener {
                finish()
            }
        }

        titleRow.addView(titleBox)
        titleRow.addView(backTextView)

        rootLayout.addView(titleRow)

        addImageCard(record)
        addResultCard(record)
        addGuideCard()
        addBackButton()
    }

    private fun addImageCard(record: LesionMeasurementEntity) {
        val imageCard = makeCard().apply {
            addView(TextView(this@MeasurementDetailActivity).apply {
                text = "측정 이미지"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            addView(TextView(this@MeasurementDetailActivity).apply {
                text = "병변과 100원 동전이 함께 촬영된 이미지입니다."
                textSize = 13f
                setTextColor(subText)
                setPadding(0, dp(6), 0, dp(12))
            })
        }

        val imagePath = record.measurementImagePath

        if (!imagePath.isNullOrBlank()) {
            val file = File(imagePath)

            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                val imageView = ImageView(this).apply {
                    setImageBitmap(bitmap)
                    adjustViewBounds = true
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    background = roundedBg(Color.rgb(235, 238, 236), 16)
                }

                imageCard.addView(
                    imageView,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        dp(320)
                    )
                )
            } else {
                imageCard.addView(makeInfoText("저장된 사진 파일을 찾을 수 없습니다."))
            }
        } else {
            imageCard.addView(makeInfoText("저장된 사진 경로가 없습니다."))
        }

        rootLayout.addView(imageCard)
    }

    private fun addResultCard(record: LesionMeasurementEntity) {
        val resultCard = makeCard().apply {
            addView(TextView(this@MeasurementDetailActivity).apply {
                text = "측정 결과"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            addView(TextView(this@MeasurementDetailActivity).apply {
                text = dateFormat.format(Date(record.createdAt))
                textSize = 12f
                setTextColor(subText)
                setPadding(0, dp(4), 0, dp(14))
            })
        }

        val metricRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val longestAxisCard = makeMiniMetricCard(
            title = "최장축",
            value = "${"%.2f".format(record.lesionLongestAxisMm)} mm",
            valueColor = green
        )

        val methodCard = makeMiniMetricCard(
            title = "분석 방식",
            value = if (record.melanomaProb >= 0f) "AI 분석 기반" else "직접 추적",
            valueColor = if (record.melanomaProb >= 0f) green else subText
        )

        metricRow.addView(
            longestAxisCard,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginEnd = dp(6)
            }
        )

        metricRow.addView(
            methodCard,
            LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            ).apply {
                marginStart = dp(6)
            }
        )

        resultCard.addView(metricRow)

        val aiInfoText = TextView(this).apply {
            text = if (record.melanomaProb >= 0f) {
                buildString {
                    appendLine("AI 예측 결과: ${record.predClass ?: "unknown"}")
                    append("melanoma 확률: ${"%.4f".format(record.melanomaProb)}")
                }
            } else {
                "사용자가 직접 병변을 추적하기 위해 저장한 측정 기록입니다."
            }
            textSize = 13f
            setTextColor(subText)
            setPadding(0, dp(14), 0, 0)
        }

        resultCard.addView(aiInfoText)

        rootLayout.addView(resultCard)
    }

    private fun addGuideCard() {
        val guideCard = makeCard().apply {
            addView(TextView(this@MeasurementDetailActivity).apply {
                text = "측정 기준 안내"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            addView(TextView(this@MeasurementDetailActivity).apply {
                text = "이 기록은 100원 동전의 실제 지름을 기준으로 병변 크기를 환산한 결과입니다. 촬영 각도, 조명, 초점, 병변과 동전의 높이 차이에 따라 오차가 발생할 수 있습니다."
                textSize = 13f
                setTextColor(subText)
                setPadding(0, dp(8), 0, 0)
            })
        }

        rootLayout.addView(guideCard)
    }

    private fun addBackButton() {
        val backButton = Button(this).apply {
            text = "기록으로 돌아가기"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = roundedBg(green, 14)
            setPadding(0, dp(10), 0, dp(10))
            setOnClickListener {
                finish()
            }
        }

        rootLayout.addView(
            backButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                topMargin = dp(4)
                bottomMargin = dp(20)
            }
        )
    }

    private fun showError(message: String) {
        rootLayout.removeAllViews()

        val titleTextView = TextView(this).apply {
            text = "측정 상세"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            setPadding(0, 0, 0, dp(18))
        }

        val errorCard = makeCard().apply {
            addView(TextView(this@MeasurementDetailActivity).apply {
                text = "기록을 불러올 수 없습니다"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(danger)
            })

            addView(TextView(this@MeasurementDetailActivity).apply {
                text = message
                textSize = 13f
                setTextColor(subText)
                setPadding(0, dp(8), 0, 0)
            })
        }

        rootLayout.addView(titleTextView)
        rootLayout.addView(errorCard)
        addBackButton()
    }

    private fun makeMiniMetricCard(
        title: String,
        value: String,
        valueColor: Int
    ): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = roundedBg(Color.rgb(248, 250, 249), 14)
            setPadding(dp(12), dp(12), dp(12), dp(12))

            addView(TextView(this@MeasurementDetailActivity).apply {
                text = title
                textSize = 12f
                setTextColor(subText)
            })

            addView(TextView(this@MeasurementDetailActivity).apply {
                text = value
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(valueColor)
                setPadding(0, dp(5), 0, 0)
            })
        }
    }

    private fun makeInfoText(message: String): TextView {
        return TextView(this).apply {
            text = message
            textSize = 14f
            setTextColor(subText)
            gravity = Gravity.CENTER
            background = roundedBg(Color.rgb(248, 250, 249), 14)
            setPadding(dp(16), dp(24), dp(16), dp(24))
        }
    }

    private fun makeCard(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBg(cardColor, 18, borderColor, 1)
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