package com.example.recipeproject

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.recipeproject.db.RecipeEntity

class ChosungListAdapter(
    private var items: List<RecipeEntity>,
    private val onItemClick: (RecipeEntity) -> Unit,
    private val onItemLongClick: (RecipeEntity) -> Unit
) : RecyclerView.Adapter<ChosungListAdapter.VH>() {

    private val selectedIds = mutableSetOf<Long>()
    var selectionMode: Boolean = false
        private set

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBadge: TextView = itemView.findViewById(R.id.tvBadge)
        val tvName: TextView = itemView.findViewById(R.id.tvName)
        val tvSub: TextView = itemView.findViewById(R.id.tvSub)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe_card, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvName.text = item.name
        holder.tvBadge.text = item.name.firstOrNull()?.toString() ?: "?"

        val hasIce = item.iceSteps.isNotBlank() || item.iceToppings.isNotBlank()
        val hasHot = item.hotSteps.isNotBlank() || item.hotToppings.isNotBlank()
        holder.tvSub.text = when {
            hasIce && hasHot -> "ICE / HOT"
            hasIce -> "ICE"
            hasHot -> "HOT"
            else -> "내용 없음"
        }

        // ✅ 선택 UI (간단 표시)
        val selected = selectedIds.contains(item.id)
        holder.itemView.alpha = if (selected) 0.6f else 1.0f

        holder.itemView.setOnClickListener {
            if (selectionMode) {
                toggleSelection(item.id)
                notifyItemChanged(position)
            } else {
                onItemClick(item)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!selectionMode) {
                selectionMode = true
                selectedIds.clear()
                selectedIds.add(item.id)
                notifyDataSetChanged()
                onItemLongClick(item)
            }
            true
        }
    }

    override fun getItemCount() = items.size

    fun submitList(newItems: List<RecipeEntity>) {
        items = newItems

        // DB 변화로 항목이 사라졌으면 선택 목록도 정리
        val alive = newItems.map { it.id }.toSet()
        selectedIds.retainAll(alive)

        notifyDataSetChanged()
    }

    private fun toggleSelection(id: Long) {
        if (selectedIds.contains(id)) selectedIds.remove(id) else selectedIds.add(id)
        // 선택이 0개면 선택모드 해제
        if (selectedIds.isEmpty()) {
            selectionMode = false
        }
    }

    fun getSelectedIds(): List<Long> = selectedIds.toList()

    fun exitSelectionMode() {
        selectionMode = false
        selectedIds.clear()
        notifyDataSetChanged()
    }
}