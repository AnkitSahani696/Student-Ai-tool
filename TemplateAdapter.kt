package com.example.firebase
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.firebase.databinding.ItemTemplateBinding


class TemplateAdapter(
    private val templateList: List<Template>,
    private var selectedTemplateId: Int,
    private val onTemplateSelected: (Int) -> Unit
) : RecyclerView.Adapter<TemplateAdapter.TemplateViewHolder>() {

    inner class TemplateViewHolder(private val binding: ItemTemplateBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(template: Template, isSelected: Boolean) {
            binding.apply {
                tvTemplateName.text = template.name
                tvTemplateDescription.text = template.description

                // Load preview image if URL is available
                if (template.previewUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(template.previewUrl)
                        .placeholder(R.drawable.ic_template_placeholder)
                        .into(ivTemplatePreview)
                } else {
                    ivTemplatePreview.setImageResource(R.drawable.ic_template_placeholder)
                }

                // Highlight selected template
                cardTemplate.strokeWidth = if (isSelected) 4 else 0
                cardTemplate.strokeColor = if (isSelected) {
                    itemView.context.getColor(R.color.primary)
                } else {
                    itemView.context.getColor(android.R.color.transparent)
                }

                cardTemplate.setOnClickListener {
                    onTemplateSelected(template.id)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TemplateViewHolder {
        val binding = ItemTemplateBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TemplateViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TemplateViewHolder, position: Int) {
        holder.bind(templateList[position], templateList[position].id == selectedTemplateId)
    }

    override fun getItemCount() = templateList.size

    fun setSelectedTemplate(templateId: Int) {
        val oldPosition = templateList.indexOfFirst { it.id == selectedTemplateId }
        val newPosition = templateList.indexOfFirst { it.id == templateId }

        selectedTemplateId = templateId

        if (oldPosition >= 0) notifyItemChanged(oldPosition)
        if (newPosition >= 0) notifyItemChanged(newPosition)
    }
}
