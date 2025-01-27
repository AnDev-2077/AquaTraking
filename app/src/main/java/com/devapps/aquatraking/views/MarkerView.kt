package com.devapps.aquatraking.views

import android.content.Context
import android.view.LayoutInflater
import com.devapps.aquatraking.R
import com.devapps.aquatraking.databinding.ItemMarkerViewBinding
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF

class CustomMarkerView (context: Context) : MarkerView(context, R.layout.item_marker_view){
    private val binding: ItemMarkerViewBinding

    init {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        binding = ItemMarkerViewBinding.inflate(inflater, this, true)
    }

    private val daysOfWeek = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        if (e != null) {
            val day = daysOfWeek.getOrNull(e.x.toInt()) ?: "Día desconocido"
            val value = e.y.toInt()
            binding.tvMarker.text = "$day: $value%"
        }
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF(-(width / 2).toFloat(), -height.toFloat())
    }

}
