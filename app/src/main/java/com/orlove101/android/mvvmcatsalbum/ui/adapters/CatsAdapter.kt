package com.orlove101.android.mvvmcatsalbum.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.orlove101.android.mvvmcatsalbum.data.models.CatsResponseItem
import com.orlove101.android.mvvmcatsalbum.databinding.ItemCatPreviewBinding

class CatsAdapter: RecyclerView.Adapter<CatsAdapter.CatViewHolder>() {

    inner class CatViewHolder(private val binding: ItemCatPreviewBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CatsResponseItem) {
            binding.apply {
                Glide.with(root).load(item.url).into(ivArticleImage)
                ivArticleImage.setOnClickListener {
                    onImageClickListener?.let {
                        it(ivArticleImage, item.url)
                    }
                }
            }
        }
    }

    private val differCallback = object : DiffUtil.ItemCallback<CatsResponseItem>() {
        override fun areItemsTheSame(oldItem: CatsResponseItem, newItem: CatsResponseItem): Boolean {
            return oldItem.url == newItem.url
        }

        override fun areContentsTheSame(oldItem: CatsResponseItem, newItem: CatsResponseItem): Boolean {
            return oldItem == newItem
        }
    }

    val differ = AsyncListDiffer(this, differCallback)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CatsAdapter.CatViewHolder {
        val binding = ItemCatPreviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatsAdapter.CatViewHolder, position: Int) {
        val cat = differ.currentList[position]
        holder.bind(cat)
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    private var onImageClickListener: ((View, String) -> Unit)? = null

    fun setOnImageClickListener(listener: (View, String) -> Unit) {
        onImageClickListener = listener
    }
}
