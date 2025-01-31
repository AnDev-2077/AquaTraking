package com.devapps.aquatraking.fragments

import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.devapps.aquatraking.databinding.FragmentHomeBinding
import com.devapps.aquatraking.services.ForegroundService
import com.devapps.aquatraking.views.WaveLoadView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.Locale

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class HomeFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private lateinit var binding: FragmentHomeBinding
    private var waveView: WaveLoadView? = null
    private lateinit var database: DatabaseReference
    private var currentDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

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
    ): View {
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeFragment", "onViewCreated called")

        val serviceIntent = Intent(requireContext(), ForegroundService::class.java)
        requireContext().startService(serviceIntent)

        database = FirebaseDatabase.getInstance().reference
        initFirebaseListener()

        binding.btnNext.setOnClickListener {
            changeDate(1)
        }

        binding.btnPrevious.setOnClickListener{
            changeDate(-1)
        }
    }

    private fun initViews() {
        waveView = binding.waveView
    }

    private fun initFirebaseListener() {

        val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", AppCompatActivity.MODE_PRIVATE)
        val moduleKey = sharedPreferences.getString("moduleKey", null)

        Log.d("HomeFragment", "initFirebaseListener called")
        database = FirebaseDatabase.getInstance().getReference("/ModulesWifi/$moduleKey")

        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                updateProgressBasedOnDate(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                updateProgressBasedOnDate(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Error al leer datos: ${error.message}")
            }
        })
    }

    private fun sendForegroundService(porcentaje: Float){
        val intent = Intent(requireContext(), ForegroundService::class.java)
        intent.putExtra("porcentaje", porcentaje)
        requireContext().startService(intent)
    }

    private fun updateProgressBasedOnDate(snapshot: DataSnapshot) {
        val selectedDate = dateFormat.format(currentDate.time)
        val fecha = snapshot.child("fecha").getValue(String::class.java)
        val porcentaje = snapshot.child("porcentaje").getValue(String::class.java)?.toFloatOrNull()

        Log.d("HomeFragment", "Datos obtenidos - Fecha: $fecha, Porcentaje: $porcentaje")

        if (fecha == selectedDate && porcentaje != null) {
            initViews()
            waveView?.setProgress(porcentaje)
            Log.d("HomeFragment", "Porcentaje actualizado ($selectedDate): $porcentaje%")
        } else {
            Log.e("HomeFragment", "No hay datos para la fecha: $selectedDate o el porcentaje es nulo")
        }
    }

    private fun updateDateTex(){
        val formattedDate = when {
            isToday() -> "Hoy"
            isYesterday() -> "Ayer"
            else -> dateFormat.format(currentDate.time)
        }
        binding.tvCurrentDate.text = formattedDate
        Log.d("HomeFragment", "Fecha actualizada: $formattedDate")
    }

    private fun changeDate(days: Int){
        val newDate = Calendar.getInstance().apply {
            time = currentDate.time
            add(Calendar.DAY_OF_MONTH, days)
        }

        val today = Calendar.getInstance()
        val threeDaysAgo = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -3)
        }

        if (newDate.before(threeDaysAgo) || newDate.after(today)) {
            Log.d("HomeFragment", "La fecha no puede ser más de dos días antes o después de hoy")
            return
        }

        currentDate.add(Calendar.DAY_OF_MONTH, days)
        updateDateTex()
    }

    private fun isToday(): Boolean{
        val today = Calendar.getInstance()
        return today.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == currentDate.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(): Boolean {
        val yesterday = Calendar.getInstance()
        yesterday.add(Calendar.DAY_OF_MONTH, -1)
        return yesterday.get(Calendar.YEAR) == currentDate.get(Calendar.YEAR) &&
                yesterday.get(Calendar.DAY_OF_YEAR) == currentDate.get(Calendar.DAY_OF_YEAR)
    }


    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            HomeFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}