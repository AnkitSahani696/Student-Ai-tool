package com.example.firebase
import com.google.gson.annotations.SerializedName

data class ResumeData(
    @SerializedName("personal_info")
    var personalInfo: PersonalInfo = PersonalInfo(),

    @SerializedName("education")
    var education: List<Education> = mutableListOf(),

    @SerializedName("skills")
    var skills: List<String> = mutableListOf(),

    @SerializedName("experience")
    var experience: List<Experience> = mutableListOf(),

    @SerializedName("projects")
    var projects: List<Project> = mutableListOf(),

    @SerializedName("template_id")
    var templateId: Int = 1
)

data class PersonalInfo(
    @SerializedName("full_name")
    var fullName: String = "",

    @SerializedName("email")
    var email: String = "",

    @SerializedName("phone")
    var phone: String = "",

    @SerializedName("address")
    var address: String = "",

    @SerializedName("linkedin")
    var linkedin: String = "",

    @SerializedName("github")
    var github: String = "",

    @SerializedName("objective")
    var objective: String = "",

    @SerializedName("profile_image_url")
    var profileImageUrl: String = ""
)

data class Education(
    @SerializedName("degree")
    var degree: String = "",

    @SerializedName("institution")
    var institution: String = "",

    @SerializedName("year")
    var year: String = "",

    @SerializedName("percentage")
    var percentage: String = "",

    @SerializedName("field_of_study")
    var fieldOfStudy: String = ""
)

data class Experience(
    @SerializedName("job_title")
    var jobTitle: String = "",

    @SerializedName("company")
    var company: String = "",

    @SerializedName("duration")
    var duration: String = "",

    @SerializedName("description")
    var description: String = "",

    @SerializedName("location")
    var location: String = ""
)

data class Project(
    @SerializedName("title")
    var title: String = "",

    @SerializedName("description")
    var description: String = "",

    @SerializedName("technologies")
    var technologies: String = "",

    @SerializedName("duration")
    var duration: String = "",

    @SerializedName("link")
    var link: String = ""
)

// API Response Models
data class ResumeResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String,

    @SerializedName("resume_url")
    val resumeUrl: String? = null,

    @SerializedName("pdf_url")
    val pdfUrl: String? = null
)

data class TemplateResponse(
    @SerializedName("templates")
    val templates: List<Template>
)

data class Template(
    @SerializedName("id")
    val id: Int,

    @SerializedName("name")
    val name: String,

    @SerializedName("preview_url")
    val previewUrl: String,

    @SerializedName("description")
    val description: String
)