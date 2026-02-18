package com.example.firebase
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.firebase.databinding.ItemPdfHistoryBinding
import com.example.firebase.databinding.ItemQuestionBinding


// PDF History Adapter
class PdfHistoryAdapter(
    private val pdfList: List<PdfItem>,
    private val onItemClick: (PdfItem) -> Unit
) : RecyclerView.Adapter<PdfHistoryAdapter.PdfViewHolder>() {

    inner class PdfViewHolder(private val binding: ItemPdfHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(pdfItem: PdfItem) {
            binding.tvPdfName.text = pdfItem.name
            binding.tvDate.text = pdfItem.date
            binding.tvQuestionCount.text = "${pdfItem.questionCount} questions"

            binding.root.setOnClickListener {
                onItemClick(pdfItem)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfViewHolder {
        val binding = ItemPdfHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PdfViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PdfViewHolder, position: Int) {
        holder.bind(pdfList[position])
    }

    override fun getItemCount() = pdfList.size
}

// Questions Adapter
class QuestionsAdapter(
    private val questions: List<Question>
) : RecyclerView.Adapter<QuestionsAdapter.QuestionViewHolder>() {

    inner class QuestionViewHolder(private val binding: ItemQuestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(question: Question, position: Int) {
            binding.tvQuestionNumber.text = "Q${position + 1}"
            binding.tvQuestion.text = question.question

            when (question.type) {
                "mcq" -> {
                    // Show options
                    val optionsText = question.options?.mapIndexed { index, option ->
                        val prefix = ('A' + index)
                        "$prefix) $option"
                    }?.joinToString("\n") ?: ""

                    binding.tvOptions.text = optionsText
                    binding.tvOptions.visibility = android.view.View.VISIBLE
                    binding.tvAnswer.text = "Correct Answer: ${question.answer}"
                }
                else -> {
                    binding.tvOptions.visibility = android.view.View.GONE
                    binding.tvAnswer.text = question.answer
                }
            }

            // Toggle answer visibility
            var isAnswerVisible = false
            binding.btnShowAnswer.setOnClickListener {
                isAnswerVisible = !isAnswerVisible
                if (isAnswerVisible) {
                    binding.tvAnswer.visibility = android.view.View.VISIBLE
                    binding.btnShowAnswer.text = "Hide Answer"
                } else {
                    binding.tvAnswer.visibility = android.view.View.GONE
                    binding.btnShowAnswer.text = "Show Answer"
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionViewHolder {
        val binding = ItemQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionViewHolder, position: Int) {
        holder.bind(questions[position], position)
    }

    override fun getItemCount() = questions.size
}
