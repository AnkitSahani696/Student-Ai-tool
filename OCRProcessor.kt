package com.example.firebase
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * OCR Processor using Google ML Kit
 * Extracts text from images
 */
class OCRProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Extract text from image bitmap
     */
    suspend fun extractTextFromBitmap(bitmap: Bitmap): Result<String> {
        return try {
            val inputImage = InputImage.fromBitmap(bitmap, 0)
            val visionText = recognizer.process(inputImage).await()

            val extractedText = visionText.text

            if (extractedText.isBlank()) {
                Result.failure(Exception("No text found in image"))
            } else {
                Result.success(extractedText)
            }
        } catch (e: Exception) {
            Log.e("OCRProcessor", "Error extracting text", e)
            Result.failure(e)
        }
    }

    /**
     * Extract text from image URI
     */
    suspend fun extractTextFromUri(
        uri: Uri,
        context: android.content.Context
    ): Result<String> {
        return try {
            val inputImage = InputImage.fromFilePath(context, uri)
            val visionText = recognizer.process(inputImage).await()

            val extractedText = visionText.text

            if (extractedText.isBlank()) {
                Result.failure(Exception("No text found in image"))
            } else {
                Result.success(extractedText)
            }
        } catch (e: Exception) {
            Log.e("OCRProcessor", "Error extracting text from URI", e)
            Result.failure(e)
        }
    }

    /**
     * Clean up resources
     */
    fun close() {
        recognizer.close()
    }
}