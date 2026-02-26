package com.example.recipeproject

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.helper.widget.Flow
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.recipeproject.db.AppDatabase
import com.example.recipeproject.db.RecipeEntity
import com.example.recipeproject.model.RecipeDraft
import com.example.recipeproject.util.KoreanUtil
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private enum class Tab { ICE, HOT }

    private val CHOSUNG = listOf("ㄱ","ㄴ","ㄷ","ㄹ","ㅁ","ㅂ","ㅅ","ㅇ","ㅈ","ㅊ","ㅋ","ㅌ","ㅍ","ㅎ")

    // ✅ 엑셀 가져오기 (파일 선택)
    private val pickExcelLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {}
            importExcelToRoom(uri)
        }

    // ✅ 엑셀 내보내기 (저장 위치 선택)
    private val createExcelLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            )
        ) { uri: Uri? ->
            uri ?: return@registerForActivityResult
            exportRoomToExcel(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setSupportActionBar(findViewById<MaterialToolbar>(R.id.toolbar))

        setupChosungButtons()

        findViewById<FloatingActionButton>(R.id.fabAdd).setOnClickListener {
            showAddRecipeDialog()
        }
    }

    // -----------------------
    // Toolbar Menu
    // -----------------------
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.action_import_excel -> {
                pickExcelLauncher.launch(
                    arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                )
                true
            }

            R.id.action_export_excel -> {
                val ts = SimpleDateFormat("yyyyMMdd_HHmm", Locale.KOREA).format(Date())
                createExcelLauncher.launch("mega_recipe_export_$ts.xlsx")
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    // -----------------------
    // 초성 버튼 생성
    // -----------------------
    private fun setupChosungButtons() {
        val parent = findViewById<ConstraintLayout>(R.id.choseongBox)
        val flow = findViewById<Flow>(R.id.flowChosung)
        val ids = mutableListOf<Int>()

        CHOSUNG.forEach { ch ->
            val btn = MaterialButton(this).apply {
                id = View.generateViewId()
                text = ch
                textSize = 20f
                isAllCaps = false
                layoutParams = ConstraintLayout.LayoutParams(dp(110), dp(64))

                setBackgroundResource(R.drawable.bg_chosung)
                backgroundTintList = null
                setTextColor(ContextCompat.getColor(this@MainActivity, R.color.mega_black))

                setOnClickListener {
                    ids.forEach { id -> parent.findViewById<View>(id)?.isSelected = false }
                    isSelected = true

                    val intent = Intent(this@MainActivity, ChosungListActivity::class.java)
                    intent.putExtra(ChosungListActivity.EXTRA_CHOSUNG, ch)
                    startActivity(intent)
                }
            }

            parent.addView(btn)
            ids.add(btn.id)
        }

        flow.referencedIds = ids.toIntArray()
    }

    // -----------------------
    // 레시피 추가 모달
    // -----------------------
    private fun showAddRecipeDialog() {
        val v = layoutInflater.inflate(R.layout.dialog_add_recipe, null)

        val etName = v.findViewById<TextInputEditText>(R.id.etName)
        val toggle = v.findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        val btnIce = v.findViewById<MaterialButton>(R.id.btnIce)
        val btnHot = v.findViewById<MaterialButton>(R.id.btnHot)
        val etSteps = v.findViewById<TextInputEditText>(R.id.etSteps)
        val etToppings = v.findViewById<TextInputEditText>(R.id.etToppings)

        val draft = RecipeDraft()
        var currentTab = Tab.ICE

        fun saveTab(tab: Tab) {
            val steps = etSteps.text?.toString().orEmpty()
            val toppings = etToppings.text?.toString().orEmpty()
            if (tab == Tab.ICE) {
                draft.ice.steps = steps
                draft.ice.toppings = toppings
            } else {
                draft.hot.steps = steps
                draft.hot.toppings = toppings
            }
        }

        fun loadTab(tab: Tab) {
            val variant = if (tab == Tab.ICE) draft.ice else draft.hot
            etSteps.setText(variant.steps)
            etToppings.setText(variant.toppings)
        }

        toggle.check(btnIce.id)
        currentTab = Tab.ICE
        loadTab(Tab.ICE)

        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            saveTab(currentTab)
            currentTab = if (checkedId == btnIce.id) Tab.ICE else Tab.HOT
            loadTab(currentTab)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(v)
            .create()

        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)

        v.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }

        v.findViewById<View>(R.id.btnSave).setOnClickListener {
            saveTab(currentTab)

            val name = etName.text?.toString()?.trim().orEmpty()
            if (name.isBlank()) {
                etName.error = "메뉴 이름을 입력해줘"
                return@setOnClickListener
            }

            val chosung = KoreanUtil.getChosung(name[0])

            val entity = RecipeEntity(
                name = name,
                chosung = chosung,
                iceSteps = draft.ice.steps,
                iceToppings = draft.ice.toppings,
                iceMemo = "",
                hotSteps = draft.hot.steps,
                hotToppings = draft.hot.toppings,
                hotMemo = "",
                memo = "" // 호환용(안씀)
            )

            lifecycleScope.launch(Dispatchers.IO) {
                AppDatabase.getInstance(this@MainActivity).recipeDao().upsertAll(listOf(entity))
            }

            dialog.dismiss()
        }

        dialog.show()
    }

    // -----------------------
    // 엑셀 Import → Room 업서트
    // -----------------------
    private fun importExcelToRoom(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(this@MainActivity).recipeDao()
            val formatter = DataFormatter()
            val items = mutableListOf<RecipeEntity>()

            contentResolver.openInputStream(uri).use { input ->
                if (input == null) return@launch

                val wb = XSSFWorkbook(input)
                val sheet = wb.getSheetAt(0)

                val headerRow = sheet.getRow(0) ?: run {
                    wb.close()
                    return@launch
                }

                val headerMap = mutableMapOf<String, Int>()
                for (c in 0 until headerRow.lastCellNum) {
                    val key = formatter.formatCellValue(headerRow.getCell(c)).trim()
                    if (key.isNotEmpty()) headerMap[key] = c
                }

                fun cell(rowIdx: Int, col: String): String {
                    val colIdx = headerMap[col] ?: return ""
                    val row = sheet.getRow(rowIdx) ?: return ""
                    return formatter.formatCellValue(row.getCell(colIdx)).trim()
                }

                for (r in 1..sheet.lastRowNum) {
                    val name = cell(r, "제품명")
                    if (name.isBlank()) continue

                    val iceSteps = cell(r, "제조순서(ICE)")
                    val hotSteps = cell(r, "제조순서(HOT)")
                    val iceToppings = cell(r, "토핑+재료(ICE)")
                    val hotToppings = cell(r, "토핑+재료(HOT)")
                    val iceMemo = cell(r, "메모(ICE)")
                    val hotMemo = cell(r, "메모(HOT)")

                    val chosung = KoreanUtil.getChosung(name[0])

                    items.add(
                        RecipeEntity(
                            name = name,
                            chosung = chosung,
                            iceSteps = iceSteps,
                            iceToppings = iceToppings,
                            iceMemo = iceMemo,
                            hotSteps = hotSteps,
                            hotToppings = hotToppings,
                            hotMemo = hotMemo,
                            memo = "" // 호환용
                        )
                    )
                }

                wb.close()
            }

            if (items.isNotEmpty()) {
                dao.upsertAll(items)
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@MainActivity,
                    "엑셀 반영 완료: ${items.size}개",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // -----------------------
    // Room → 엑셀 Export
    // -----------------------
    private fun exportRoomToExcel(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            val dao = AppDatabase.getInstance(this@MainActivity).recipeDao()
            val list = dao.getAll()

            val wb = XSSFWorkbook()
            val sheet = wb.createSheet("recipes")

            val headers = listOf(
                "제품명",
                "제조순서(ICE)",
                "제조순서(HOT)",
                "토핑+재료(ICE)",
                "토핑+재료(HOT)",
                "메모(ICE)",
                "메모(HOT)"
            )

            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { i, h ->
                headerRow.createCell(i).setCellValue(h)
            }

            list.forEachIndexed { idx, item ->
                val r = sheet.createRow(idx + 1)
                r.createCell(0).setCellValue(item.name)
                r.createCell(1).setCellValue(item.iceSteps)
                r.createCell(2).setCellValue(item.hotSteps)
                r.createCell(3).setCellValue(item.iceToppings)
                r.createCell(4).setCellValue(item.hotToppings)
                r.createCell(5).setCellValue(item.iceMemo)
                r.createCell(6).setCellValue(item.hotMemo)
            }

            val widths = intArrayOf(24, 42, 42, 30, 30, 28, 28)
            for (i in widths.indices) {
                sheet.setColumnWidth(i, widths[i] * 256)
            }

            contentResolver.openOutputStream(uri)?.use { out ->
                wb.write(out)
                out.flush()
            }
            wb.close()

            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "엑셀 내보내기 완료!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}