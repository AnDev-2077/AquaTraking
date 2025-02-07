package com.devapps.aquatraking.fragments

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.devapps.aquatraking.views.CustomMarkerView
import com.devapps.aquatraking.R
import com.devapps.aquatraking.databinding.FragmentChartsBinding
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.datepicker.DateValidatorPointBackward.before
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ChartsFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!

    private val moduleId = "-O9AOGhgVPLt464eEULY"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        actualizarGraficoConSemana(weekOffset = 0)

        binding.btnNextWeek.setOnClickListener {
            actualizarGraficoConSemana(weekOffset = 0)
            binding.tvWeek.text = "Semana actual"
        }
        binding.btnPreviousWeek.setOnClickListener {
            actualizarGraficoConSemana(weekOffset = -1)
            binding.tvWeek.text = "Semana anterior"
        }

    }

    private fun actualizarGraficoConSemana(weekOffset: Int) {
        val database = FirebaseDatabase.getInstance()
        val ref = database.getReference("ModulesWifi").child(moduleId)
        ref.get().addOnSuccessListener { dataSnapshot ->
            val entries = getEntriesForWeek(dataSnapshot, weekOffset)
            if (entries.isNotEmpty()) {
                val dataSet = crearDataSet(entries, "Porcentaje")
                val lineData = LineData(dataSet)
                binding.lineChart.data = lineData
                binding.lineChart.invalidate()
            } else {
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
                val lineData = LineData(defaultDataSet)
                binding.lineChart.data = lineData
                binding.lineChart.invalidate()
            }
        }.addOnFailureListener { exception ->
            // Maneja el error (por ejemplo, muestra un Toast o log)
            exception.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun crearDataSet(entries: List<Entry>, label: String): LineDataSet {
        val dataSet = LineDataSet(entries, label)
        dataSet.setDrawFilled(true)
        val gradientDrawable: Drawable? =
            ContextCompat.getDrawable(requireContext(), R.drawable.gradient_fill)
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

        //other customs
        binding.lineChart.description.isEnabled = false
        binding.lineChart.legend.isEnabled = false
        val markerView = CustomMarkerView(requireContext())
        markerView.chartView = binding.lineChart
        binding.lineChart.marker = markerView

        //setup X Axis
        val xAxis = binding.lineChart.xAxis
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = XAxisValueFormatter()
        xAxis.labelCount = 7
        xAxis.granularity = 1f
        xAxis.axisMinimum = 0f
        xAxis.axisMaximum = 6f

        //setup Y Axis
        val yAxisLeft = binding.lineChart.axisLeft
        yAxisLeft.axisMinimum = 0f
        yAxisLeft.axisMaximum = 100f
        yAxisLeft.granularity = 25f
        yAxisLeft.enableGridDashedLine(10f,10f,10f)
        yAxisLeft.setDrawGridLines(true)

        //disable right Y Axis
        val yAxisRight = binding.lineChart.axisRight
        yAxisRight.isEnabled = false

        //disable zoom
        binding.lineChart.setScaleEnabled(false)
        binding.lineChart.setPinchZoom(false)

        val lineData = LineData(dataSet)
        binding.lineChart.data = lineData
        binding.lineChart.invalidate()

        //enable animations
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

        // Configurar calendario con locale apropiado (Lunes como primer día)
        val calendarNow = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.SUNDAY
            add(Calendar.WEEK_OF_YEAR, weekOffset)
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        }

        // Obtener fecha de inicio y fin de la semana
        val startOfWeek = calendarNow.clone() as Calendar
        val endOfWeek = calendarNow.clone() as Calendar
        endOfWeek.add(Calendar.DAY_OF_WEEK, 7)

        // Recorrer registros
        for (recordSnapshot in dataSnapshot.children) {
            val fechaString = recordSnapshot.child("fecha").getValue(String::class.java)
            val porcentajeStr = recordSnapshot.child("porcentaje").getValue(String::class.java)

            if (!fechaString.isNullOrEmpty() && !porcentajeStr.isNullOrEmpty()) {
                try {
                    val date = sdf.parse(fechaString)
                    val recordCalendar = Calendar.getInstance().apply { time = date }

                    // Verificar si la fecha está dentro del rango de la semana
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

        // Ordenar y completar días faltantes
        return (0..6).map { x ->
            entries.firstOrNull { it.x == x.toFloat() } ?: Entry(x.toFloat(), 0f)
        }.sortedBy { it.x }
    }

    companion object {

        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            ChartsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}