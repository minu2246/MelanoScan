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
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var rootLayout: LinearLayout

    private val dateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.KOREA)
    private val shortDateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.KOREA)

    private var selectedLesionId: Long = -1L
    private var selectedLesionName: String? = null

    private val bgColor = Color.rgb(246, 250, 247)
    private val cardColor = Color.WHITE
    private val green = Color.rgb(67, 153, 75)
    private val lightGreen = Color.rgb(237, 248, 241)
    private val darkText = Color.rgb(35, 45, 55)
    private val subText = Color.rgb(110, 120, 125)
    private val borderColor = Color.rgb(225, 235, 228)
    private val danger = Color.rgb(235, 80, 80)
    private val warning = Color.rgb(245, 160, 45)
    private val blue = Color.rgb(50, 135, 220)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = AppDatabase.getDatabase(applicationContext)

        selectedLesionId = intent.getLongExtra("lesion_id", -1L)
        selectedLesionName = intent.getStringExtra("lesion_name")

        if (selectedLesionId > 0L) {
            setupDetailUi()
            loadSelectedLesionRecords(selectedLesionId)
        } else {
            setupProfileListUi()
            loadLesionProfiles()
        }
    }

    private fun setupProfileListUi() {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }

        val titleTextView = TextView(this).apply {
            text = "병변 변화 추적"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            setPadding(0, 0, 0, dp(6))
        }

        val guideTextView = TextView(this).apply {
            textSize = 14f
            setTextColor(subText)
            text = "관심 있는 병변을 등록하고, 동일 병변의 크기 변화를 기록할 수 있습니다."
            setPadding(0, 0, 0, dp(18))
        }

        val newLesionCard = makeCard().apply {
            addView(TextView(this@HistoryActivity).apply {
                text = "새 병변 추적 시작"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            addView(TextView(this@HistoryActivity).apply {
                text = "처음 관찰하는 병변이라면 이름을 등록하고 측정을 진행하세요. 병변은 3개월 이상 지속 관찰해야 합니다."
                textSize = 13f
                setTextColor(subText)
                setPadding(0, dp(8), 0, dp(14))
            })

            val newLesionButton = Button(this@HistoryActivity).apply {
                text = "+ 새 병변 등록 및 측정 시작"
                textSize = 15f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
                background = roundedBg(green, 14)
                setPadding(0, dp(10), 0, dp(10))
                setOnClickListener {
                    showNewLesionDialog()
                }
            }

            addView(
                newLesionButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(52)
                )
            )
        }

        val listTitleRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(10), 0, dp(12))
        }

        val listTitleTextView = TextView(this).apply {
            text = "기존 병변"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        listTitleRow.addView(listTitleTextView)
        rootLayout.addView(titleTextView)
        rootLayout.addView(guideTextView)
        rootLayout.addView(newLesionCard)
        rootLayout.addView(listTitleRow)

        val scrollView = ScrollView(this).apply {
            addView(rootLayout)
        }

        setContentView(scrollView)
    }

    private fun showNewLesionDialog() {
        val editText = EditText(this).apply {
            hint = "예: 왼쪽 팔 점"
            textSize = 16f
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        AlertDialog.Builder(this)
            .setTitle("새 병변 등록")
            .setMessage("추적할 병변 이름을 입력하세요.")
            .setView(editText)
            .setPositiveButton("등록") { _, _ ->
                val name = editText.text.toString().trim()

                if (name.isBlank()) {
                    return@setPositiveButton
                }

                createNewLesionAndStartMeasurement(name)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun createNewLesionAndStartMeasurement(name: String) {
        Thread {
            val profile = LesionProfileEntity(
                lesionName = name,
                createdAt = System.currentTimeMillis()
            )

            val lesionId = db.lesionProfileDao().insert(profile)

            runOnUiThread {
                startMeasurement(
                    lesionId = lesionId,
                    lesionName = name,
                    predClass = "직접 추적",
                    melanomaProb = -1f
                )
            }
        }.start()
    }

    private fun loadLesionProfiles() {
        Thread {
            val profiles = db.lesionProfileDao().getAll()

            val summaries = profiles.map { profile ->
                val recordsAsc = db.lesionMeasurementDao().getByLesionIdAsc(profile.lesionId)
                val latest = recordsAsc.lastOrNull()

                LesionProfileSummary(
                    lesionId = profile.lesionId,
                    lesionName = profile.lesionName,
                    recordCount = recordsAsc.size,
                    latestLongestAxisMm = latest?.lesionLongestAxisMm,
                    latestDate = latest?.createdAt
                )
            }

            runOnUiThread {
                renderProfileList(summaries)
            }
        }.start()
    }

    private fun renderProfileList(summaries: List<LesionProfileSummary>) {
        if (summaries.isEmpty()) {
            val emptyCard = makeCard().apply {
                gravity = Gravity.CENTER

                addView(TextView(this@HistoryActivity).apply {
                    text = "아직 등록된 병변이 없습니다"
                    textSize = 17f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(darkText)
                    gravity = Gravity.CENTER
                })

                addView(TextView(this@HistoryActivity).apply {
                    text = "새 병변을 등록하면 이곳에서 기록과 그래프를 확인할 수 있습니다."
                    textSize = 13f
                    setTextColor(subText)
                    gravity = Gravity.CENTER
                    setPadding(0, dp(8), 0, 0)
                })
            }

            rootLayout.addView(emptyCard)
            return
        }

        summaries.forEach { summary ->
            addProfileSummaryCard(summary)
        }
    }

    private fun addProfileSummaryCard(summary: LesionProfileSummary) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBg(cardColor, 18, borderColor, 1)
            elevation = dp(2).toFloat()
            isClickable = true
            isFocusable = true

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }

            setOnClickListener {
                val intent = Intent(this@HistoryActivity, HistoryActivity::class.java).apply {
                    putExtra("lesion_id", summary.lesionId)
                    putExtra("lesion_name", summary.lesionName)
                }
                startActivity(intent)
            }
        }

        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val nameBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val nameText = TextView(this).apply {
            text = summary.lesionName
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
        }

        val subInfoText = TextView(this).apply {
            text = if (summary.recordCount == 0) {
                "측정 기록 없음"
            } else {
                "최근 측정일: ${summary.latestDate?.let { dateFormat.format(Date(it)) } ?: "-"}"
            }
            textSize = 12f
            setTextColor(subText)
            setPadding(0, dp(4), 0, 0)
        }

        nameBox.addView(nameText)
        nameBox.addView(subInfoText)

        val arrowText = TextView(this).apply {
            text = "보기 >"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
        }

        topRow.addView(nameBox)
        topRow.addView(arrowText)

        val metricRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, 0)
        }

        val countBadge = makeBadge(
            text = "측정 ${summary.recordCount}회",
            textColor = green,
            bgColor = lightGreen
        )

        val sizeBadge = makeBadge(
            text = if (summary.latestLongestAxisMm != null) {
                "최근 최장축 ${"%.2f".format(summary.latestLongestAxisMm)} mm"
            } else {
                "첫 측정 필요"
            },
            textColor = subText,
            bgColor = Color.rgb(245, 247, 246)
        )

        val spacer = TextView(this).apply {
            text = ""
            layoutParams = LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val deleteBadge = TextView(this).apply {
            text = "삭제"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(danger)
            gravity = Gravity.CENTER
            background = roundedBg(Color.rgb(255, 240, 240), 20)
            setPadding(dp(10), dp(5), dp(10), dp(5))
            setOnClickListener {
                showDeleteLesionFromListDialog(summary)
            }
        }

        metricRow.addView(
            countBadge,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(8)
            }
        )

        metricRow.addView(
            sizeBadge,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        metricRow.addView(spacer)
        metricRow.addView(deleteBadge)

        card.addView(topRow)
        card.addView(metricRow)

        rootLayout.addView(card)
    }

    private fun setupDetailUi() {
        rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bgColor)
            setPadding(dp(20), dp(24), dp(20), dp(28))
        }

        val topRow = LinearLayout(this).apply {
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

        val titleTextView = TextView(this).apply {
            text = selectedLesionName ?: "병변 상세 기록"
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
        }

        val subtitleTextView = TextView(this).apply {
            text = "측정 기록과 최장축 변화를 확인합니다."
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

        topRow.addView(titleBox)
        topRow.addView(backTextView)

        rootLayout.addView(topRow)

        val scrollView = ScrollView(this).apply {
            addView(rootLayout)
        }

        setContentView(scrollView)
    }

    private fun loadSelectedLesionRecords(lesionId: Long) {
        Thread {
            val recordsAsc = db.lesionMeasurementDao().getByLesionIdAsc(lesionId)
            val recordsDesc = db.lesionMeasurementDao().getByLesionIdDesc(lesionId)

            runOnUiThread {
                renderSelectedLesionRecords(recordsAsc, recordsDesc)
            }
        }.start()
    }

    private fun renderSelectedLesionRecords(
        recordsAsc: List<LesionMeasurementEntity>,
        recordsDesc: List<LesionMeasurementEntity>
    ) {
        val addMeasureButton = Button(this).apply {
            text = "+ 이 병변 추가 측정"
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = roundedBg(green, 14)
            setPadding(0, dp(10), 0, dp(10))
            setOnClickListener {
                val latest = recordsAsc.lastOrNull()

                startMeasurement(
                    lesionId = selectedLesionId,
                    lesionName = selectedLesionName ?: "이름 없는 병변",
                    predClass = latest?.predClass ?: "직접 추적",
                    melanomaProb = latest?.melanomaProb ?: -1f
                )
            }
        }

        rootLayout.addView(
            addMeasureButton,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(52)
            ).apply {
                topMargin = dp(20)
                bottomMargin = dp(14)
            }
        )

        if (recordsAsc.isEmpty()) {
            val emptyCard = makeCard().apply {
                addView(TextView(this@HistoryActivity).apply {
                    text = "아직 측정 기록이 없습니다"
                    textSize = 18f
                    typeface = Typeface.DEFAULT_BOLD
                    setTextColor(darkText)
                })

                addView(TextView(this@HistoryActivity).apply {
                    text = "[이 병변 추가 측정]을 눌러 병변과 100원 동전이 함께 보이는 사진으로 첫 측정을 진행하세요."
                    textSize = 13f
                    setTextColor(subText)
                    setPadding(0, dp(8), 0, 0)
                })
            }

            rootLayout.addView(emptyCard)
            addDeleteButtonAtBottom()
            return
        }

        val latest = recordsAsc.last()
        val first = recordsAsc.first()
        val totalChange = latest.lesionLongestAxisMm - first.lesionLongestAxisMm

        addLatestSummaryCard(
            latest = latest,
            first = first,
            totalChange = totalChange,
            totalCount = recordsAsc.size
        )

        addGraphCard(recordsAsc)

        val listTitleTextView = TextView(this).apply {
            text = "측정 기록"
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
            setPadding(0, dp(10), 0, dp(12))
        }

        rootLayout.addView(listTitleTextView)

        recordsAsc.forEachIndexed { index, record ->
            val previousOlder = recordsAsc
                .filter { it.createdAt < record.createdAt }
                .maxByOrNull { it.createdAt }

            val changeText = if (previousOlder != null) {
                val diff = record.lesionLongestAxisMm - previousOlder.lesionLongestAxisMm
                "이전 대비 ${formatSigned(diff)} mm"
            } else {
                "초기 측정"
            }

            addMeasurementRecordCard(
                index = index,
                record = record,
                changeText = changeText
            )
        }

        addDeleteButtonAtBottom()
    }

    private fun addLatestSummaryCard(
        latest: LesionMeasurementEntity,
        first: LesionMeasurementEntity,
        totalChange: Double,
        totalCount: Int
    ) {
        val summaryCard = makeCard().apply {
            addView(TextView(this@HistoryActivity).apply {
                text = "최근 측정 요약"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            addView(TextView(this@HistoryActivity).apply {
                text = shortDateFormat.format(Date(latest.createdAt))
                textSize = 12f
                setTextColor(subText)
                setPadding(0, dp(4), 0, dp(14))
            })
        }

        val metricRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val latestSizeCard = makeMiniMetricCard(
            title = "최근 최장축",
            value = "${"%.2f".format(latest.lesionLongestAxisMm)} mm",
            valueColor = green
        )

        val totalChangeCard = makeMiniMetricCard(
            title = if (totalCount >= 2) "초기 대비" else "상태",
            value = if (totalCount >= 2) "${formatSigned(totalChange)} mm" else "초기 측정",
            valueColor = when {
                totalCount < 2 -> subText
                totalChange > 0 -> warning
                totalChange < 0 -> blue
                else -> green
            }
        )

        metricRow.addView(
            latestSizeCard,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dp(6)
            }
        )

        metricRow.addView(
            totalChangeCard,
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(6)
            }
        )

        summaryCard.addView(metricRow)

        val infoText = TextView(this).apply {
            text = buildString {
                appendLine("총 측정 횟수: ${totalCount}회")

                if (latest.melanomaProb >= 0f) {
                    appendLine("AI 결과: ${latest.predClass ?: "unknown"}")
                    appendLine("melanoma 확률: ${"%.4f".format(latest.melanomaProb)}")
                } else {
                    appendLine("분석 방식: 직접 추적")
                }
            }
            textSize = 13f
            setTextColor(subText)
            setPadding(0, dp(14), 0, 0)
        }

        summaryCard.addView(infoText)

        rootLayout.addView(summaryCard)
    }

    private fun addGraphCard(recordsAsc: List<LesionMeasurementEntity>) {
        val graphCard = makeCard().apply {
            addView(TextView(this@HistoryActivity).apply {
                text = "최장축 변화 그래프"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
                setPadding(0, 0, 0, dp(10))
            })

            val graphView = LongestAxisGraphView(this@HistoryActivity).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(280)
                )
                setRecords(recordsAsc)
            }

            addView(graphView)
        }

        rootLayout.addView(graphCard)
    }

    private fun addMeasurementRecordCard(
        index: Int,
        record: LesionMeasurementEntity,
        changeText: String
    ) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(16), dp(16), dp(16))
            background = roundedBg(cardColor, 18, borderColor, 1)
            elevation = dp(2).toFloat()
            isClickable = true
            isFocusable = true

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dp(12)
            }

            setOnClickListener {
                val intent = Intent(
                    this@HistoryActivity,
                    MeasurementDetailActivity::class.java
                ).apply {
                    putExtra("measurement_id", record.id)
                }

                startActivity(intent)
            }
        }

        val topRow = LinearLayout(this).apply {
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

        val titleText = TextView(this).apply {
            text = "${index + 1}회차 측정"
            textSize = 17f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
        }

        val dateText = TextView(this).apply {
            text = dateFormat.format(Date(record.createdAt))
            textSize = 12f
            setTextColor(subText)
            setPadding(0, dp(4), 0, 0)
        }

        titleBox.addView(titleText)
        titleBox.addView(dateText)

        val detailText = TextView(this).apply {
            text = "상세 >"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
        }

        topRow.addView(titleBox)
        topRow.addView(detailText)

        val metricRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(0, dp(14), 0, 0)
        }

        val sizeBadge = makeBadge(
            text = "최장축 ${"%.2f".format(record.lesionLongestAxisMm)} mm",
            textColor = green,
            bgColor = lightGreen
        )

        val changeBadge = makeBadge(
            text = changeText,
            textColor = when {
                changeText.contains("+") -> warning
                changeText.contains("-") -> blue
                else -> subText
            },
            bgColor = Color.rgb(245, 247, 246)
        )

        metricRow.addView(
            sizeBadge,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = dp(8)
            }
        )

        metricRow.addView(
            changeBadge,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        val aiText = TextView(this).apply {
            text = if (record.melanomaProb >= 0f) {
                "AI 결과: ${record.predClass ?: "unknown"} · melanoma ${"%.4f".format(record.melanomaProb)}"
            } else {
                "분석 방식: 직접 추적"
            }
            textSize = 12f
            setTextColor(subText)
            setPadding(0, dp(12), 0, 0)
        }

        card.addView(topRow)
        card.addView(metricRow)
        card.addView(aiText)

        rootLayout.addView(card)
    }

    private fun addDeleteButtonAtBottom() {
        val deleteButton = Button(this).apply {
            text = "병변 삭제"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = roundedBg(danger, 14)
            setPadding(0, dp(10), 0, dp(10))
            setOnClickListener {
                showDeleteLesionDialog()
            }
        }

        val helpText = TextView(this).apply {
            text = "잘못 등록했거나 더 이상 추적하지 않는 병변은 삭제할 수 있습니다."
            textSize = 12f
            setTextColor(subText)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
        }

        val deleteArea = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(14), 0, dp(24))

            addView(
                deleteButton,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(50)
                )
            )

            addView(helpText)
        }

        rootLayout.addView(deleteArea)
    }

    private fun showDeleteLesionDialog() {
        val lesionName = selectedLesionName ?: "이 병변"

        AlertDialog.Builder(this)
            .setTitle("병변 삭제")
            .setMessage(
                """
                "$lesionName" 병변을 삭제할까요?
                
                삭제하면 이 병변의 측정 기록과 그래프 기록도 함께 삭제됩니다.
                이 작업은 되돌릴 수 없습니다.
                """.trimIndent()
            )
            .setPositiveButton("삭제") { _, _ ->
                deleteCurrentLesion()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    private fun showDeleteLesionFromListDialog(summary: LesionProfileSummary) {
        AlertDialog.Builder(this)
            .setTitle("병변 삭제")
            .setMessage(
                """
            "${summary.lesionName}" 병변을 삭제할까요?
            
            이 병변에 저장된 모든 측정 기록도 함께 삭제됩니다.
            삭제 후에는 되돌릴 수 없습니다.
            """.trimIndent()
            )
            .setPositiveButton("삭제") { _, _ ->
                deleteLesionFromList(summary.lesionId)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteLesionFromList(lesionId: Long) {
        Thread {
            try {
                db.lesionMeasurementDao().deleteByLesionId(lesionId)
                db.lesionProfileDao().deleteByLesionId(lesionId)

                runOnUiThread {
                    setupProfileListUi()
                    loadLesionProfiles()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("삭제 실패")
                        .setMessage(e.message ?: "병변을 삭제하지 못했습니다.")
                        .setPositiveButton("확인", null)
                        .show()
                }
            }
        }.start()
    }
    private fun deleteCurrentLesion() {
        val lesionId = selectedLesionId

        if (lesionId <= 0L) {
            AlertDialog.Builder(this)
                .setTitle("삭제 실패")
                .setMessage("삭제할 병변 ID를 찾을 수 없습니다.")
                .setPositiveButton("확인", null)
                .show()
            return
        }

        Thread {
            try {
                db.lesionMeasurementDao().deleteByLesionId(lesionId)
                db.lesionProfileDao().deleteByLesionId(lesionId)

                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("삭제 완료")
                        .setMessage("병변과 관련 측정 기록이 삭제되었습니다.")
                        .setPositiveButton("확인") { _, _ ->
                            val intent = Intent(this@HistoryActivity, HistoryActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            startActivity(intent)
                            finish()
                        }
                        .show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    AlertDialog.Builder(this)
                        .setTitle("삭제 실패")
                        .setMessage(e.message ?: "알 수 없는 오류가 발생했습니다.")
                        .setPositiveButton("확인", null)
                        .show()
                }
            }
        }.start()
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

            addView(TextView(this@HistoryActivity).apply {
                text = title
                textSize = 12f
                setTextColor(subText)
            })

            addView(TextView(this@HistoryActivity).apply {
                text = value
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(valueColor)
                setPadding(0, dp(5), 0, 0)
            })
        }
    }

    private fun makeBadge(
        text: String,
        textColor: Int,
        bgColor: Int
    ): TextView {
        return TextView(this).apply {
            this.text = text
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(textColor)
            gravity = Gravity.CENTER
            background = roundedBg(bgColor, 20)
            setPadding(dp(10), dp(5), dp(10), dp(5))
        }
    }

    private fun startMeasurement(
        lesionId: Long,
        lesionName: String,
        predClass: String,
        melanomaProb: Float
    ) {
        if (lesionId <= 0L) return

        val intent = Intent(this, TrackingActivity::class.java).apply {
            putExtra("lesion_id", lesionId)
            putExtra("lesion_name", lesionName)
            putExtra("pred_class", predClass)
            putExtra("melanoma_prob", melanomaProb)
        }

        startActivity(intent)
    }

    private fun formatSigned(value: Double): String {
        return if (value >= 0) {
            "+${"%.2f".format(value)}"
        } else {
            "%.2f".format(value)
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

    data class LesionProfileSummary(
        val lesionId: Long,
        val lesionName: String,
        val recordCount: Int,
        val latestLongestAxisMm: Double?,
        val latestDate: Long?
    )
}

class LongestAxisGraphView(context: Context) : View(context) {

    private var records: List<LesionMeasurementEntity> = emptyList()

    private val axisPaint = Paint().apply {
        color = Color.rgb(190, 200, 195)
        strokeWidth = 3f
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = Color.rgb(232, 238, 234)
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val linePaint = Paint().apply {
        color = Color.rgb(67, 153, 75)
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val pointPaint = Paint().apply {
        color = Color.rgb(67, 153, 75)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val pointStrokePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.rgb(70, 80, 85)
        textSize = 28f
        isAntiAlias = true
    }

    private val smallTextPaint = Paint().apply {
        color = Color.rgb(110, 120, 125)
        textSize = 24f
        isAntiAlias = true
    }

    fun setRecords(newRecords: List<LesionMeasurementEntity>) {
        records = newRecords
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()

        val leftPad = 78f
        val rightPad = 36f
        val topPad = 36f
        val bottomPad = 68f

        val chartLeft = leftPad
        val chartRight = w - rightPad
        val chartTop = topPad
        val chartBottom = h - bottomPad

        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, axisPaint)
        canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, axisPaint)

        val gridCount = 3
        for (i in 1..gridCount) {
            val y = chartTop + i * (chartBottom - chartTop) / (gridCount + 1)
            canvas.drawLine(chartLeft, y, chartRight, y, gridPaint)
        }

        if (records.isEmpty()) {
            canvas.drawText("저장된 기록이 없습니다.", chartLeft + 20f, chartTop + 110f, textPaint)
            return
        }

        if (records.size == 1) {
            val value = records[0].lesionLongestAxisMm
            val cx = (chartLeft + chartRight) / 2f
            val cy = (chartTop + chartBottom) / 2f

            canvas.drawCircle(cx, cy, 12f, pointPaint)
            canvas.drawCircle(cx, cy, 12f, pointStrokePaint)
            canvas.drawText("${"%.2f".format(value)} mm", cx - 62f, cy - 22f, textPaint)
            canvas.drawText("기록 1개", chartLeft, chartBottom + 42f, smallTextPaint)
            return
        }

        val values = records.map { it.lesionLongestAxisMm }

        var minValue = values.minOrNull() ?: 0.0
        var maxValue = values.maxOrNull() ?: 1.0

        if (minValue == maxValue) {
            minValue -= 1.0
            maxValue += 1.0
        }

        val paddingValue = (maxValue - minValue) * 0.12
        minValue -= paddingValue
        maxValue += paddingValue

        val points = records.mapIndexed { index, record ->
            val xRatio = index.toFloat() / (records.size - 1).toFloat()
            val x = chartLeft + xRatio * (chartRight - chartLeft)

            val yRatio = ((record.lesionLongestAxisMm - minValue) / (maxValue - minValue)).toFloat()
            val y = chartBottom - yRatio * (chartBottom - chartTop)

            Pair(x, y)
        }

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]
            canvas.drawLine(p1.first, p1.second, p2.first, p2.second, linePaint)
        }

        points.forEachIndexed { index, point ->
            canvas.drawCircle(point.first, point.second, 12f, pointPaint)
            canvas.drawCircle(point.first, point.second, 12f, pointStrokePaint)

            val label = "${"%.2f".format(records[index].lesionLongestAxisMm)}"
            canvas.drawText(label, point.first - 30f, point.second - 22f, smallTextPaint)
        }

        canvas.drawText("${"%.2f".format(maxValue)}", 8f, chartTop + 10f, smallTextPaint)
        canvas.drawText("${"%.2f".format(minValue)}", 8f, chartBottom, smallTextPaint)
        canvas.drawText("측정 횟수: ${records.size}", chartLeft, chartBottom + 42f, smallTextPaint)
    }
}