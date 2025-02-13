package com.devapps.aquatraking.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import com.devapps.aquatraking.views.CustomMarkerView
import com.devapps.aquatraking.R
import com.devapps.aquatraking.databinding.FragmentChartsBinding
import com.devapps.aquatraking.services.ViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ChartsFragment : Fragment() {

    private lateinit var binding: FragmentChartsBinding
    private var currentWeekOffset = 0

    private val tankViewModel: ViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tankViewModel.selectedKey.observe(viewLifecycleOwner) { key ->
            key?.let { loadChartData(it, currentWeekOffset) } ?: showDefaultValues()
        }

        setupWeekNavigation()
    }

    private fun setupWeekNavigation() {
        binding.btnNextWeek.setOnClickListener {
            if (currentWeekOffset > 0) {
                currentWeekOffset--
                updateChartForCurrentKey()
                updateWeekDisplay()
            }
        }

        binding.btnPreviousWeek.setOnClickListener {
            if (currentWeekOffset < 4) {
                currentWeekOffset++
                updateChartForCurrentKey()
                updateWeekDisplay()
            }
        }

        updateWeekDisplay()
    }

    private fun updateChartForCurrentKey() {
        tankViewModel.selectedKey.value?.let { key ->
            loadChartData(key, currentWeekOffset)
        } ?: showDefaultValues()
    }

    private fun loadChartData(key: String, weekOffset: Int) {
        val ref = FirebaseDatabase.getInstance().getReference("ModulesWifi/$key")
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

    private fun updateWeekDisplay() {
        binding.tvWeek.text = when (currentWeekOffset) {
            0 -> "Semana actual"
            1 -> "Semana anterior"
            else -> "Hace $currentWeekOffset semanas"
        }
        binding.btnNextWeek.isEnabled = currentWeekOffset > 0
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
        xAxis.textColor = resources.getColor(R.color.chart_text_primary, null)
        xAxis.axisLineColor = resources.getColor(R.color.chart_text_primary, null)
        xAxis.axisLineWidth = 4f

        val yAxisLeft = binding.lineChart.axisLeft
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.axisMaximum = 100f
        yAxisLeft.granularity = 25f
        yAxisLeft.enableGridDashedLine(10f, 10f, 10f)
        yAxisLeft.setDrawGridLines(true)
        yAxisLeft.textColor = resources.getColor(R.color.chart_text_primary, null)
        yAxisLeft.axisLineColor = resources.getColor(R.color.chart_text_primary, null)
        yAxisLeft.axisLineWidth = 4f
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
        // Opcional: asegurarse de usar la zona horaria del dispositivo
        sdf.timeZone = Calendar.getInstance().timeZone

        val entries = mutableListOf<Entry>()

        // Inicio de semana: lunes a las 00:00:00.000
        val startOfWeek = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            add(Calendar.WEEK_OF_YEAR, -weekOffset)
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Fin de semana: domingo a las 23:59:59.999
        val endOfWeek = (startOfWeek.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, 6)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        Log.d("ChartsFragment", "Rango de la semana: ${sdf.format(startOfWeek.time)} - ${sdf.format(endOfWeek.time)}")

        dataSnapshot.children.forEach { recordSnapshot ->
            val fechaString = recordSnapshot.child("fecha").getValue(String::class.java)
            val porcentajeStr = recordSnapshot.child("porcentaje").getValue(String::class.java)

            if (!fechaString.isNullOrEmpty() && !porcentajeStr.isNullOrEmpty()) {
                try {
                    val fecha = sdf.parse(fechaString)
                    val calendarFecha = Calendar.getInstance().apply {
                        time = fecha
                        set(Calendar.MILLISECOND, 0)
                    }

                    // Verificar si la fecha está dentro del rango
                    if (!calendarFecha.before(startOfWeek) && !calendarFecha.after(endOfWeek)) {
                        val xValue = when (calendarFecha.get(Calendar.DAY_OF_WEEK)) {
                            Calendar.MONDAY -> 0f
                            Calendar.TUESDAY -> 1f
                            Calendar.WEDNESDAY -> 2f
                            Calendar.THURSDAY -> 3f
                            Calendar.FRIDAY -> 4f
                            Calendar.SATURDAY -> 5f
                            Calendar.SUNDAY -> 6f
                            else -> -1f
                        }
                        val porcentaje = porcentajeStr.toFloatOrNull() ?: 0f
                        entries.add(Entry(xValue, porcentaje))
                        Log.d("ChartsFragment", "Agregado -> Fecha: $fechaString, Día: ${calendarFecha.get(Calendar.DAY_OF_WEEK)}, xValue: $xValue, Porcentaje: $porcentaje")
                    }
                } catch (e: Exception) {
                    Log.e("ChartsFragment", "Error al parsear fecha: ${e.message}")
                }
            }
        }

        // Rellenar días faltantes con 0
        return (0..6).map { day ->
            entries.firstOrNull { it.x == day.toFloat() } ?: Entry(day.toFloat(), 0f)
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