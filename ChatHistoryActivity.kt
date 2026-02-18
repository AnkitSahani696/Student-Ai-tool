package com.example.firebase

import com.example.firebase.ChatEntity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.firebase.R

import com.example.firebase.databinding.ActivityChatHistoryBinding

import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatHistoryBinding
    private lateinit var database: ChatDatabase
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        binding = ActivityChatHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()

        database = ChatDatabase.getDatabase(this)

        loadHistory()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chat History"
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        historyAdapter = HistoryAdapter { chatEntity ->
            // Handle delete click
            lifecycleScope.launch {
                database.chatDao().deleteMessage(chatEntity)
                Toast.makeText(this@ChatHistoryActivity, "Deleted", Toast.LENGTH_SHORT).show()
            }
        }

        binding.rvHistory.apply {
            layoutManager = LinearLayoutManager(this@ChatHistoryActivity)
            adapter = historyAdapter
        }
    }

    private fun loadHistory() {
        lifecycleScope.launch {
            database.chatDao().getAllMessages().collectLatest { messages ->
                historyAdapter.submitList(messages)

                if (messages.isEmpty()) {
                    binding.tvEmptyState.visibility = android.view.View.VISIBLE
                    binding.rvHistory.visibility = android.view.View.GONE
                } else {
                    binding.tvEmptyState.visibility = android.view.View.GONE
                    binding.rvHistory.visibility = android.view.View.VISIBLE
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.history_menu, menu)

        // Setup search
        val searchItem = menu?.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                searchHistory(newText ?: "")
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_clear_all -> {
                showClearAllDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun searchHistory(query: String) {
        lifecycleScope.launch {
            if (query.isEmpty()) {
                loadHistory()
            } else {
                database.chatDao().searchMessages(query).collectLatest { messages ->
                    historyAdapter.submitList(messages)
                }
            }
        }
    }

    private fun showClearAllDialog() {
        AlertDialog.Builder(this)
            .setTitle("Clear All History?")
            .setMessage("सभी chats permanently delete हो जाएंगे। क्या आप sure हैं?")
            .setPositiveButton("Yes") { _, _ ->
                lifecycleScope.launch {
                    database.chatDao().deleteAllMessages()
                    Toast.makeText(this@ChatHistoryActivity, "All history cleared", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}