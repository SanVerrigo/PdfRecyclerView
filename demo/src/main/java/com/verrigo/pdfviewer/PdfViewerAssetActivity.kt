package com.verrigo.pdfviewer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.verrigo.pdfviewer.databinding.ActivityPdfViewerAssetBinding
import java.io.File

class PdfViewerAssetActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityPdfViewerAssetBinding.inflate(layoutInflater)
    }
    private val assetPath: String by lazy {
        intent.getStringExtra(EXTRA_PATH)!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        title = getString(R.string.asset_title, assetPath)
        supportActionBar!!.apply {
            setDisplayHomeAsUpEnabled(true)
        }
        setPdfFile()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setPdfFile() {
        binding.pdfViewer
            .setPdfFile(getSampleFile())
            .load()
    }

    private fun getSampleFile(): File {
        val file = File(application.filesDir, "temp_file.pdf")
        assets.open(assetPath).use { assetFile ->
            file.writeBytes(assetFile.readBytes())
        }
        return file
    }

    companion object {

        private const val EXTRA_PATH = "path"

        fun createIntent(context: Context, assetPath: String): Intent {
            return Intent(context, PdfViewerAssetActivity::class.java).apply {
                putExtra(EXTRA_PATH, assetPath)
            }
        }
    }
}
