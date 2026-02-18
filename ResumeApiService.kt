package com.example.firebase


import retrofit2.Response
import retrofit2.http.*

interface ResumeApiService {

    @POST("api/resume/create")
    suspend fun createResume(
        @Body resumeData: ResumeData
    ): Response<ResumeResponse>

    @PUT("api/resume/update/{id}")
    suspend fun updateResume(
        @Path("id") resumeId: String,
        @Body resumeData: ResumeData
    ): Response<ResumeResponse>

    @GET("api/resume/{id}")
    suspend fun getResume(
        @Path("id") resumeId: String
    ): Response<ResumeData>

    @GET("api/templates")
    suspend fun getTemplates(): Response<TemplateResponse>

    @POST("api/resume/generate-pdf")
    suspend fun generatePDF(
        @Body resumeData: ResumeData
    ): Response<ResumeResponse>

    @GET("api/resume/download/{id}")
    suspend fun downloadResume(
        @Path("id") resumeId: String
    ): Response<ByteArray>
}