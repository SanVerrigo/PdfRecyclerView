package com.verrigo.pdfviewer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.verrigo.pdfviewer.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        with (binding) {
            btnSmallPdfAsset.setOnClickListener { openAsset("sample_small.pdf") }
            btnMedium10PdfAsset.setOnClickListener { openAsset("sample_medium_10pages.pdf") }
            btnMedium82PdfAsset.setOnClickListener { openAsset("sample_medium_82pages.pdf") }
            btnLargePdfAsset.setOnClickListener { openAsset("sample_large.pdf") }
        }
    }

    private fun openAsset(path: String) {
        startActivity(PdfViewerAssetActivity.createIntent(this, path))
    }
}
