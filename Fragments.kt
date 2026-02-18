package com.example.firebase
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.firebase.databinding.FragmentQuestionsListBinding


// ViewPager Adapter
class QuestionsPagerAdapter(
    fragmentActivity: FragmentActivity,
    private val pdfId: Long
) : FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = 3

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> QuestionsListFragment.newInstance(pdfId, "short")
            1 -> QuestionsListFragment.newInstance(pdfId, "long")
            2 -> QuestionsListFragment.newInstance(pdfId, "mcq")
            else -> QuestionsListFragment.newInstance(pdfId, "short")
        }
    }
}

// Questions List Fragment
class QuestionsListFragment : Fragment() {

    private var _binding: FragmentQuestionsListBinding? = null
    private val binding get() = _binding!!

    private var pdfId: Long = -1
    private var questionType: String = "short"

    companion object {
        private const val ARG_PDF_ID = "pdf_id"
        private const val ARG_TYPE = "type"

        fun newInstance(pdfId: Long, type: String): QuestionsListFragment {
            val fragment = QuestionsListFragment()
            val args = Bundle()
            args.putLong(ARG_PDF_ID, pdfId)
            args.putString(ARG_TYPE, type)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pdfId = it.getLong(ARG_PDF_ID)
            questionType = it.getString(ARG_TYPE) ?: "short"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuestionsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadQuestions()
    }

    private fun loadQuestions() {
        val dbHelper = DatabaseHelper(requireContext())
        val questions = dbHelper.getQuestionsByType(pdfId, questionType)

        if (questions.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.rvQuestions.visibility = View.GONE

            binding.tvEmptyState.text = when (questionType) {
                "short" -> "Koi short questions nahi mile"
                "long" -> "Koi long questions nahi mile"
                "mcq" -> "Koi MCQs nahi mile"
                else -> "Koi questions nahi mile"
            }
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.rvQuestions.visibility = View.VISIBLE

            val adapter = QuestionsAdapter(questions)
            binding.rvQuestions.apply {
                layoutManager = LinearLayoutManager(requireContext())
                this.adapter = adapter
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}