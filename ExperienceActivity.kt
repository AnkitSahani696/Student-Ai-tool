package com.example.firebase

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebase.databinding.ActivityExperienceBinding
import com.example.firebase.databinding.DialogAddExperienceBinding



class ExperienceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExperienceBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var experienceAdapter: ExperienceAdapter
    private var resumeData: ResumeData? = null
    private val experienceList = mutableListOf<Experience>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityExperienceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)

        setupToolbar()
        loadExistingData()
        setupRecyclerView()
        setupClickListeners()
        updateUI()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Work Experience"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun loadExistingData() {
        resumeData = preferenceManager.getResumeData()
        resumeData?.experience?.let { experience ->
            experienceList.clear()
            experienceList.addAll(experience)
        }
    }

    private fun setupRecyclerView() {
        experienceAdapter = ExperienceAdapter(
            experienceList,
            onEdit = { position -> editExperience(position) },
            onDelete = { position -> deleteExperience(position) }
        )

        binding.rvExperience.apply {
            layoutManager = LinearLayoutManager(this@ExperienceActivity)
            adapter = experienceAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddExperience.setOnClickListener {
            showAddExperienceDialog()
        }

        binding.btnNext.setOnClickListener {
            saveData()
            navigateToTemplate()
        }

        binding.btnSkip.setOnClickListener {
            saveData()
            navigateToTemplate()
        }
    }

    private fun showAddExperienceDialog(experience: Experience? = null, position: Int = -1) {
        val dialogBinding = DialogAddExperienceBinding.inflate(LayoutInflater.from(this))

        // Pre-fill if editing
        experience?.let {
            dialogBinding.apply {
                etJobTitle.setText(it.jobTitle)
                etCompany.setText(it.company)
                etDuration.setText(it.duration)
                etLocation.setText(it.location)
                etDescription.setText(it.description)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(if (experience == null) "Add Experience" else "Edit Experience")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { dialog, _ ->
                val newExperience = Experience(
                    jobTitle = dialogBinding.etJobTitle.text.toString().trim(),
                    company = dialogBinding.etCompany.text.toString().trim(),
                    duration = dialogBinding.etDuration.text.toString().trim(),
                    location = dialogBinding.etLocation.text.toString().trim(),
                    description = dialogBinding.etDescription.text.toString().trim()
                )

                if (validateExperience(newExperience)) {
                    if (position >= 0) {
                        experienceList[position] = newExperience
                    } else {
                        experienceList.add(newExperience)
                    }
                    experienceAdapter.notifyDataSetChanged()
                    updateUI()
                    saveData()
                } else {
                    Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun editExperience(position: Int) {
        showAddExperienceDialog(experienceList[position], position)
    }

    private fun deleteExperience(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Experience")
            .setMessage("Are you sure you want to delete this experience?")
            .setPositiveButton("Delete") { _, _ ->
                experienceList.removeAt(position)
                experienceAdapter.notifyItemRemoved(position)
                updateUI()
                saveData()
                Toast.makeText(this, "Experience deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validateExperience(experience: Experience): Boolean {
        return experience.jobTitle.isNotEmpty() &&
                experience.company.isNotEmpty() &&
                experience.duration.isNotEmpty()
    }

    private fun updateUI() {
        if (experienceList.isEmpty()) {
            binding.tvEmptyState.visibility = android.view.View.VISIBLE
            binding.rvExperience.visibility = android.view.View.GONE
        } else {
            binding.tvEmptyState.visibility = android.view.View.GONE
            binding.rvExperience.visibility = android.view.View.VISIBLE
        }
    }

    private fun saveData() {
        if (resumeData == null) {
            resumeData = ResumeData()
        }
        resumeData?.experience = experienceList.toList()
        preferenceManager.saveResumeData(resumeData!!)
    }

    private fun navigateToTemplate() {
        val intent = Intent(this, TemplateActivity::class.java)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
