package com.example.melanoscan

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
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

class LesionSelectActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var rootLayout: LinearLayout
    private lateinit var listLayout: LinearLayout

    private var predClass: String? = null
    private var melanomaProb: Float = -1f

    private val bgColor = Color.rgb(246, 250, 247)
    private val cardColor = Color.WHITE
    private val green = Color.rgb(67, 153, 75)
    private val lightGreen = Color.rgb(237, 248, 241)
    private val darkText = Color.rgb(35, 45, 55)
    private val subText = Color.rgb(110, 120, 125)
    private val borderColor = Color.rgb(225, 235, 228)
    private val danger = Color.rgb(235, 80, 80)
    private val lightRed = Color.rgb(255, 240, 240)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        db = AppDatabase.getDatabase(applicationContext)

        predClass = intent.getStringExtra("pred_class")
        melanomaProb = intent.getFloatExtra("melanoma_prob", -1f)

        setupUi()
        loadLesionProfiles()
    }

    private fun setupUi() {
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
            text = if (isFromAiAnalysis()) {
                "방금 분석한 병변을 새 병변으로 등록하거나 기존 병변에 이어서 추가 측정할 수 있습니다."
            } else {
                "같은 병변을 반복 측정하면 크기 변화를 기록하고 그래프로 확인할 수 있습니다."
            }
            setPadding(0, 0, 0, dp(18))
        }

        val newLesionCard = makeCard().apply {
            addView(TextView(this@LesionSelectActivity).apply {
                text = "새 병변 등록"
                textSize = 18f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
            })

            addView(TextView(this@LesionSelectActivity).apply {
                text = if (isFromAiAnalysis()) {
                    "방금 분석한 병변을 새 병변으로 등록하고 크기 측정을 이어서\n진행합니다. 병변을 3개월 이상 지속 관찰하는 것을 추천합니다.\n예: 왼쪽 팔 점, 목 뒤 점, 오른쪽 볼 점"
                } else {
                    "처음 관찰하는 병변이라면 이름을 정해 등록하세요.\n예: 왼쪽 팔 점, 목 뒤 점, 오른쪽 볼 점"
                }
                textSize = 13f
                setTextColor(subText)
                setPadding(0, dp(8), 0, dp(14))
            })

            val newLesionButton = Button(this@LesionSelectActivity).apply {
                text = "+ 새 병변 등록"
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

        listLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        rootLayout.addView(titleTextView)
        rootLayout.addView(guideTextView)
        rootLayout.addView(newLesionCard)
        rootLayout.addView(listTitleRow)
        rootLayout.addView(listLayout)

        val scrollView = ScrollView(this).apply {
            addView(rootLayout)
        }

        setContentView(scrollView)
    }

    private fun loadLesionProfiles() {
        Thread {
            val profiles = db.lesionProfileDao().getAll()

            runOnUiThread {
                listLayout.removeAllViews()

                if (profiles.isEmpty()) {
                    addEmptyCard()
                    return@runOnUiThread
                }

                profiles.forEach { profile ->
                    addProfileCard(profile)
                }
            }
        }.start()
    }

    private fun addEmptyCard() {
        val emptyCard = makeCard().apply {
            gravity = Gravity.CENTER

            addView(TextView(this@LesionSelectActivity).apply {
                text = "등록된 병변이 없습니다"
                textSize = 17f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(darkText)
                gravity = Gravity.CENTER
            })

            addView(TextView(this@LesionSelectActivity).apply {
                text = "새 병변을 등록하면 이곳에 리스트로 표시됩니다."
                textSize = 13f
                setTextColor(subText)
                gravity = Gravity.CENTER
                setPadding(0, dp(8), 0, 0)
            })
        }

        listLayout.addView(emptyCard)
    }

    private fun addProfileCard(profile: LesionProfileEntity) {
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
                if (isFromAiAnalysis()) {
                    showAddToExistingLesionDialog(profile)
                } else {
                    openLesionHistoryActivity(profile.lesionId, profile.lesionName)
                }
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
            text = profile.lesionName
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(darkText)
        }

        val createdText = TextView(this).apply {
            text = "등록일: ${formatDate(profile.createdAt)}"
            textSize = 12f
            setTextColor(subText)
            setPadding(0, dp(4), 0, 0)
        }

        nameBox.addView(nameText)
        nameBox.addView(createdText)

        val arrowText = TextView(this).apply {
            text = if (isFromAiAnalysis()) "추가 >" else "보기 >"
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            gravity = Gravity.CENTER
        }

        topRow.addView(nameBox)
        topRow.addView(arrowText)

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(14), 0, 0)
        }

        val actionBadge = TextView(this).apply {
            text = if (isFromAiAnalysis()) "해당 병변에 추가" else "기록 보기"
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(green)
            gravity = Gravity.CENTER
            background = roundedBg(lightGreen, 20)
            setPadding(dp(10), dp(5), dp(10), dp(5))
        }

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
            background = roundedBg(lightRed, 20)
            setPadding(dp(10), dp(5), dp(10), dp(5))
            setOnClickListener {
                showDeleteLesionDialog(profile)
            }
        }

        actionRow.addView(actionBadge)
        actionRow.addView(spacer)
        actionRow.addView(deleteBadge)

        card.addView(topRow)
        card.addView(actionRow)

        listLayout.addView(card)
    }

    private fun showDeleteLesionDialog(profile: LesionProfileEntity) {
        AlertDialog.Builder(this)
            .setTitle("병변 삭제")
            .setMessage(
                """
                "${profile.lesionName}" 병변을 삭제할까요?
                
                이 병변에 저장된 모든 측정 기록도 함께 삭제됩니다.
                삭제 후에는 되돌릴 수 없습니다.
                """.trimIndent()
            )
            .setPositiveButton("삭제") { _, _ ->
                deleteLesion(profile.lesionId)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteLesion(lesionId: Long) {
        Thread {
            try {
                db.lesionMeasurementDao().deleteByLesionId(lesionId)
                db.lesionProfileDao().deleteByLesionId(lesionId)

                runOnUiThread {
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

    private fun showAddToExistingLesionDialog(profile: LesionProfileEntity) {
        val analyzedClass = predClass ?: "unknown"
        val probText = if (melanomaProb >= 0f) {
            "melanoma 확률: ${"%.4f".format(melanomaProb)}"
        } else {
            "AI 확률 정보 없음"
        }

        AlertDialog.Builder(this)
            .setTitle("기존 병변에 추가")
            .setMessage(
                """
                방금 분석한 병변을 "${profile.lesionName}" 기록에 추가하시겠습니까?
                
                이전 분석 결과
                예측 클래스: $analyzedClass
                $probText
                
                확인을 누르면 이 병변의 추가 측정 화면으로 이동합니다.
                병변과 100원 동전을 함께 촬영하면 해당 병변 기록에 저장됩니다.
                """.trimIndent()
            )
            .setPositiveButton("추가하기") { _, _ ->
                startTrackingActivity(profile.lesionId, profile.lesionName)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun getSourceLabel(): String {
        return if (melanomaProb >= 0f) {
            "AI 분석 후 등록"
        } else {
            "직접 추적"
        }
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

                createNewLesionProfile(name)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun createNewLesionProfile(name: String) {
        Thread {
            val profile = LesionProfileEntity(
                lesionName = name,
                createdAt = System.currentTimeMillis()
            )

            val lesionId = db.lesionProfileDao().insert(profile)

            runOnUiThread {
                startTrackingActivity(lesionId, name)
            }
        }.start()
    }

    private fun openLesionHistoryActivity(lesionId: Long, lesionName: String) {
        val intent = Intent(this, HistoryActivity::class.java).apply {
            putExtra("lesion_id", lesionId)
            putExtra("lesion_name", lesionName)
        }

        startActivity(intent)
    }

    private fun startTrackingActivity(lesionId: Long, lesionName: String) {
        val intent = Intent(this, TrackingActivity::class.java).apply {
            putExtra("lesion_id", lesionId)
            putExtra("lesion_name", lesionName)
            putExtra("pred_class", predClass ?: "직접 추적")
            putExtra("melanoma_prob", melanomaProb)
        }

        startActivity(intent)
    }

    private fun isFromAiAnalysis(): Boolean {
        return melanomaProb >= 0f || !predClass.isNullOrBlank()
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

    private fun formatDate(timestamp: Long): String {
        return try {
            val formatter = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            formatter.format(Date(timestamp))
        } catch (e: Exception) {
            "-"
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}