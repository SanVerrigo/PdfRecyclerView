package com.verrigo.pdfviewer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.Handler
import android.os.ParcelFileDescriptor
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.math.min

class PdfRecyclerView @JvmOverloads constructor(
    context: Context,
    defAttrs: AttributeSet? = null,
    defStyleRes: Int = 0
) : RecyclerView(context, defAttrs, defStyleRes) {

    private val mainHandler: Handler = Handler(context.mainLooper)
    private val pdfAdapter: PdfAdapter = PdfAdapter()

    init {
        isSaveEnabled = true
        adapter = pdfAdapter
        layoutManager = LinearLayoutManager(context)
        clipToPadding = false
    }

    private var lastSeenItemPos: Int = -1
    private var savedLastSeenItemPos: Int = -1
    private var startedDebounce: Boolean = false

    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val scope: CoroutineScope = CoroutineScope(executor.asCoroutineDispatcher())

    private var pdfFile: File? = null
    private lateinit var pdfRenderer: PdfRenderer
    private lateinit var pdfFileDescriptor: ParcelFileDescriptor

    private val onLastItemPosChange: (Int) -> Unit = debounce(
        waitMs = 20L, coroutineScope = scope, ::loadNewPages
    )

    fun setPdfFile(pdfFile: File): PdfRecyclerView {
        this.pdfFile = pdfFile
        pdfFileDescriptor =
            ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).also {
                pdfRenderer = PdfRenderer(it)
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
                val range = 0..min(pdfRenderer.pageCount - 1, INITIAL_PAGE_COUNT)
                pdfAdapter.bitmapPoolPagesRange = range
                loadRange(range)
                // todo: bottom is deprecated?
                /*for (i in pdfAdapter.bitmapPoolPagesRange) {
                    addLoadedBitmapAndNotify(createBitmap(i), i)
                }*/
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
        if (lastItemPos == lastSeenItemPos) {
            return
        }

        lastSeenItemPos = lastItemPos
        if (!startedDebounce) { // todo: is this flag yet needed?
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
        val oldRange = pdfAdapter.bitmapPoolPagesRange

        val rangeToLeave = oldRange.findIntersection(newRange)
        val rangeToDelete = oldRange - rangeToLeave
        val rangeToLoad = newRange - rangeToLeave

        pdfAdapter.bitmapPoolPagesRange = newRange

        deleteRange(rangeToDelete)
        loadRange(rangeToLoad)
    }

    private fun loadRange(rangeToLoad: Iterable<Int>) {
        val rangeSize = rangeToLoad.size()
        if (rangeSize == 0) {
            return
        }
        scope.launch {
            for (i in rangeToLoad) {
                pdfAdapter.bitmapPool[i] = createBitmap(i)
            }
            mainHandler.post {
                pdfAdapter.notifyItemRangeChanged(rangeToLoad.first(), rangeSize)
            }
        }
    }

    private fun deleteRange(rangeToDelete: List<Int>) {
        if (rangeToDelete.isEmpty()) {
            return
        }
        scope.launch { // todo: do we need to delete bitmaps in scope? not in main thread directly?
            for (i in rangeToDelete) {
                pdfAdapter.bitmapPool[i] = null
            }
            mainHandler.post {
                pdfAdapter.notifyItemRangeChanged(rangeToDelete.first(), rangeToDelete.size)
            }
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
        private const val PAGES_BEFORE_LAST_SEEN = 4 // delta to load before
        private const val PAGES_AFTER_LAST_SEEN = 4 // delta to load after
    }
}
