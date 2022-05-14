package com.verrigo.pdfviewer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.verrigo.pdfviewer.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        setPdfFile()
    }

    private fun setPdfFile() {
        binding.pdfViewer
            .setPdfFile(getSampleFile(true))
            .load()
    }

    private fun getSampleFile(big: Boolean): File {
        val file = File(application.filesDir, "temp_file.pdf")
        val assetName = if (big) {
            "sample_big.pdf"
        } else {
            "sample.pdf"
        }
        assets.open(assetName).use { assetFile ->
            file.writeBytes(assetFile.readBytes())
        }
        return file
    }
}
