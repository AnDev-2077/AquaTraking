package com.devapps.aquatraking.fragments

import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.devapps.aquatraking.databinding.FragmentHomeBinding
import com.devapps.aquatraking.services.ForegroundService
import com.devapps.aquatraking.services.ViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Locale

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class HomeFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private val tankViewModel: ViewModel by activityViewModels()

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var currentDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var consumoListener: ChildEventListener? = null
    private var consumoRef: DatabaseReference? = null

    private val maxDaysBack = 5
    private var currentOffset = 0
    private var capacidadLitros: Double = 0.0
    private var ultimoPorcentaje: Float? = null

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
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeFragment", "onViewCreated called")
        tankViewModel.selectedKey.observe(viewLifecycleOwner) { key ->
            key?.let { loadTankData(it) } ?: showDefaultData()
        }
        val serviceIntent = Intent(requireContext(), ForegroundService::class.java)
        requireContext().startService(serviceIntent)
        binding.tvDate.text = dateFormat.format(currentDate.time)
        actualizarFechaDisplay()
        binding.btnPrevious.setOnClickListener {
            if (currentOffset < maxDaysBack) {
                currentOffset++
                actualizarFechaDisplay()
                actualizarConsumoPorDia()
            }
        }
        binding.btnNext.setOnClickListener {
            if (currentOffset > 0) {
                currentOffset--
                actualizarFechaDisplay()
                actualizarConsumoPorDia()
            }
        }
        actualizarConsumoPorDia()
    }

    private fun loadTankData(key: String) {
        consumoListener?.let { consumoRef?.removeEventListener(it) }

        FirebaseFirestore.getInstance().collection("modules").document(key)
            .get()
            .addOnSuccessListener { doc ->
                capacidadLitros = doc.getDouble("capacidadLitros") ?: 0.0
                // Si RTDB ya entregó el porcentaje antes que Firestore, recalcular tvVolume ahora
                ultimoPorcentaje?.let { actualizarVolumen(it) }
            }

        val ref = FirebaseDatabase.getInstance().getReference("ModulesWifi/$key")
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                actualizarDatos(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                actualizarDatos(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                Log.d("HomeFragment", "Dato eliminado: ${snapshot.key}")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Error al leer datos: ${error.message}")
            }
        }
        ref.addChildEventListener(listener)
        consumoListener = listener
        consumoRef = ref
    }

    private fun actualizarDatos(snapshot: DataSnapshot) {
        val fecha = snapshot.child("fecha").value?.toString() ?: return
        val targetDate = Calendar.getInstance().apply { add(Calendar.DATE, -currentOffset) }
        val fechaFormateada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(targetDate.time)
        if (fecha == fechaFormateada) {
            updateWaveView(snapshot)
        }
    }

    private fun showDefaultData() {
        ultimoPorcentaje = null
        _binding?.waveView?.setProgress(0f)
        _binding?.tvPercentage?.text = "0%"
        _binding?.tvVolume?.text = "-- L"
    }

    private fun actualizarVolumen(porcentaje: Float) {
        val litros = if (capacidadLitros > 0) (porcentaje / 100f) * capacidadLitros else null
        _binding?.tvVolume?.text = if (litros != null) String.format("%.2f L", litros) else "-- L"
    }

    private fun actualizarFechaDisplay() {
        val calendar = Calendar.getInstance().apply {
            add(Calendar.DATE, -currentOffset)
        }

        binding.tvDate.text = when (currentOffset) {
            0 -> "Hoy"
            1 -> "Ayer"
            else -> SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(calendar.time)
        }

        binding.btnPrevious.isEnabled = currentOffset < maxDaysBack
        binding.btnNext.isEnabled = currentOffset > 0
    }

    private fun actualizarConsumoPorDia() {
        val targetDate = Calendar.getInstance().apply {
            add(Calendar.DATE, -currentOffset)
        }
        val fechaFormateada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(targetDate.time)

        val ref = FirebaseDatabase.getInstance().getReference("ModulesWifi/${tankViewModel.selectedKey.value}")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (childSnapshot in snapshot.children) {
                        val fecha = childSnapshot.child("fecha").getValue(String::class.java)
                        if (fecha == fechaFormateada) {
                            updateWaveView(childSnapshot)
                            return
                        }
                    }
                    Log.e("HomeFragment", "No se encontraron datos para la fecha: $fechaFormateada")
                    showDefaultData()
                } else {
                    Log.e("HomeFragment", "No se encontraron datos para la clave: ${tankViewModel.selectedKey.value}")
                    showDefaultData()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Error al leer datos: ${error.message}")
            }
        })
    }

    private fun updateWaveView(snapshot: DataSnapshot) {
        val porcentaje = snapshot.child("porcentaje").value?.toString()?.toFloatOrNull()
        val fecha = snapshot.child("fecha").value?.toString()
        if (porcentaje != null && fecha != null) {
            ultimoPorcentaje = porcentaje
            _binding?.waveView?.setProgress(porcentaje)
            _binding?.tvPercentage?.text = "${porcentaje.toInt()}%"
            actualizarVolumen(porcentaje)
            Log.d("HomeFragment", "Porcentaje actualizado: $porcentaje% fecha: $fecha")
            sendPercentageToService(porcentaje, fecha)
        } else {
            Log.e("HomeFragment", "El porcentaje es nulo o no válido")
        }
    }

    private fun sendPercentageToService(porcentaje: Float, fecha: String) {
        val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        if (fecha == currentDate) {
            val serviceIntent = Intent(requireContext(), ForegroundService::class.java).apply {
                putExtra("porcentaje", porcentaje)
            }
            requireContext().startService(serviceIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        val key = tankViewModel.selectedKey.value ?: return
        FirebaseFirestore.getInstance().collection("modules").document(key)
            .get()
            .addOnSuccessListener { doc ->
                capacidadLitros = doc.getDouble("capacidadLitros") ?: 0.0
                ultimoPorcentaje?.let { actualizarVolumen(it) }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        consumoListener?.let { consumoRef?.removeEventListener(it) }
        consumoListener = null
        consumoRef = null
        _binding = null
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
