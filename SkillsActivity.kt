package com.example.firebase
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebase.databinding.ActivitySkillsBinding
import com.example.firebase.databinding.DialogAddSkillBinding
import com.google.android.material.chip.Chip




class SkillsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySkillsBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var skillsAdapter: SkillsAdapter
    private var resumeData: ResumeData? = null
    private val skillsList = mutableListOf<String>()

    // Predefined skill suggestions
    private val suggestedSkills = listOf(
        "Java", "Kotlin", "Python", "JavaScript", "React", "Node.js",
        "Android Development", "iOS Development", "Machine Learning", "Data Structures",
        "Algorithms", "SQL", "MongoDB", "Git", "Firebase", "REST API",
        "UI/UX Design", "Figma", "HTML", "CSS", "C++", "C#", "Swift"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivitySkillsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceManager = PreferenceManager(this)

        setupToolbar()
        loadExistingData()
        setupRecyclerView()
        setupSuggestedSkills()
        setupClickListeners()
        updateUI()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = "Skills"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun loadExistingData() {
        resumeData = preferenceManager.getResumeData()
        resumeData?.skills?.let { skills ->
            skillsList.clear()
            skillsList.addAll(skills)
        }
    }

    private fun setupRecyclerView() {
        skillsAdapter = SkillsAdapter(
            skillsList,
            onDelete = { position -> deleteSkill(position) }
        )

        binding.rvSkills.apply {
            layoutManager = LinearLayoutManager(this@SkillsActivity)
            adapter = skillsAdapter
        }
    }

    private fun setupSuggestedSkills() {
        binding.chipGroupSuggested.removeAllViews()

        suggestedSkills.forEach { skill ->
            val chip = Chip(this)
            chip.text = skill
            chip.isClickable = true
            chip.isCheckable = false
            chip.setOnClickListener {
                if (!skillsList.contains(skill)) {
                    addSkill(skill)
                } else {
                    Toast.makeText(this, "Skill already added", Toast.LENGTH_SHORT).show()
                }
            }
            binding.chipGroupSuggested.addView(chip)
        }
    }

    private fun setupClickListeners() {
        binding.fabAddSkill.setOnClickListener {
            showAddSkillDialog()
        }

        binding.btnNext.setOnClickListener {
            if (skillsList.isEmpty()) {
                Toast.makeText(this, "Please add at least one skill", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            saveData()
            navigateToProjects()
        }

        binding.btnSkip.setOnClickListener {
            saveData()
            navigateToProjects()
        }
    }

    private fun showAddSkillDialog() {
        val dialogBinding = DialogAddSkillBinding.inflate(LayoutInflater.from(this))

        AlertDialog.Builder(this)
            .setTitle("Add Skill")
            .setView(dialogBinding.root)
            .setPositiveButton("Add") { dialog, _ ->
                val skill = dialogBinding.etSkill.text.toString().trim()

                if (skill.isNotEmpty()) {
                    if (!skillsList.contains(skill)) {
                        addSkill(skill)
                    } else {
                        Toast.makeText(this, "Skill already added", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please enter a skill", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun addSkill(skill: String) {
        skillsList.add(skill)
        skillsAdapter.notifyItemInserted(skillsList.size - 1)
        updateUI()
        saveData()
        Toast.makeText(this, "Skill added", Toast.LENGTH_SHORT).show()
    }

    private fun deleteSkill(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Skill")
            .setMessage("Are you sure you want to delete this skill?")
            .setPositiveButton("Delete") { _, _ ->
                skillsList.removeAt(position)
                skillsAdapter.notifyItemRemoved(position)
                updateUI()
                saveData()
                Toast.makeText(this, "Skill deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateUI() {
        if (skillsList.isEmpty()) {
            binding.tvEmptyState.visibility = android.view.View.VISIBLE
            binding.rvSkills.visibility = android.view.View.GONE
        } else {
            binding.tvEmptyState.visibility = android.view.View.GONE
            binding.rvSkills.visibility = android.view.View.VISIBLE
        }

        binding.tvSkillCount.text = "${skillsList.size} skills added"
    }

    private fun saveData() {
        if (resumeData == null) {
            resumeData = ResumeData()
        }
        resumeData?.skills = skillsList.toList()
        preferenceManager.saveResumeData(resumeData!!)
    }

    private fun navigateToProjects() {
        val intent = Intent(this, ProjectsActivity::class.java)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
