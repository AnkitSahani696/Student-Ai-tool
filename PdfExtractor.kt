package com.example.firebase

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader  // ✅ Import added
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream

class PdfExtractor(private val context: Context) {

    // ✅ Init block - jab bhi class ka object banega, ye automatically run hoga
    init {
        PDFBoxResourceLoader.init(context)
    }

    fun extractText(uri: Uri): String {
        val inputStream: InputStream =
            context.contentResolver.openInputStream(uri)
                ?: throw Exception("PDF open nahi ho pa raha")

        return try {
            val document = PDDocument.load(inputStream)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            text.trim()
        } catch (e: Exception) {
            throw Exception("PDF se text extract karne me error: ${e.message}")
        } finally {
            inputStream.close()
        }
    }

    fun getPdfName(uri: Uri): String {
        var fileName = "Document"

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && index != -1) {
                fileName = cursor.getString(index)
            }
        }

        return fileName.replace(".pdf", "", true)
    }
}