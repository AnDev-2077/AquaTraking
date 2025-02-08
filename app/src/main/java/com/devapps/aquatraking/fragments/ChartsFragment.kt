package com.devapps.aquatraking.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.core.content.ContextCompat
import com.devapps.aquatraking.views.CustomMarkerView
import com.devapps.aquatraking.R
import com.devapps.aquatraking.databinding.FragmentChartsBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ChartsFragment : Fragment() {

    private lateinit var binding: FragmentChartsBinding
    private lateinit var spinnerModules: Spinner
    private var modulesList: MutableList<String> = mutableListOf()
    private lateinit var database: FirebaseDatabase
    private var currentWeekOffset = 0 // Semana actual por defecto
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerModules = binding.spinnerModules

        if (userId != null) {
            Log.d("ChartsFragment", "El usuario actual es: $userId")
            getModulesForUser(userId)
        } else {
            Log.e("ChartsFragment", "No se ha encontrado un usuario autenticado")
            return
        }

        // Configurar listeners de los botones
        binding.btnNextWeek.setOnClickListener {
            if (currentWeekOffset > 0) { // No permitir valores negativos
                currentWeekOffset--
                actualizarGraficoConSemana(currentWeekOffset)
                updateWeekDisplay()
            }
        }

        binding.btnPreviousWeek.setOnClickListener {
            if (currentWeekOffset < 4) { // Limitar el retroceso a 4 semanas
                currentWeekOffset++
                actualizarGraficoConSemana(currentWeekOffset)
                updateWeekDisplay()
            }
        }

        updateWeekDisplay()
    }

    private fun getModulesForUser(userId: String) {
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)
        userRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val modules = documentSnapshot.get("modules") as? List<String>
                    if (modules != null && modules.isNotEmpty()) {
                        modulesList.addAll(modules)
                        setupSpinner()
                    } else {
                        Log.d("ChartsFragment", "No se encontraron módulos para el usuario")
                    }
                } else {
                    Log.d("ChartsFragment", "El documento del usuario no existe")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ChartsFragment", "Error al obtener el documento del usuario: ${exception.message}")
            }
    }

    private fun setupSpinner() {
        if (modulesList.size > 1) {
            spinnerModules.visibility = View.VISIBLE
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, modulesList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerModules.adapter = adapter

            spinnerModules.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedModule = modulesList[position]
                    updateDatabaseReference(selectedModule)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                }
            }
        } else if (modulesList.size == 1) {
            spinnerModules.visibility = View.GONE
            updateDatabaseReference(modulesList[0])
        }
    }

    private fun updateDatabaseReference(moduleId: String) {
        database = FirebaseDatabase.getInstance()
        actualizarGraficoConSemana(currentWeekOffset)
    }

    private fun actualizarGraficoConSemana(weekOffset: Int) {
        if (modulesList.isNotEmpty()) {
            val ref = database.getReference("ModulesWifi").child(modulesList[0])
            ref.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val entries = getEntriesForWeek(dataSnapshot, weekOffset)
                    if (entries.isNotEmpty()) {
                        val dataSet = crearDataSet(entries, "Porcentaje")
                        binding.lineChart.data = LineData(dataSet)
                        binding.lineChart.invalidate()
                    } else {
                        showDefaultValues()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("ChartsFragment", "Error al leer datos: ${databaseError.message}")
                }
            })
        }
    }

    private fun updateWeekDisplay() {
        binding.tvWeek.text = when (currentWeekOffset) {
            0 -> "Semana actual"
            1 -> "Semana anterior"
            else -> "Hace $currentWeekOffset semanas"
        }
        binding.btnNextWeek.isEnabled = currentWeekOffset > 0 // Deshabilitar si es la semana actual
    }

    private fun showDefaultValues() {
        val defaultEntries = listOf(
            Entry(0f, 0f),
            Entry(1f, 0f),
            Entry(2f, 0f),
            Entry(3f, 0f),
            Entry(4f, 0f),
            Entry(5f, 0f),
            Entry(6f, 0f)
        )
        val defaultDataSet = crearDataSet(defaultEntries, "Porcentaje predeterminado")
        binding.lineChart.data = LineData(defaultDataSet)
        binding.lineChart.invalidate()
    }

    private fun crearDataSet(entries: List<Entry>, label: String): LineDataSet {
        val dataSet = LineDataSet(entries, label)
        dataSet.setDrawFilled(true)
        val gradientDrawable: Drawable? = ContextCompat.getDrawable(requireContext(), R.drawable.gradient_fill)
        dataSet.fillDrawable = gradientDrawable
        dataSet.color = resources.getColor(R.color.blue, null)
        dataSet.valueTextColor = resources.getColor(R.color.black, null)
        dataSet.setDrawHorizontalHighlightIndicator(false)
        dataSet.setDrawVerticalHighlightIndicator(true)
        dataSet.enableDashedHighlightLine(10f, 5f, 0f)
        dataSet.highlightLineWidth = 1.5f
        dataSet.highLightColor = resources.getColor(R.color.gray, null)
        dataSet.setDrawCircles(true)
        dataSet.setCircleColor(resources.getColor(R.color.blue, null))
        dataSet.circleHoleColor = resources.getColor(R.color.blue, null)
        dataSet.setDrawValues(false)
        dataSet.lineWidth = 2f
        dataSet.circleRadius = 6f

        binding.lineChart.description.isEnabled = false
        binding.lineChart.legend.isEnabled = false
        val markerView = CustomMarkerView(requireContext())
        markerView.chartView = binding.lineChart
        binding.lineChart.marker = markerView

        val xAxis = binding.lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = XAxisValueFormatter()
        xAxis.labelCount = 7
        xAxis.granularity = 1f
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 6f

        val yAxisLeft = binding.lineChart.axisLeft
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.axisMaximum = 100f
        yAxisLeft.granularity = 25f
        yAxisLeft.enableGridDashedLine(10f, 10f, 10f)
        yAxisLeft.setDrawGridLines(true)

        val yAxisRight = binding.lineChart.axisRight
        yAxisRight.isEnabled = false

        binding.lineChart.setScaleEnabled(false)
        binding.lineChart.setPinchZoom(false)

        binding.lineChart.animateY(1000, Easing.EaseInCubic)

        return dataSet
    }

    class XAxisValueFormatter : com.github.mikephil.charting.formatter.ValueFormatter() {
        override fun getFormattedValue(value: Float): String {
            return when (value) {
                0f -> "L"
                1f -> "M"
                2f -> "X"
                3f -> "J"
                4f -> "V"
                5f -> "S"
                6f -> "D"
                else -> ""
            }
        }
    }

    private fun getEntriesForWeek(dataSnapshot: DataSnapshot, weekOffset: Int): List<Entry> {
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val entries = mutableListOf<Entry>()

        val calendarNow = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            add(Calendar.WEEK_OF_YEAR, -weekOffset) // Ajustar para semanas anteriores
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }

        val startOfWeek = calendarNow.clone() as Calendar
        val endOfWeek = calendarNow.clone() as Calendar
        endOfWeek.add(Calendar.DAY_OF_WEEK, 7)

        for (recordSnapshot in dataSnapshot.children) {
            val fechaString = recordSnapshot.child("fecha").getValue(String::class.java)
            val porcentajeStr = recordSnapshot.child("porcentaje").getValue(String::class.java)

            if (!fechaString.isNullOrEmpty() && !porcentajeStr.isNullOrEmpty()) {
                try {
                    val date = sdf.parse(fechaString)
                    val recordCalendar = Calendar.getInstance().apply { time = date }

                    if (!recordCalendar.before(startOfWeek) && !recordCalendar.after(endOfWeek)) {
                        val xValue = when (recordCalendar.get(Calendar.DAY_OF_WEEK)) {
                            Calendar.MONDAY -> 0f
                            Calendar.TUESDAY -> 1f
                            Calendar.WEDNESDAY -> 2f
                            Calendar.THURSDAY -> 3f
                            Calendar.FRIDAY -> 4f
                            Calendar.SATURDAY -> 5f
                            Calendar.SUNDAY -> 6f
                            else -> 0f
                        }
                        val porcentaje = porcentajeStr.toFloatOrNull() ?: 0f
                        entries.add(Entry(xValue, porcentaje))
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        return (0..6).map { x ->
            entries.firstOrNull { it.x == x.toFloat() } ?: Entry(x.toFloat(), 0f)
        }.sortedBy { it.x }
    }

    override fun onDestroyView() {
        super.onDestroyView()
    }

    companion object {
        @JvmStatic
        fun newInstance() = ChartsFragment()
    }
}