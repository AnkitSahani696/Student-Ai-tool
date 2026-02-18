package com.example.firebase
import android.content.Context
import com.itextpdf.kernel.colors.ColorConstants
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Text
import com.itextpdf.layout.properties.TextAlignment
import java.io.File
import java.io.FileOutputStream

class PdfGenerator(private val context: Context) {

    fun generateResumePDF(resumeData: ResumeData, fileName: String = "resume.pdf"): File {
        val pdfFile = File(context.getExternalFilesDir(null), fileName)
        val pdfWriter = PdfWriter(FileOutputStream(pdfFile))
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)

        // Header with name
        val nameText = Text(resumeData.personalInfo.fullName)
            .setFontSize(24f)
            .setBold()
            .setFontColor(DeviceRgb(41, 128, 185))
        document.add(Paragraph(nameText).setTextAlignment(TextAlignment.CENTER))

        // Contact Information
        val contactInfo = "${resumeData.personalInfo.email} | ${resumeData.personalInfo.phone}"
        document.add(
            Paragraph(contactInfo)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontSize(10f)
        )

        if (resumeData.personalInfo.linkedin.isNotEmpty()) {
            document.add(
                Paragraph("LinkedIn: ${resumeData.personalInfo.linkedin}")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(9f)
            )
        }

        if (resumeData.personalInfo.github.isNotEmpty()) {
            document.add(
                Paragraph("GitHub: ${resumeData.personalInfo.github}")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(9f)
            )
        }

        document.add(Paragraph("\n"))

        // Objective
        if (resumeData.personalInfo.objective.isNotEmpty()) {
            addSectionHeader(document, "OBJECTIVE")
            document.add(Paragraph(resumeData.personalInfo.objective).setFontSize(10f))
            document.add(Paragraph("\n"))
        }

        // Education
        if (resumeData.education.isNotEmpty()) {
            addSectionHeader(document, "EDUCATION")
            resumeData.education.forEach { edu ->
                val eduTitle = Text("${edu.degree} - ${edu.fieldOfStudy}")
                    .setBold()
                    .setFontSize(11f)
                document.add(Paragraph(eduTitle))

                document.add(
                    Paragraph("${edu.institution} | ${edu.year} | ${edu.percentage}")
                        .setFontSize(10f)
                        .setMarginLeft(10f)
                )
                document.add(Paragraph("\n"))
            }
        }

        // Skills
        if (resumeData.skills.isNotEmpty()) {
            addSectionHeader(document, "SKILLS")
            val skillsText = resumeData.skills.joinToString(", ")
            document.add(Paragraph(skillsText).setFontSize(10f))
            document.add(Paragraph("\n"))
        }

        // Projects
        if (resumeData.projects.isNotEmpty()) {
            addSectionHeader(document, "PROJECTS")
            resumeData.projects.forEach { project ->
                val projectTitle = Text(project.title)
                    .setBold()
                    .setFontSize(11f)
                document.add(Paragraph(projectTitle))

                document.add(
                    Paragraph(project.description)
                        .setFontSize(10f)
                        .setMarginLeft(10f)
                )

                if (project.technologies.isNotEmpty()) {
                    document.add(
                        Paragraph("Technologies: ${project.technologies}")
                            .setFontSize(9f)
                            .setMarginLeft(10f)
                            .setItalic()
                    )
                }
                document.add(Paragraph("\n"))
            }
        }

        // Experience
        if (resumeData.experience.isNotEmpty()) {
            addSectionHeader(document, "EXPERIENCE")
            resumeData.experience.forEach { exp ->
                val expTitle = Text("${exp.jobTitle} at ${exp.company}")
                    .setBold()
                    .setFontSize(11f)
                document.add(Paragraph(expTitle))

                document.add(
                    Paragraph("${exp.duration} | ${exp.location}")
                        .setFontSize(9f)
                        .setMarginLeft(10f)
                )

                document.add(
                    Paragraph(exp.description)
                        .setFontSize(10f)
                        .setMarginLeft(10f)
                )
                document.add(Paragraph("\n"))
            }
        }

        document.close()
        return pdfFile
    }

    private fun addSectionHeader(document: Document, title: String) {
        val headerText = Text(title)
            .setBold()
            .setFontSize(14f)
            .setFontColor(DeviceRgb(41, 128, 185))
        document.add(Paragraph(headerText))
        document.add(
            Paragraph("_".repeat(80))
                .setFontSize(8f)
                .setFontColor(ColorConstants.LIGHT_GRAY)
        )
    }
}