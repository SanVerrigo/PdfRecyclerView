package com.verrigo.pdfviewer

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.marginEnd
import androidx.core.view.marginStart
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.verrigo.pdfviewer.databinding.PdfPageLayoutBinding
import kotlin.math.roundToInt

class PdfAdapter : RecyclerView.Adapter<PdfAdapter.ViewHolder>() {

    var bitmapPoolRange: IntRange = IntRange.EMPTY

    var bitmapPool: MutableList<Bitmap?> = mutableListOf()

    private lateinit var placeholderBitmap: Bitmap

    private var widthInPx: Int = -1

    var isPlaceholderInitialized = false

    fun setPlaceholderBitmap(bitmap: Bitmap) {
        placeholderBitmap = bitmap
        isPlaceholderInitialized = true
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        PdfPageLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        with(holder.binding) {
            val bitmap = bitmapPool[position] ?: placeholderBitmap
            val ratio: Double =
                bitmap.width.div((widthInPx - root.marginStart - root.marginEnd) * 1.0)
            root.layoutParams.height = (bitmap.height / ratio).roundToInt()
            pageImg.layoutParams.height = (bitmap.height / ratio).roundToInt()
            if (position in bitmapPoolRange) {
                progressIndicator.isGone = true
                Glide.with(root.context)
                    .load(bitmap)
                    .into(pageImg)
            } else {
                progressIndicator.isVisible = true
                Glide.with(root.context)
                    .load(placeholderBitmap)
                    .into(pageImg)
            }
        }
    }

    override fun getItemCount(): Int = bitmapPool.size

    fun setWidth(width: Int) {
        if (width <= 0) {
            return
        }
        this.widthInPx = width
    }

    class ViewHolder(val binding: PdfPageLayoutBinding) : RecyclerView.ViewHolder(binding.root)
}
