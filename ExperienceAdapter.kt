package com.example.firebase

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.firebase.databinding.ItemExperienceBinding
import com.example.firebase.Experience

class ExperienceAdapter(
    private val experienceList: List<Experience>,
    private val onEdit: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<ExperienceAdapter.ExperienceViewHolder>() {

    inner class ExperienceViewHolder(private val binding: ItemExperienceBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(experience: Experience, position: Int) {
            binding.apply {
                tvJobTitle.text = experience.jobTitle
                tvCompany.text = experience.company
                tvDuration.text = experience.duration
                tvLocation.text = experience.location
                tvDescription.text = experience.description

                btnEdit.setOnClickListener { onEdit(position) }
                btnDelete.setOnClickListener { onDelete(position) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExperienceViewHolder {
        val binding = ItemExperienceBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExperienceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExperienceViewHolder, position: Int) {
        holder.bind(experienceList[position], position)
    }

    override fun getItemCount() = experienceList.size
}
