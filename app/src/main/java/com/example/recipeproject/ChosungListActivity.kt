package com.example.recipeproject

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.recipeproject.db.AppDatabase
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChosungListActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CHOSUNG = "extra_chosung"
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var adapter: ChosungListAdapter

    private var chosung: String = "?"
    private var deleteMenuVisible: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_chosung_list)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        chosung = intent.getStringExtra(EXTRA_CHOSUNG) ?: "?"

        toolbar = findViewById(R.id.toolbar)
        toolbar.title = "'$chosung' 목록"
        setSupportActionBar(toolbar)

        toolbar.setNavigationOnClickListener {
            // 선택 모드면 종료, 아니면 뒤로
            if (::adapter.isInitialized && adapter.selectionMode) {
                adapter.exitSelectionMode()
                setDeleteMenu(false)
            } else {
                finish()
            }
        }

        val rv = findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvList)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = ChosungListAdapter(
            emptyList(),
            onItemClick = { item ->
                val intent = Intent(this, RecipeDetailActivity::class.java)
                intent.putExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, item.id)
                startActivity(intent)
            },
            onItemLongClick = {
                // 선택 모드 진입 시 삭제 메뉴 보이기
                setDeleteMenu(true)
            }
        )
        rv.adapter = adapter

        val dao = AppDatabase.getInstance(this).recipeDao()

        // --- 여기서부터 대체 코드 시작 ---
        // 1. 넘어온 초성(chosung 변수)에 매칭되는 쌍자음을 찾아 배열(List)로 묶어줍니다.
        val searchTargets = when (chosung) {
            "ㄱ" -> listOf("ㄱ", "ㄲ")
            "ㄷ" -> listOf("ㄷ", "ㄸ")
            "ㅂ" -> listOf("ㅂ", "ㅃ")
            "ㅅ" -> listOf("ㅅ", "ㅆ")
            "ㅈ" -> listOf("ㅈ", "ㅉ")
            else -> listOf(chosung) // 쌍자음이 없는 글자는 자기 자신만 배열에 넣음
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 2. 아까 RecipeDao.kt에서 수정한 새 함수 'observeByChosungs'를 호출하고 searchTargets를 넘깁니다.
                dao.observeByChosungs(searchTargets).collect { list ->
                    adapter.submitList(list)
                    // 선택모드가 자동 해제되면 메뉴도 숨김
                    if (!adapter.selectionMode) setDeleteMenu(false)
                }
            }
        }
        // --- 여기까지 ---
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chosung_list, menu)
        menu.findItem(R.id.action_delete)?.isVisible = deleteMenuVisible
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_delete)?.isVisible = deleteMenuVisible
        return super.onPrepareOptionsMenu(menu)
    }

    private fun setDeleteMenu(visible: Boolean) {
        deleteMenuVisible = visible
        invalidateOptionsMenu()
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
            R.id.action_delete -> {
                val ids = adapter.getSelectedIds()
                if (ids.isEmpty()) return true

                MaterialAlertDialogBuilder(this)
                    .setTitle("삭제할까?")
                    .setMessage("${ids.size}개 레시피를 삭제하면 복구할 수 없어.")
                    .setNegativeButton("취소", null)
                    .setPositiveButton("삭제") { _, _ ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            AppDatabase.getInstance(this@ChosungListActivity)
                                .recipeDao()
                                .deleteByIds(ids)

                            withContext(Dispatchers.Main) {
                                adapter.exitSelectionMode()
                                setDeleteMenu(false)
                            }
                        }
                    }
                    .show()

                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}