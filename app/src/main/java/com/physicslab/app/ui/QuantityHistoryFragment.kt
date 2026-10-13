package com.physicslab.app.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.physicslab.app.R
import com.physicslab.app.data.SensorReading
import com.physicslab.app.databinding.FragmentQuantityHistoryBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * History tab for a single physical quantity.
 *
 * Features:
 * - RecyclerView with [ReadingAdapter] (stable item IDs via diff callback)
 * - Debounced search (250 ms) dispatched via [QuantityViewModel.setSearchQuery]
 * - Two distinct empty states: "no readings saved yet" vs "no results for this search"
 * - Tap on a row opens [ReadingDetailBottomSheet] (name, date/time, values, sensor info,
 *   sample count, physical interpretation)
 * - Rename dialog with trim + max-60 + non-blank validation, Snackbar feedback
 * - Delete with confirmation dialog + Snackbar with working Undo
 * - Clear All with confirmation + Snackbar feedback
 */
class QuantityHistoryFragment : Fragment() {

    private var _binding: FragmentQuantityHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: QuantityViewModel by lazy {
        (requireParentFragment() as QuantityFragment).viewModel
    }

    private lateinit var adapter: ReadingAdapter

    /** Coroutine job that debounces search input. */
    private var searchDebounceJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuantityHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ReadingAdapter(
            onItemClick   = { reading -> showDetailSheet(reading) },
            onRenameClick = { reading -> showRenameDialog(reading) },
            onDeleteClick = { reading -> showDeleteConfirmDialog(reading) }
        )

        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = adapter

        // Restore query if saved across process death
        val currentQuery = viewModel.searchQuery.value
        if (currentQuery.isNotEmpty()) {
            binding.etHistorySearch.setText(currentQuery)
            binding.btnClearSearch.visibility = View.VISIBLE
        }

        // --- Debounced search (250 ms) ---
        binding.etHistorySearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString().orEmpty()
                binding.btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                searchDebounceJob?.cancel()
                searchDebounceJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(250)
                    viewModel.setSearchQuery(query)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etHistorySearch.text?.clear()
        }

        binding.btnClearHistory.setOnClickListener {
            showClearAllConfirmDialog()
        }

        // "Go to Live" button in empty state
        binding.btnGoToLive.setOnClickListener {
            (parentFragment as? QuantityFragment)?.selectTab(0)
        }

        // Observe history state (discriminates between the two empty states)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.historyState.collectLatest { state ->
                    when (state) {
                        is HistoryUiState.NoSearch -> {
                            val list = state.readings
                            adapter.submitList(list)
                            updateCountLabel(list.size)
                            when {
                                list.isEmpty() -> {
                                    binding.rvHistory.visibility = View.GONE
                                    binding.layoutHistoryEmpty.visibility = View.VISIBLE
                                    binding.layoutSearchEmpty.visibility = View.GONE
                                }
                                else -> {
                                    binding.rvHistory.visibility = View.VISIBLE
                                    binding.layoutHistoryEmpty.visibility = View.GONE
                                    binding.layoutSearchEmpty.visibility = View.GONE
                                }
                            }
                        }
                        is HistoryUiState.Searching -> {
                            val list = state.readings
                            adapter.submitList(list)
                            updateCountLabel(list.size)
                            when {
                                list.isEmpty() -> {
                                    binding.rvHistory.visibility = View.GONE
                                    binding.layoutHistoryEmpty.visibility = View.GONE
                                    binding.layoutSearchEmpty.visibility = View.VISIBLE
                                    binding.tvSearchEmptyQuery.text =
                                        "No readings match \"${state.query}\"."
                                }
                                else -> {
                                    binding.rvHistory.visibility = View.VISIBLE
                                    binding.layoutHistoryEmpty.visibility = View.GONE
                                    binding.layoutSearchEmpty.visibility = View.GONE
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateCountLabel(count: Int) {
        binding.tvHistoryCount.text = if (count == 1) "1 reading" else "$count readings"
    }

    // ------------------------------------------------------------------------------------
    // Detail bottom sheet
    // ------------------------------------------------------------------------------------

    private fun showDetailSheet(reading: SensorReading) {
        ReadingDetailBottomSheet.show(
            reading = reading,
            fragmentManager = childFragmentManager,
            onRename = { showRenameDialog(it) },
            onDelete = { showDeleteConfirmDialog(it) }
        )
    }

    // ------------------------------------------------------------------------------------
    // Rename
    // ------------------------------------------------------------------------------------

    private fun showRenameDialog(reading: SensorReading) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_edit_reading, null)
        val etName    = dialogView.findViewById<TextInputEditText>(R.id.etReadingName)
        val til       = dialogView.findViewById<TextInputLayout>(R.id.tilReadingName)
        val tvTitle   = dialogView.findViewById<android.widget.TextView>(R.id.tvEditDialogTitle)
        val tvSub     = dialogView.findViewById<android.widget.TextView>(R.id.tvEditDialogSubtitle)
        val btnCancel  = dialogView.findViewById<View>(R.id.btnDialogCancel)
        val btnConfirm = dialogView.findViewById<View>(R.id.btnDialogConfirm)

        tvTitle.text = "Rename Reading"
        tvSub.text   = "Enter a new name (max 60 characters)."
        etName.setText(reading.name)
        etName.setSelection(reading.name.length)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnConfirm.setOnClickListener {
            val newName = etName.text?.toString()?.trim().orEmpty()
            when {
                newName.isBlank() -> {
                    til.error = "Name cannot be empty"
                    return@setOnClickListener
                }
                newName.length > 60 -> {
                    til.error = "Name must be 60 characters or fewer"
                    return@setOnClickListener
                }
                else -> til.error = null
            }

            viewLifecycleOwner.lifecycleScope.launch {
                val success = viewModel.renameReading(reading.id, newName)
                if (success) {
                    dialog.dismiss()
                    showSnackbar("Renamed to \"$newName\"")
                } else {
                    til.error = "Failed to rename — reading may no longer exist"
                }
            }
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    // ------------------------------------------------------------------------------------
    // Delete with Undo
    // ------------------------------------------------------------------------------------

    private fun showDeleteConfirmDialog(reading: SensorReading) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Reading")
            .setMessage("Delete \"${reading.name}\"? You can undo this.")
            .setPositiveButton("Delete") { _, _ -> performDelete(reading) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performDelete(reading: SensorReading) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.deleteReading(reading.id)
        }

        // Show Snackbar with Undo — the record is already soft-deleted from DB;
        // Undo re-inserts a copy with the same data but a new auto-generated ID.
        val snackbar = Snackbar.make(
            binding.root,
            "\"${reading.name}\" deleted",
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction("Undo") {
            viewLifecycleOwner.lifecycleScope.launch {
                val newId = viewModel.restoreReading(reading)
                if (newId > 0) {
                    showSnackbar("Restored \"${reading.name}\"")
                } else {
                    showSnackbar("Could not restore reading")
                }
            }
        }
        snackbar.setActionTextColor(
            androidx.core.content.ContextCompat.getColor(requireContext(), com.physicslab.app.R.color.lab_primary)
        )
        snackbar.show()
    }

    // ------------------------------------------------------------------------------------
    // Clear All
    // ------------------------------------------------------------------------------------

    private fun showClearAllConfirmDialog() {
        val quantity = viewModel.quantity
        AlertDialog.Builder(requireContext())
            .setTitle("Clear ${quantity.displayName} History")
            .setMessage("This will permanently delete all saved ${quantity.displayName} readings. This cannot be undone.")
            .setPositiveButton("Clear All") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    viewModel.deleteAllHistory()
                    showSnackbar("All ${quantity.displayName} readings cleared")
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ------------------------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------------------------

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        searchDebounceJob?.cancel()
        super.onDestroyView()
        _binding = null
    }
}
