package com.physicslab.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.physicslab.app.data.AppDatabase
import com.physicslab.app.data.ReadingEntity
import com.physicslab.app.databinding.ActivityHistoryBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private var allReadings: List<ReadingEntity> = emptyList()
    private var currentFilterMs: Long? = null // null = all time

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.txtBack.setOnClickListener { finish() }

        adapter = HistoryAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        binding.filter1d.setOnClickListener { setFilter(TimeUnit.DAYS.toMillis(1)) }
        binding.filter1w.setOnClickListener { setFilter(TimeUnit.DAYS.toMillis(7)) }
        binding.filter1m.setOnClickListener { setFilter(TimeUnit.DAYS.toMillis(30)) }
        binding.filter3m.setOnClickListener { setFilter(TimeUnit.DAYS.toMillis(90)) }
        binding.filterAll.setOnClickListener { setFilter(null) }

        binding.btnClear.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                AppDatabase.getInstance(this@HistoryActivity).readingDao().clearAll()
            }
        }

        binding.txtExport.setOnClickListener { exportCsv() }

        lifecycleScope.launch {
            AppDatabase.getInstance(this@HistoryActivity).readingDao().getAll().collect { list ->
                allReadings = list
                applyFilter()
            }
        }
    }

    private fun setFilter(windowMs: Long?) {
        currentFilterMs = windowMs
        applyFilter()
    }

    private fun applyFilter() {
        val filtered = if (currentFilterMs == null) {
            allReadings
        } else {
            val cutoff = System.currentTimeMillis() - currentFilterMs!!
            allReadings.filter { it.timestamp >= cutoff }
        }
        adapter.submitList(filtered)
        binding.trendGraph.setValues(filtered.asReversed().map { it.value })
        binding.txtEmpty.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (filtered.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun exportCsv() {
        val filtered = if (currentFilterMs == null) allReadings else {
            val cutoff = System.currentTimeMillis() - currentFilterMs!!
            allReadings.filter { it.timestamp >= cutoff }
        }
        if (filtered.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            val dir = File(getExternalFilesDir(null), "exports").apply { mkdirs() }
            val fileName = "physicslab_readings_${System.currentTimeMillis()}.csv"
            val file = File(dir, fileName)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            file.bufferedWriter().use { writer ->
                writer.write("sensor_type,label,value,unit,timestamp,datetime\n")
                for (r in filtered) {
                    writer.write("${r.sensorType},${r.label},${r.value},${r.unit},${r.timestamp},${sdf.format(Date(r.timestamp))}\n")
                }
            }

            withContext(Dispatchers.Main) {
                val uri: Uri = FileProvider.getUriForFile(this@HistoryActivity, "$packageName.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(intent, "Export readings CSV"))
            }
        }
    }
}
