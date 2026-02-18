package com.example.firebase

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebase.databinding.ActivityEducationBinding
import com.example.firebase.databinding.DialogAddEducationBinding



class EducationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEducationBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var educationAdapter: EducationAdapter
    private var resumeData: ResumeData? = null
    private val educationList = mutableListOf<Education>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityEducationBinding.inflate(layoutInflater)
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
            title = "Education"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun loadExistingData() {
        resumeData = preferenceManager.getResumeData()
        resumeData?.education?.let { education ->
            educationList.clear()
            educationList.addAll(education)
        }
    }

    private fun setupRecyclerView() {
        educationAdapter = EducationAdapter(
            educationList,
            onEdit = { position -> editEducation(position) },
            onDelete = { position -> deleteEducation(position) }
        )

        binding.rvEducation.apply {
            layoutManager = LinearLayoutManager(this@EducationActivity)
            adapter = educationAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddEducation.setOnClickListener {
            showAddEducationDialog()
        }

        binding.btnNext.setOnClickListener {
            if (educationList.isEmpty()) {
                Toast.makeText(this, "Please add at least one education entry", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveData()
            navigateToSkills()
        }

        binding.btnSkip.setOnClickListener {
            saveData()
            navigateToSkills()
        }
    }

    private fun showAddEducationDialog(education: Education? = null, position: Int = -1) {
        val dialogBinding = DialogAddEducationBinding.inflate(LayoutInflater.from(this))

        // Pre-fill if editing
        education?.let {
            dialogBinding.apply {
                etDegree.setText(it.degree)
                etInstitution.setText(it.institution)
                etYear.setText(it.year)
                etPercentage.setText(it.percentage)
                etFieldOfStudy.setText(it.fieldOfStudy)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(if (education == null) "Add Education" else "Edit Education")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { dialog, _ ->
                val newEducation = Education(
                    degree = dialogBinding.etDegree.text.toString().trim(),
                    institution = dialogBinding.etInstitution.text.toString().trim(),
                    year = dialogBinding.etYear.text.toString().trim(),
                    percentage = dialogBinding.etPercentage.text.toString().trim(),
                    fieldOfStudy = dialogBinding.etFieldOfStudy.text.toString().trim()
                )

                if (validateEducation(newEducation)) {
                    if (position >= 0) {
                        educationList[position] = newEducation
                    } else {
                        educationList.add(newEducation)
                    }
                    educationAdapter.notifyDataSetChanged()
                    updateUI()
                    saveData()
                } else {
                    Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun editEducation(position: Int) {
        showAddEducationDialog(educationList[position], position)
    }

    private fun deleteEducation(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Education")
            .setMessage("Are you sure you want to delete this education entry?")
            .setPositiveButton("Delete") { _, _ ->
                educationList.removeAt(position)
                educationAdapter.notifyItemRemoved(position)
                updateUI()
                saveData()
                Toast.makeText(this, "Education deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validateEducation(education: Education): Boolean {
        return education.degree.isNotEmpty() &&
                education.institution.isNotEmpty() &&
                education.year.isNotEmpty()
    }

    private fun updateUI() {
        if (educationList.isEmpty()) {
            binding.tvEmptyState.visibility = android.view.View.VISIBLE
            binding.rvEducation.visibility = android.view.View.GONE
        } else {
            binding.tvEmptyState.visibility = android.view.View.GONE
            binding.rvEducation.visibility = android.view.View.VISIBLE
        }
    }

    private fun saveData() {
        if (resumeData == null) {
            resumeData = ResumeData()
        }
        resumeData?.education = educationList.toList()
        preferenceManager.saveResumeData(resumeData!!)
    }

    private fun navigateToSkills() {
        val intent = Intent(this, SkillsActivity::class.java)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
