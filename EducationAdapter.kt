package com.example.firebase
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.firebase.databinding.ItemEducationBinding
import com.example.firebase.Education

class EducationAdapter(
    private val educationList: List<Education>,
    private val onEdit: (Int) -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<EducationAdapter.EducationViewHolder>() {

    inner class EducationViewHolder(private val binding: ItemEducationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(education: Education, position: Int) {
            binding.apply {
                tvDegree.text = "${education.degree} - ${education.fieldOfStudy}"
                tvInstitution.text = education.institution
                tvYear.text = education.year
                tvPercentage.text = education.percentage

                btnEdit.setOnClickListener { onEdit(position) }
                btnDelete.setOnClickListener { onDelete(position) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EducationViewHolder {
        val binding = ItemEducationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EducationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EducationViewHolder, position: Int) {
        holder.bind(educationList[position], position)
    }

    override fun getItemCount() = educationList.size
}
