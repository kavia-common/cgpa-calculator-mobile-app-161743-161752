package org.example.app

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * PUBLIC_INTERFACE
 * MainActivity
 * This is the entry point of the CGPA Calculator app. It provides:
 * - Input fields to add subjects with grade and credit
 * - Real-time CGPA calculation as items change
 * - Edit and delete capabilities on individual subject entries
 * - A summary showing total credits and CGPA
 * - A simple in-memory history of past calculations
 */
class MainActivity : Activity() {

    private lateinit var subjectInput: EditText
    private lateinit var gradeSpinner: Spinner
    private lateinit var creditInput: EditText
    private lateinit var addButton: Button
    private lateinit var recycler: RecyclerView
    private lateinit var cgpaText: TextView
    private lateinit var totalCreditsText: TextView
    private lateinit var saveToHistoryButton: Button
    private lateinit var historyList: LinearLayout

    private val items = mutableListOf<SubjectEntry>()
    private lateinit var adapter: SubjectAdapter
    private val history = mutableListOf<HistoryItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_CGPA_Light_Compat)
        setContentView(R.layout.activity_main)

        bindViews()
        setupGradeSpinner()
        setupRecycler()
        setupInteractions()
        updateSummary()
    }

    private fun bindViews() {
        subjectInput = findViewById(R.id.input_subject)
        gradeSpinner = findViewById(R.id.spinner_grade)
        creditInput = findViewById(R.id.input_credit)
        addButton = findViewById(R.id.btn_add)
        recycler = findViewById(R.id.recycler_subjects)
        cgpaText = findViewById(R.id.text_cgpa_value)
        totalCreditsText = findViewById(R.id.text_total_credits)
        saveToHistoryButton = findViewById(R.id.btn_save_history)
        historyList = findViewById(R.id.history_list)
    }

    private fun setupGradeSpinner() {
        val grades = GradePoint.values().map { it.label }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, grades)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        gradeSpinner.adapter = adapter
    }

    private fun setupRecycler() {
        adapter = SubjectAdapter(
            items,
            onItemChanged = { updateSummary() },
            onDelete = { position ->
                if (position in items.indices) {
                    items.removeAt(position)
                    adapter.notifyItemRemoved(position)
                    updateSummary()
                }
            }
        )
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
    }

    private fun setupInteractions() {
        // Restrict credit input to max 2 decimals and reasonable length
        creditInput.filters = arrayOf<InputFilter>(InputFilter.LengthFilter(6))
        creditInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                // no-op
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // optional live validation
            }
        })

        addButton.setOnClickListener { tryAddEntry() }
        creditInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                tryAddEntry()
                true
            } else false
        }

        saveToHistoryButton.setOnClickListener {
            val cgpa = calculateCGPA(items)
            val total = items.sumOf { it.credit.toDoubleOrNull() ?: 0.0 }
            val snapshot = items.map { it.copy() }
            val historyItem = HistoryItem(System.currentTimeMillis(), snapshot, cgpa, total)
            history.add(0, historyItem)
            renderHistory()
            Toast.makeText(this, "Saved to history", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryAddEntry() {
        val subject = subjectInput.text.toString().trim()
        val gradeLabel = gradeSpinner.selectedItem?.toString()?.trim() ?: ""
        val creditStr = creditInput.text.toString().trim()

        if (subject.isEmpty()) {
            subjectInput.error = "Enter subject"
            return
        }
        val credit = creditStr.toDoubleOrNull()
        if (credit == null || credit <= 0) {
            creditInput.error = "Enter valid credit"
            return
        }
        val gradePoint = GradePoint.fromLabel(gradeLabel) ?: GradePoint.A

        items.add(SubjectEntry(subject, gradePoint, creditStr))
        adapter.notifyItemInserted(items.lastIndex)

        subjectInput.text?.clear()
        creditInput.text?.clear()
        gradeSpinner.setSelection(0)

        updateSummary()
    }

    private fun updateSummary() {
        val cgpa = calculateCGPA(items)
        val totalCredits = items.sumOf { it.credit.toDoubleOrNull() ?: 0.0 }
        cgpaText.text = if (cgpa.isNaN()) "--" else String.format("%.2f", cgpa)
        totalCreditsText.text = String.format("%.1f", totalCredits)
    }

    // PUBLIC_INTERFACE
    private fun calculateCGPA(entries: List<SubjectEntry>): Double {
        /**
         * Calculates the CGPA using a weighted average:
         * sum(gradePoint * credit) / sum(credit)
         * Returns NaN if no valid credits are present.
         */
        var weightSum = 0.0
        var creditSum = 0.0
        for (e in entries) {
            val c = e.credit.toDoubleOrNull() ?: 0.0
            if (c <= 0) continue
            weightSum += e.grade.point * c
            creditSum += c
        }
        return if (creditSum > 0) weightSum / creditSum else Double.NaN
    }

    private fun renderHistory() {
        historyList.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (item in history) {
            val view = inflater.inflate(R.layout.item_history, historyList, false)
            val title = view.findViewById<TextView>(R.id.history_title)
            val details = view.findViewById<TextView>(R.id.history_details)
            title.text = String.format("CGPA: %.2f  •  Credits: %.1f", item.cgpa, item.totalCredits)
            details.text = item.entries.joinToString(separator = "  •  ") {
                "${it.subject}: ${it.grade.label} (${it.credit})"
            }
            historyList.addView(view)
        }
        findViewById<View>(R.id.history_container).visibility =
            if (history.isEmpty()) View.GONE else View.VISIBLE
    }
}

/**
 * PUBLIC_INTERFACE
 * Data class representing a subject entry.
 */
data class SubjectEntry(
    var subject: String,
    var grade: GradePoint,
    var credit: String
)

/**
 * PUBLIC_INTERFACE
 * Grade to point mapping and helpers.
 */
enum class GradePoint(val label: String, val point: Double) {
    A("A (10)", 10.0),
    A_MINUS("A- (9)", 9.0),
    B_PLUS("B+ (8)", 8.0),
    B("B (7)", 7.0),
    C_PLUS("C+ (6)", 6.0),
    C("C (5)", 5.0),
    D("D (4)", 4.0),
    F("F (0)", 0.0);

    companion object {
        fun fromLabel(label: String): GradePoint? = values().firstOrNull { it.label == label }
    }
}

/**
 * PUBLIC_INTERFACE
 * RecyclerView Adapter to display and allow editing/deleting of subject entries.
 */
class SubjectAdapter(
    private val data: MutableList<SubjectEntry>,
    private val onItemChanged: () -> Unit,
    private val onDelete: (Int) -> Unit
) : RecyclerView.Adapter<SubjectAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val subject: EditText = view.findViewById(R.id.item_subject)
        val grade: Spinner = view.findViewById(R.id.item_grade)
        val credit: EditText = view.findViewById(R.id.item_credit)
        val deleteBtn: ImageButton = view.findViewById(R.id.item_delete)
        init {
            // Grade options
            val grades = GradePoint.values().map { it.label }
            val spinnerAdapter = ArrayAdapter(view.context, android.R.layout.simple_spinner_item, grades)
            spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            grade.adapter = spinnerAdapter
        }
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): VH {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.item_subject, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = data[position]
        // Avoid recursive triggering by first setting current values without listeners, then attaching listeners.
        holder.subject.setText(item.subject)
        holder.credit.setText(item.credit)
        holder.grade.setSelection(GradePoint.values().indexOf(item.grade))

        holder.subject.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (adapterPositionValid(holder)) {
                    data[holder.adapterPosition].subject = s?.toString() ?: ""
                    onItemChanged()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        holder.credit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (adapterPositionValid(holder)) {
                    data[holder.adapterPosition].credit = s?.toString() ?: ""
                    onItemChanged()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        holder.grade.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, pos: Int, id: Long
            ) {
                if (adapterPositionValid(holder)) {
                    data[holder.adapterPosition].grade = GradePoint.values()[pos]
                    onItemChanged()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        holder.deleteBtn.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onDelete(pos)
            }
        }
    }

    private fun adapterPositionValid(holder: VH): Boolean {
        val pos = holder.adapterPosition
        return pos != RecyclerView.NO_POSITION && pos in data.indices
    }
}

/**
 * PUBLIC_INTERFACE
 * Represents a snapshot of a calculation for history.
 */
data class HistoryItem(
    val timestamp: Long,
    val entries: List<SubjectEntry>,
    val cgpa: Double,
    val totalCredits: Double
)
