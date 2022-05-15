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
            btnSmallPdfAsset.setOnClickListener { openAsset("sample.pdf") }
            btnLargePdfAsset.setOnClickListener { openAsset("sample_big.pdf") }
        }
    }

    private fun openAsset(path: String) {
        startActivity(PdfViewerAssetActivity.createIntent(this, path))
    }
}
