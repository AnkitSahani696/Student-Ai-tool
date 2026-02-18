package com.example.firebase

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.firebase.databinding.ItemProjectBinding
import com.example.firebase.Project

class ProjectsAdapter(
    private val projectsList: List<Project>,
    private val onEdit: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<ProjectsAdapter.ProjectViewHolder>() {

    inner class ProjectViewHolder(private val binding: ItemProjectBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(project: Project, position: Int) {
            binding.apply {
                tvTitle.text = project.title
                tvDescription.text = project.description
                tvTechnologies.text = "Tech: ${project.technologies}"
                tvDuration.text = project.duration

                if (project.link.isNotEmpty()) {
                    tvLink.text = project.link
                    tvLink.visibility = android.view.View.VISIBLE
                } else {
                    tvLink.visibility = android.view.View.GONE
                }

                btnEdit.setOnClickListener { onEdit(position) }
                btnDelete.setOnClickListener { onDelete(position) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProjectViewHolder {
        val binding = ItemProjectBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProjectViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProjectViewHolder, position: Int) {
        holder.bind(projectsList[position], position)
    }

    override fun getItemCount() = projectsList.size
}
