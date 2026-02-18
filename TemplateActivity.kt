package com.example.firebase

import android.content.Intent
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.firebase.databinding.ActivityTemplateBinding

import kotlinx.coroutines.launch

class TemplateActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTemplateBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var templateAdapter: TemplateAdapter
    private val templateList = mutableListOf<Template>()
    private var selectedTemplateId = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityTemplateBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)
        selectedTemplateId = preferenceManager.getSelectedTemplate()

        setupToolbar()
        setupRecyclerView()
        loadTemplates()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Choose Template"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        templateAdapter = TemplateAdapter(
            templateList,
            selectedTemplateId,
            onTemplateSelected = { templateId ->
                selectedTemplateId = templateId
                preferenceManager.saveSelectedTemplate(templateId)
                templateAdapter.setSelectedTemplate(templateId)
            }
        )

        binding.rvTemplates.apply {
            layoutManager = GridLayoutManager(this@TemplateActivity, 2)
            adapter = templateAdapter
        }
    }

    private fun loadTemplates() {
        // Show loading
        binding.progressBar.visibility = android.view.View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getTemplates()

                if (response.isSuccessful && response.body() != null) {
                    val templates = response.body()!!.templates
                    templateList.clear()
                    templateList.addAll(templates)
                    templateAdapter.notifyDataSetChanged()
                } else {
                    // Use default templates if API fails
                    loadDefaultTemplates()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Use default templates if API call fails
                loadDefaultTemplates()
            } finally {
                binding.progressBar.visibility = android.view.View.GONE
            }
        }
    }

    private fun loadDefaultTemplates() {
        // Default templates as fallback
        templateList.clear()
        templateList.addAll(
            listOf(
                Template(1, "Modern Professional", "", "Clean and modern design"),
                Template(2, "Classic Elegant", "", "Traditional professional look"),
                Template(3, "Creative Bold", "", "Stand out with bold design"),
                Template(4, "Minimalist", "", "Simple and clean layout"),
                Template(5, "Tech Savvy", "", "Perfect for tech professionals"),
                Template(6, "Academic", "", "Ideal for academic positions")
            )
        )
        templateAdapter.notifyDataSetChanged()
    }

    private fun setupClickListeners() {
        binding.btnNext.setOnClickListener {
            navigateToPreview()
        }
    }

    private fun navigateToPreview() {
        val intent = Intent(this, PreviewActivity::class.java)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
