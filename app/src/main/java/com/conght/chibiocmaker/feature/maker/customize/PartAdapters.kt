package com.conght.chibiocmaker.feature.maker.customize

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.conght.chibiocmaker.R
import com.conght.chibiocmaker.core.ui.loadImage
import com.conght.chibiocmaker.core.ui.setOnSingleClickListener
import com.conght.chibiocmaker.data.model.PartCategory
import com.conght.chibiocmaker.data.model.PartColor
import com.conght.chibiocmaker.data.model.PartItem
import com.conght.chibiocmaker.databinding.ItemCategoryTabBinding
import com.conght.chibiocmaker.databinding.ItemColorDotBinding
import com.conght.chibiocmaker.databinding.ItemPartBinding

/** Một phần tử list kèm cờ đang chọn / đang khoá (để DiffUtil vẽ lại đúng ô). */
data class Selectable<T>(val value: T, val selected: Boolean, val locked: Boolean = false)

private fun <T> diff(id: (T) -> String) = object : DiffUtil.ItemCallback<Selectable<T>>() {
    override fun areItemsTheSame(old: Selectable<T>, new: Selectable<T>) = id(old.value) == id(new.value)
    override fun areContentsTheSame(old: Selectable<T>, new: Selectable<T>) = old == new
}

/** Lưới item phụ kiện (sau ô None + ô Xúc xắc). */
class PartItemAdapter(
    private val onClick: (PartItem) -> Unit,
) : ListAdapter<Selectable<PartItem>, PartItemAdapter.ViewHolder>(diff { it.id }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemPartBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemPartBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: Selectable<PartItem>) {
            binding.ivThumbnail.loadImage(item.value.thumbnailUrl, R.color.cell_tan)
            binding.root.isSelected = item.selected
            // Ô khoá (Lucky Gift): phủ tối + icon khoá giữa ô; ô khoá đang mặc sẵn vẫn hiện chọn.
            // Vẫn bấm được: ViewModel hỏi xem quảng cáo để mở khoá (dialog Unlock Item)
            binding.vLockOverlay.isVisible = item.locked && !item.selected
            binding.ivLock.isVisible = item.locked
            binding.root.setOnSingleClickListener(intervalMs = CLICK_DEBOUNCE_MS, haptic = true) { onClick(item.value) }
        }
    }
}

/** Chấm màu của item đang chọn. */
class ColorDotAdapter(
    private val onClick: (PartColor) -> Unit,
) : ListAdapter<Selectable<PartColor>, ColorDotAdapter.ViewHolder>(diff { it.id }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemColorDotBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemColorDotBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(color: Selectable<PartColor>) {
            val tint = runCatching { color.value.hex.toColorInt() }.getOrDefault(0)
            binding.root.backgroundTintList = ColorStateList.valueOf(tint)
            binding.root.isSelected = color.selected
            binding.root.contentDescription = color.value.hex
            binding.root.setOnSingleClickListener(intervalMs = CLICK_DEBOUNCE_MS, haptic = true) { onClick(color.value) }
        }
    }
}

/** Tab category ở đáy; tab đang chọn cao 56 (nhô lên 8). */
class CategoryTabAdapter(
    private val onClick: (PartCategory) -> Unit,
) : ListAdapter<Selectable<PartCategory>, CategoryTabAdapter.ViewHolder>(diff { it.id }) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCategoryTabBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemCategoryTabBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: Selectable<PartCategory>) {
            val res = binding.root.resources
            binding.tab.isSelected = category.selected
            binding.tab.updateLayoutParams {
                height = res.getDimensionPixelSize(
                    if (category.selected) R.dimen.category_tab_selected_h else R.dimen.category_tab_h,
                )
            }
            binding.ivIcon.loadImage(category.value.iconUrl, R.color.cell_tan)
            binding.tab.contentDescription = category.value.name
            binding.tab.setOnSingleClickListener(intervalMs = CLICK_DEBOUNCE_MS, haptic = true) { onClick(category.value) }
        }
    }
}

/** Chọn item/màu/tab cần phản hồi nhanh hơn debounce mặc định của điều hướng. */
private const val CLICK_DEBOUNCE_MS = 150L
