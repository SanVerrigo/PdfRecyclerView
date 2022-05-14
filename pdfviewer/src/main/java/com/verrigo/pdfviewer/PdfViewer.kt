package com.verrigo.pdfviewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.gemarktech.x5group.findIntersection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class PdfViewer @JvmOverloads constructor(
    context: Context,
    defAttrs: AttributeSet? = null,
    defStyleRes: Int = 0
) : RecyclerView(context, defAttrs, defStyleRes) {

    private val mainHandler = Handler(context.mainLooper)

    private val pdfAdapter = PdfAdapter()

    init {
        isSaveEnabled = true
        adapter = pdfAdapter
        layoutManager = LinearLayoutManager(context)
        setPadding(0, 20, 0, 40)
        clipToPadding = false
    }

    private var lastSeenItemPos = -1
    private var savedLastSeenItemPos = -1
    private var startedDebounce = false

    private val executor = Executors.newSingleThreadExecutor()
    private val scope = CoroutineScope(executor.asCoroutineDispatcher())

    private var pdfFile: File? = null
    private lateinit var pdfRenderer: PdfRenderer
    private lateinit var pdfFileDescriptor: ParcelFileDescriptor

    private val onLastItemPosChange: (Int) -> Unit = debounce(
        waitMs = 20L, coroutineScope = scope, ::loadNewPages
    )

    fun setPdfFile(pdfFile: File): PdfViewer {
        this.pdfFile = pdfFile
        try {
            pdfFileDescriptor =
                ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).also {
                    pdfRenderer = PdfRenderer(it)
                }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return this
    }

    fun load(givenWidthInPx: Int = -1) {
        post {
            pdfAdapter.setWidth(if (givenWidthInPx == -1) width else givenWidthInPx)
            val firstPage = pdfRenderer.openPage(0)
            pdfAdapter.setPlaceholderBitmap(
                Bitmap.createBitmap(firstPage.width, firstPage.height, Bitmap.Config.ARGB_8888)
            )
            firstPage.close()
            pdfAdapter.bitmapPool = MutableList(pdfRenderer.pageCount) { null }
            pdfAdapter.notifyDataSetChanged()
            scope.launch {
                pdfAdapter.bitmapPoolRange = 0..min(pdfRenderer.pageCount - 1, INITIAL_PAGE_COUNT)
                for (i in pdfAdapter.bitmapPoolRange) {
                    addLoadedBitmapAndNotify(createBitmap(i), i)
                }
            }
        }
    }

    private fun <T> debounce(
        waitMs: Long = 300L,
        coroutineScope: CoroutineScope,
        destinationFunction: (T) -> Unit
    ): (T) -> Unit {
        var debounceJob: Job? = null
        return { param: T ->
            debounceJob?.cancel()
            debounceJob = coroutineScope.launch {
                delay(waitMs)
                destinationFunction(param)
            }
        }
    }

    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        onScrollChange()
    }

    private fun onScrollChange() {
        val linearLayoutManager = layoutManager as LinearLayoutManager
        val lastItemPos = linearLayoutManager.findLastVisibleItemPosition()
        if (lastItemPos == lastSeenItemPos) return
        lastSeenItemPos = lastItemPos
        if (!startedDebounce) {
            startedDebounce = true
        }
        onLastItemPosChange(lastItemPos)
    }

    private fun loadNewPages(lastSeenPagePos: Int) {
        startedDebounce = false
        if (savedLastSeenItemPos == lastSeenPagePos) {
            return
        }
        savedLastSeenItemPos = lastSeenPagePos
        val start = max(0, lastSeenPagePos - PAGES_BEFORE_LAST_SEEN)
        val end = min(lastSeenPagePos + PAGES_AFTER_LAST_SEEN, pdfRenderer.pageCount - 1)

        val newRange = start..end
        val oldRange = pdfAdapter.bitmapPoolRange

        val rangeToLeave = oldRange.findIntersection(newRange)
        val rangeToDelete = oldRange - rangeToLeave
        val rangeToLoad = newRange - rangeToLeave

        pdfAdapter.bitmapPoolRange = newRange

        deleteRange(rangeToDelete)
        loadRange(rangeToLoad)
    }

    private fun loadRange(rangeToLoad: List<Int>) {
        scope.launch {
            for (i in rangeToLoad) {
                pdfAdapter.bitmapPool[i] = createBitmap(i)
            }
            if (rangeToLoad.isNotEmpty()) {
                mainHandler.post {
                    pdfAdapter.notifyItemRangeChanged(rangeToLoad.first(), rangeToLoad.size)
                }
            }
        }
    }

    private fun deleteRange(rangeToDelete: List<Int>) {
        scope.launch {
            for (i in rangeToDelete) {
                pdfAdapter.bitmapPool[i] = null
            }
            if (rangeToDelete.isNotEmpty()) {
                mainHandler.post {
                    pdfAdapter.notifyItemRangeChanged(rangeToDelete.first(), rangeToDelete.size)
                }
            }
        }
    }

    private fun addLoadedBitmapAndNotify(bitmap: Bitmap, ind: Int) {
        pdfAdapter.bitmapPool[ind] = bitmap
        mainHandler.post {
            pdfAdapter.notifyItemChanged(ind)
        }
    }

    private fun createBitmap(pageNumber: Int): Bitmap {
        val page = pdfRenderer.openPage(pageNumber)
        val bitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        return bitmap
    }

    companion object {
        private const val INITIAL_PAGE_COUNT = 10
        private const val PAGES_BEFORE_LAST_SEEN = 4
        private const val PAGES_AFTER_LAST_SEEN = 4
    }
}
