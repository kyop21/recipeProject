package com.example.recipeproject

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.recipeproject.db.AppDatabase
import com.example.recipeproject.db.RecipeEntity
import com.example.recipeproject.util.KoreanUtil
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class RecipeDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_RECIPE_ID = "extra_recipe_id"
    }

    private enum class Tab { ICE, HOT }

    private val dao by lazy { AppDatabase.getInstance(this).recipeDao() }

    private var recipe: RecipeEntity? = null
    private var currentTab: Tab = Tab.ICE

    // memo 자동 저장 디바운스
    private var memoJob: Job? = null
    private var ignoreMemoChange = false

    // views
    private lateinit var toolbar: MaterialToolbar
    private lateinit var toggle: MaterialButtonToggleGroup
    private lateinit var btnIce: MaterialButton
    private lateinit var btnHot: MaterialButton
    private lateinit var tvName: android.widget.TextView
    private lateinit var tvSteps: android.widget.TextView
    private lateinit var tvToppings: android.widget.TextView
    private lateinit var etMemo: TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_recipe_detail)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindViews()

        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        // ✅ 탭 전환 시: (1) 현재 탭 메모 저장 (2) 새 탭 메모 로드 (3) steps/toppings 렌더
        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            // 1) 탭 바뀌기 전 현재 탭 memo를 recipe 상태에 저장 (DB는 이미 입력중 자동 저장되지만, UI 상태도 유지)
            saveMemoToLocal()

            // 2) tab 갱신
            currentTab = if (checkedId == btnIce.id) Tab.ICE else Tab.HOT

            // 3) 새 탭 memo 로드 + 렌더
            loadMemoFromLocal()
            render()
        }

        // ✅ 메모 입력 즉시 저장(디바운스 300ms) - 현재 탭(ICE/HOT)에 맞춰 저장
        etMemo.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (ignoreMemoChange) return
                val r = recipe ?: return

                val newMemo = s?.toString().orEmpty()

                // 로컬 recipe에도 반영
                recipe = if (currentTab == Tab.ICE) {
                    r.copy(iceMemo = newMemo)
                } else {
                    r.copy(hotMemo = newMemo)
                }

                // DB 저장(디바운스)
                memoJob?.cancel()
                memoJob = lifecycleScope.launch {
                    delay(300)
                    launch(Dispatchers.IO) {
                        val id = r.id
                        if (currentTab == Tab.ICE) dao.updateIceMemo(id, newMemo)
                        else dao.updateHotMemo(id, newMemo)
                    }
                }
            }
        })

        // ✅ DB에서 데이터 로드
        val id = intent.getLongExtra(EXTRA_RECIPE_ID, -1L)
        if (id == -1L) {
            finish()
            return
        }
        loadRecipe(id)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_recipe_detail, menu) // 수정/삭제 메뉴
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_home -> {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
                true
            }
            R.id.action_edit -> {
                recipe?.let { showEditRecipeDialog(it) }
                true
            }
            R.id.action_delete -> {
                recipe?.let { confirmDelete(it) }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun bindViews() {
        toolbar = findViewById(R.id.toolbar)
        toggle = findViewById(R.id.toggleGroup)
        btnIce = findViewById(R.id.btnIce)
        btnHot = findViewById(R.id.btnHot)
        tvName = findViewById(R.id.tvName)
        tvSteps = findViewById(R.id.tvSteps)
        tvToppings = findViewById(R.id.tvToppings)
        etMemo = findViewById(R.id.etMemo)
    }

    private fun loadRecipe(id: Long) {
        lifecycleScope.launch(Dispatchers.IO) {
            val data = dao.getById(id)
            withContext(Dispatchers.Main) {
                if (data == null) {
                    finish()
                    return@withContext
                }

                recipe = data
                toolbar.title = data.name
                tvName.text = data.name

                // 기본 ICE
                currentTab = Tab.ICE
                toggle.check(btnIce.id)

                // memo 복원(ICE)
                ignoreMemoChange = true
                etMemo.setText(data.iceMemo)
                ignoreMemoChange = false

                render()
            }
        }
    }

    private fun render() {
        val r = recipe ?: return

        tvName.text = r.name
        toolbar.title = r.name

        if (currentTab == Tab.ICE) {
            tvSteps.text = if (r.iceSteps.isBlank()) "내용 없음" else r.iceSteps
            tvToppings.text = if (r.iceToppings.isBlank()) "없음" else r.iceToppings
        } else {
            tvSteps.text = if (r.hotSteps.isBlank()) "내용 없음" else r.hotSteps
            tvToppings.text = if (r.hotToppings.isBlank()) "없음" else r.hotToppings
        }
    }

    /** 현재 etMemo 내용을 recipe 로컬 상태에 저장 */
    private fun saveMemoToLocal() {
        val r = recipe ?: return
        val text = etMemo.text?.toString().orEmpty()

        recipe = if (currentTab == Tab.ICE) {
            r.copy(iceMemo = text)
        } else {
            r.copy(hotMemo = text)
        }
    }

    /** currentTab 기준으로 recipe에 저장된 memo를 etMemo에 로드 */
    private fun loadMemoFromLocal() {
        val r = recipe ?: return
        val text = if (currentTab == Tab.ICE) r.iceMemo else r.hotMemo

        ignoreMemoChange = true
        etMemo.setText(text)
        ignoreMemoChange = false
    }

    // -------------------------
    // 수정 다이얼로그 (dialog_add_recipe 재사용)
    // -------------------------
    private fun showEditRecipeDialog(current: RecipeEntity) {
        val v = layoutInflater.inflate(R.layout.dialog_add_recipe, null)

        val etName = v.findViewById<TextInputEditText>(R.id.etName)
        val tg = v.findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        val dIce = v.findViewById<MaterialButton>(R.id.btnIce)
        val dHot = v.findViewById<MaterialButton>(R.id.btnHot)
        val etSteps = v.findViewById<TextInputEditText>(R.id.etSteps)
        val etToppings = v.findViewById<TextInputEditText>(R.id.etToppings)

        etName.setText(current.name)

        var iceSteps = current.iceSteps
        var iceToppings = current.iceToppings
        var hotSteps = current.hotSteps
        var hotToppings = current.hotToppings

        var tab = Tab.ICE

        fun saveTab(t: Tab) {
            val s = etSteps.text?.toString().orEmpty()
            val tp = etToppings.text?.toString().orEmpty()
            if (t == Tab.ICE) {
                iceSteps = s
                iceToppings = tp
            } else {
                hotSteps = s
                hotToppings = tp
            }
        }

        fun loadTab(t: Tab) {
            if (t == Tab.ICE) {
                etSteps.setText(iceSteps)
                etToppings.setText(iceToppings)
            } else {
                etSteps.setText(hotSteps)
                etToppings.setText(hotToppings)
            }
        }

        tg.check(dIce.id)
        tab = Tab.ICE
        loadTab(Tab.ICE)

        tg.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            saveTab(tab)
            tab = if (checkedId == dIce.id) Tab.ICE else Tab.HOT
            loadTab(tab)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(v)
            .create()

        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)

        v.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }

        v.findViewById<View>(R.id.btnSave).setOnClickListener {
            saveTab(tab)

            val newName = etName.text?.toString()?.trim().orEmpty()
            if (newName.isBlank()) {
                etName.error = "메뉴 이름을 입력해줘"
                return@setOnClickListener
            }

            val newChosung = KoreanUtil.getChosung(newName[0])

            // ✅ memo는 ICE/HOT 각각 유지
            val updated = current.copy(
                name = newName,
                chosung = newChosung,
                iceSteps = iceSteps,
                iceToppings = iceToppings,
                hotSteps = hotSteps,
                hotToppings = hotToppings
                // iceMemo/hotMemo는 current에 있던 값 그대로 유지됨
            )

            lifecycleScope.launch(Dispatchers.IO) {
                dao.update(updated)
                val refreshed = dao.getById(updated.id)
                withContext(Dispatchers.Main) {
                    if (refreshed != null) {
                        recipe = refreshed
                        // 현재 탭 기준 memo 다시 로드
                        loadMemoFromLocal()
                        render()
                    }
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    // -------------------------
    // 삭제
    // -------------------------
    private fun confirmDelete(current: RecipeEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("삭제할까?")
            .setMessage("‘${current.name}’ 레시피를 삭제하면 복구할 수 없어.")
            .setNegativeButton("취소", null)
            .setPositiveButton("삭제") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    dao.deleteById(current.id)
                    withContext(Dispatchers.Main) { finish() }
                }
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        memoJob?.cancel()
    }
}