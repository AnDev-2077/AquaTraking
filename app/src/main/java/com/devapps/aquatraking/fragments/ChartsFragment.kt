package com.devapps.aquatraking.fragments

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.devapps.aquatraking.CustomMarkerView
import com.devapps.aquatraking.R
import com.devapps.aquatraking.databinding.FragmentChartsBinding
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class ChartsFragment : Fragment() {

    private var param1: String? = null
    private var param2: String? = null

    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!

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
        val dataEntries = listOf(
            Entry(0f, 60f),
            Entry(1f, 75f),
            Entry(2f, 40f),
            Entry(3f, 90f),
            Entry(4f, 20f),
            Entry(5f, 50f),
            Entry(6f, 85f),

        )
        val dataSet = LineDataSet(dataEntries, "")
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



        val lineData = LineData(dataSet)
        binding.lineChart.data = lineData

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

        binding.lineChart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
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