package com.example.firebase
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebase.databinding.ActivityProjectsBinding
import com.example.firebase.databinding.DialogAddProjectBinding


class ProjectsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProjectsBinding
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var projectsAdapter: ProjectsAdapter
    private var resumeData: ResumeData? = null
    private val projectsList = mutableListOf<Project>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityProjectsBinding.inflate(layoutInflater)
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
            title = "Projects"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun loadExistingData() {
        resumeData = preferenceManager.getResumeData()
        resumeData?.projects?.let { projects ->
            projectsList.clear()
            projectsList.addAll(projects)
        }
    }

    private fun setupRecyclerView() {
        projectsAdapter = ProjectsAdapter(
            projectsList,
            onEdit = { position -> editProject(position) },
            onDelete = { position -> deleteProject(position) }
        )

        binding.rvProjects.apply {
            layoutManager = LinearLayoutManager(this@ProjectsActivity)
            adapter = projectsAdapter
        }
    }

    private fun setupClickListeners() {
        binding.fabAddProject.setOnClickListener {
            showAddProjectDialog()
        }

        binding.btnNext.setOnClickListener {
            saveData()
            navigateToExperience()
        }

        binding.btnSkip.setOnClickListener {
            saveData()
            navigateToExperience()
        }
    }

    private fun showAddProjectDialog(project: Project? = null, position: Int = -1) {
        val dialogBinding = DialogAddProjectBinding.inflate(LayoutInflater.from(this))

        // Pre-fill if editing
        project?.let {
            dialogBinding.apply {
                etProjectTitle.setText(it.title)
                etProjectDescription.setText(it.description)
                etTechnologies.setText(it.technologies)
                etDuration.setText(it.duration)
                etLink.setText(it.link)
            }
        }

        AlertDialog.Builder(this)
            .setTitle(if (project == null) "Add Project" else "Edit Project")
            .setView(dialogBinding.root)
            .setPositiveButton("Save") { dialog, _ ->
                val newProject = Project(
                    title = dialogBinding.etProjectTitle.text.toString().trim(),
                    description = dialogBinding.etProjectDescription.text.toString().trim(),
                    technologies = dialogBinding.etTechnologies.text.toString().trim(),
                    duration = dialogBinding.etDuration.text.toString().trim(),
                    link = dialogBinding.etLink.text.toString().trim()
                )

                if (validateProject(newProject)) {
                    if (position >= 0) {
                        projectsList[position] = newProject
                    } else {
                        projectsList.add(newProject)
                    }
                    projectsAdapter.notifyDataSetChanged()
                    updateUI()
                    saveData()
                } else {
                    Toast.makeText(this, "Please fill required fields (Title & Description)", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun editProject(position: Int) {
        showAddProjectDialog(projectsList[position], position)
    }

    private fun deleteProject(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Project")
            .setMessage("Are you sure you want to delete this project?")
            .setPositiveButton("Delete") { _, _ ->
                projectsList.removeAt(position)
                projectsAdapter.notifyItemRemoved(position)
                updateUI()
                saveData()
                Toast.makeText(this, "Project deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun validateProject(project: Project): Boolean {
        return project.title.isNotEmpty() && project.description.isNotEmpty()
    }

    private fun updateUI() {
        if (projectsList.isEmpty()) {
            binding.tvEmptyState.visibility = android.view.View.VISIBLE
            binding.rvProjects.visibility = android.view.View.GONE
        } else {
            binding.tvEmptyState.visibility = android.view.View.GONE
            binding.rvProjects.visibility = android.view.View.VISIBLE
        }
    }

    private fun saveData() {
        if (resumeData == null) {
            resumeData = ResumeData()
        }
        resumeData?.projects = projectsList.toList()
        preferenceManager.saveResumeData(resumeData!!)
    }

    private fun navigateToExperience() {
        val intent = Intent(this, ExperienceActivity::class.java)
        startActivity(intent)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}