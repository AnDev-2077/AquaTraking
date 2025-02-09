package com.devapps.aquatraking.fragments

import android.R
import android.content.Intent
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.fragment.app.activityViewModels
import com.devapps.aquatraking.databinding.FragmentHomeBinding
import com.devapps.aquatraking.services.ForegroundService
import com.devapps.aquatraking.services.ViewModel
import com.devapps.aquatraking.views.WaveLoadView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class HomeFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private val tankViewModel: ViewModel by activityViewModels()

    private lateinit var binding: FragmentHomeBinding
    private var waveView: WaveLoadView? = null
    private lateinit var database: DatabaseReference
    private var currentDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var consumoListener: ChildEventListener? = null
    private val userId = FirebaseAuth.getInstance().currentUser?.uid

    private lateinit var spinnerModules: Spinner
    private var modulesList: MutableList<String> = mutableListOf()



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

        spinnerModules = binding.spinnerModules

        tankViewModel.selectedKey.observe(viewLifecycleOwner) { key ->
            key?.let { loadTankData(it) } ?: showDefaultData()
        }

        val serviceIntent = Intent(requireContext(), ForegroundService::class.java)
        requireContext().startService(serviceIntent)

        if (userId != null) {
            Log.d("HomeFragment", "El usuario actual es: $userId")
            getModulesForUser(userId)
        } else {
            Log.e("HomeFragment", "No se ha encontrado un usuario autenticado")
            return
        }

        waveView = binding.waveView

        database = FirebaseDatabase.getInstance().reference.child("ModulesWifi")

        binding.tvDate.text = dateFormat.format(currentDate.time)

        actualizarFechaDisplay()

        binding.btnPrevious.setOnClickListener{
            if (currentOffset < maxDaysBack) {
                currentOffset++
                actualizarFechaDisplay()
                actualizarConsumoPorDia()
            }
        }

        binding.btnNext.setOnClickListener{
            if (currentOffset > 0) {
                currentOffset--
                actualizarFechaDisplay()
                actualizarConsumoPorDia()
            }
        }
        actualizarConsumoPorDia()
    }

    private fun rebootListener() {
        consumoListener?.let { database.removeEventListener(it) }
    }

    private fun loadTankData(key: String) {
        val ref = FirebaseDatabase.getInstance().getReference("ModulesWifi/$key")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Actualizar UI con datos reales
            }
            override fun onCancelled(error: DatabaseError) {}
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

    private fun getModulesForUser(userId: String) {
        val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)
        userRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Asumiendo que el campo "modules" es un array de strings.
                    val modules = documentSnapshot.get("modules") as? List<String>
                    if (modules != null && modules.isNotEmpty()) {
                        for (module in modules) {
                            Log.d("HomeFragment", "Módulo encontrado: $module")
                            // Aquí puedes procesar el módulo, por ejemplo, agregarlo a una lista para el spinner:
                            modulesList.add(module)
                        }
                        // Si necesitas mostrar el spinner con los módulos:
                        setupSpinner()
                    } else {
                        Log.d("HomeFragment", "No se encontraron módulos para el usuario")
                    }
                } else {
                    Log.d("HomeFragment", "El documento del usuario no existe")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("HomeFragment", "Error al obtener el documento del usuario: ${exception.message}")
            }
    }

    private fun setupSpinner() {
        if (modulesList.size > 1) {
            // Si hay más de un módulo, mostrar el Spinner
            spinnerModules.visibility = View.VISIBLE
            val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_item, modulesList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerModules.adapter = adapter

            spinnerModules.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    val selectedModule = modulesList[position]
                    updateDatabaseReference(selectedModule)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // No hacer nada
                }
            }
        } else if (modulesList.size == 1) {
            // Si solo hay un módulo, seleccionarlo automáticamente
            spinnerModules.visibility = View.GONE
            updateDatabaseReference(modulesList[0])
        }
    }



    private fun updateDatabaseReference(moduleId: String) {
        database = FirebaseDatabase.getInstance().getReference("/ModulesWifi/$moduleId")
        actualizarConsumoPorDia()
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