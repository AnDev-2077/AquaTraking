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
import com.devapps.aquatraking.views.WaveLoadView
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

    private val tankViewModel: ViewModel by activityViewModels() // ViewModel compartido

    private lateinit var binding: FragmentHomeBinding
    private var waveView: WaveLoadView? = null
    private lateinit var database: DatabaseReference
    private var currentDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var consumoListener: ChildEventListener? = null

    private val maxDaysBack = 5
    private var currentOffset = 0

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

        // Observar cambios en la key seleccionada
        tankViewModel.selectedKey.observe(viewLifecycleOwner) { key ->
            key?.let { loadTankData(it) } ?: showDefaultData()
        }

        // Iniciar el servicio en primer plano
        val serviceIntent = Intent(requireContext(), ForegroundService::class.java)
        requireContext().startService(serviceIntent)

        // Inicializar vistas y listeners
        waveView = binding.waveView
        database = FirebaseDatabase.getInstance().reference.child("ModulesWifi")

        // Mostrar fecha actual
        binding.tvDate.text = dateFormat.format(currentDate.time)
        actualizarFechaDisplay()

        // Configurar listeners de los botones
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

        // Cargar datos iniciales
        actualizarConsumoPorDia()
    }

    private fun rebootListener() {
        consumoListener?.let { database.removeEventListener(it) }
    }

    private fun loadTankData(key: String) {
        val ref = FirebaseDatabase.getInstance().getReference("ModulesWifi/$key")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (childSnapshot in snapshot.children) {
                        val fecha = childSnapshot.child("fecha").getValue(String::class.java)
                        val porcentaje = childSnapshot.child("porcentaje").getValue(String::class.java)
                        Log.d("HomeFragment", "Fecha: $fecha, Porcentaje: $porcentaje")
                        updateWaveView(childSnapshot)
                    }
                } else {
                    Log.e("HomeFragment", "No se encontraron datos para la clave: $key")
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Error al leer datos: ${error.message}")
            }
        })
    }

    private fun showDefaultData() {
        waveView?.setProgress(0f)
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
        rebootListener()

        val targetDate = Calendar.getInstance().apply {
            add(Calendar.DATE, -currentOffset)
        }
        val fechaFormateada = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(targetDate.time)

        // Se crea una query para filtrar por el campo "fecha"
        val query = database.orderByChild("fecha").equalTo(fechaFormateada)
        consumoListener = query.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                updateWaveView(snapshot)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                updateWaveView(snapshot)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                // Puedes implementar lógica si se elimina un registro
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                // No es necesario para este caso
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HomeFragment", "Error al leer datos: ${error.message}")
            }
        })
    }

    private fun updateWaveView(snapshot: DataSnapshot) {
        val porcentaje = snapshot.child("porcentaje").getValue(String::class.java)?.toFloatOrNull()
        if (porcentaje != null) {
            waveView?.setProgress(porcentaje)
            Log.d("HomeFragment", "Porcentaje actualizado: $porcentaje% para la fecha: ${snapshot.child("fecha").value}")
        } else {
            Log.e("HomeFragment", "El porcentaje es nulo o no válido")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        rebootListener()
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