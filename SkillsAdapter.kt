package com.example.firebase
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.firebase.databinding.ItemSkillBinding


class SkillsAdapter(
    private val skillsList: List<String>,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<SkillsAdapter.SkillViewHolder>() {

    inner class SkillViewHolder(private val binding: ItemSkillBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(skill: String, position: Int) {
            binding.apply {
                tvSkill.text = skill
                btnDelete.setOnClickListener { onDelete(position) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkillViewHolder {
        val binding = ItemSkillBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SkillViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SkillViewHolder, position: Int) {
        holder.bind(skillsList[position], position)
    }

    override fun getItemCount() = skillsList.size
}
