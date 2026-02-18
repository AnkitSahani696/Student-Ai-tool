package com.example.firebase
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "QuestionGenerator.db"
        private const val DATABASE_VERSION = 1

        // Tables
        private const val TABLE_PDF = "pdfs"
        private const val TABLE_QUESTIONS = "questions"

        // PDF Table Columns
        private const val COL_PDF_ID = "id"
        private const val COL_PDF_NAME = "name"
        private const val COL_PDF_DATE = "date"

        // Questions Table Columns
        private const val COL_Q_ID = "id"
        private const val COL_Q_PDF_ID = "pdf_id"
        private const val COL_Q_QUESTION = "question"
        private const val COL_Q_ANSWER = "answer"
        private const val COL_Q_TYPE = "type"
        private const val COL_Q_OPTIONS = "options"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Create PDF table
        val createPdfTable = """
            CREATE TABLE $TABLE_PDF (
                $COL_PDF_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PDF_NAME TEXT NOT NULL,
                $COL_PDF_DATE TEXT NOT NULL
            )
        """.trimIndent()

        // Create Questions table
        val createQuestionsTable = """
            CREATE TABLE $TABLE_QUESTIONS (
                $COL_Q_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_Q_PDF_ID INTEGER NOT NULL,
                $COL_Q_QUESTION TEXT NOT NULL,
                $COL_Q_ANSWER TEXT NOT NULL,
                $COL_Q_TYPE TEXT NOT NULL,
                $COL_Q_OPTIONS TEXT,
                FOREIGN KEY($COL_Q_PDF_ID) REFERENCES $TABLE_PDF($COL_PDF_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db?.execSQL(createPdfTable)
        db?.execSQL(createQuestionsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_QUESTIONS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_PDF")
        onCreate(db)
    }

    fun savePdfWithQuestions(
        pdfName: String,
        shortQuestions: List<Question>,
        longQuestions: List<Question>,
        mcqs: List<Question>
    ): Long {
        val db = writableDatabase

        // Save PDF entry
        val pdfValues = ContentValues().apply {
            put(COL_PDF_NAME, pdfName)
            put(COL_PDF_DATE, getCurrentDate())
        }

        val pdfId = db.insert(TABLE_PDF, null, pdfValues)

        // Save all questions
        val allQuestions = shortQuestions + longQuestions + mcqs

        for (question in allQuestions) {
            val questionValues = ContentValues().apply {
                put(COL_Q_PDF_ID, pdfId)
                put(COL_Q_QUESTION, question.question)
                put(COL_Q_ANSWER, question.answer)
                put(COL_Q_TYPE, question.type)
                put(COL_Q_OPTIONS, question.options?.joinToString("|||"))
            }

            db.insert(TABLE_QUESTIONS, null, questionValues)
        }

        return pdfId
    }

    fun getAllPdfHistory(): List<PdfItem> {
        val pdfList = mutableListOf<PdfItem>()
        val db = readableDatabase

        val cursor = db.rawQuery(
            """
            SELECT p.$COL_PDF_ID, p.$COL_PDF_NAME, p.$COL_PDF_DATE, COUNT(q.$COL_Q_ID) as question_count
            FROM $TABLE_PDF p
            LEFT JOIN $TABLE_QUESTIONS q ON p.$COL_PDF_ID = q.$COL_Q_PDF_ID
            GROUP BY p.$COL_PDF_ID
            ORDER BY p.$COL_PDF_ID DESC
            """.trimIndent(),
            null
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_PDF_ID))
                val name = cursor.getString(cursor.getColumnIndexOrThrow(COL_PDF_NAME))
                val date = cursor.getString(cursor.getColumnIndexOrThrow(COL_PDF_DATE))
                val count = cursor.getInt(cursor.getColumnIndexOrThrow("question_count"))

                pdfList.add(PdfItem(id, name, date, count))
            } while (cursor.moveToNext())
        }

        cursor.close()
        return pdfList
    }

    fun getAllQuestionsForPdf(pdfId: Long): List<Question> {
        val questions = mutableListOf<Question>()
        val db = readableDatabase

        val cursor = db.query(
            TABLE_QUESTIONS,
            null,
            "$COL_Q_PDF_ID = ?",
            arrayOf(pdfId.toString()),
            null,
            null,
            COL_Q_ID
        )

        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(COL_Q_ID))
                val question = cursor.getString(cursor.getColumnIndexOrThrow(COL_Q_QUESTION))
                val answer = cursor.getString(cursor.getColumnIndexOrThrow(COL_Q_ANSWER))
                val type = cursor.getString(cursor.getColumnIndexOrThrow(COL_Q_TYPE))
                val optionsString = cursor.getString(cursor.getColumnIndexOrThrow(COL_Q_OPTIONS))

                val options = optionsString?.split("|||")

                questions.add(Question(id, question, answer, type, options))
            } while (cursor.moveToNext())
        }

        cursor.close()
        return questions
    }

    fun getQuestionsByType(pdfId: Long, type: String): List<Question> {
        return getAllQuestionsForPdf(pdfId).filter { it.type == type }
    }

    fun deletePdf(pdfId: Long) {
        val db = writableDatabase

        // Delete questions first
        db.delete(TABLE_QUESTIONS, "$COL_Q_PDF_ID = ?", arrayOf(pdfId.toString()))

        // Delete PDF
        db.delete(TABLE_PDF, "$COL_PDF_ID = ?", arrayOf(pdfId.toString()))
    }

    fun clearAllHistory() {
        val db = writableDatabase
        db.delete(TABLE_QUESTIONS, null, null)
        db.delete(TABLE_PDF, null, null)
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        return sdf.format(Date())
    }
}