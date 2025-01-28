package com.devapps.aquatraking.fragments

import android.content.Intent
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

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class HomeFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null

    private var binding: FragmentHomeBinding? = null
    private var waveView: WaveLoadView? = null
    private lateinit var database: DatabaseReference

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
        binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("HomeFragment", "onViewCreated called")

        val serviceIntent = Intent(requireContext(), ForegroundService::class.java)
        requireContext().startService(serviceIntent)

        initFirebaseListener()
    }

    private fun initViews() {
        waveView = binding?.waveView
    }

    private fun initFirebaseListener() {

        val sharedPreferences = requireContext().getSharedPreferences("UserPreferences", AppCompatActivity.MODE_PRIVATE)
        val moduleKey = sharedPreferences.getString("moduleKey", null)

        Log.d("HomeFragment", "initFirebaseListener called")
        database = FirebaseDatabase.getInstance().getReference("/ModulesWifi/$moduleKey")

        if (moduleKey == null) {
            Log.e("HomeFragment", "No se encontró ninguna clave de módulo en SharedPreferences")
            return
        }

        database.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val porcentaje = snapshot.child("porcentaje").getValue(String::class.java)?.toFloatOrNull()
                if (porcentaje != null) {
                    initViews()
                    waveView?.setProgress(porcentaje)
                    Log.d("HomeFragment", "Porcentaje actualizado: $porcentaje%")
                } else {
                    Log.e("HomeFragment", "El porcentaje es nulo o no válido")
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val porcentaje = snapshot.child("porcentaje").getValue(String::class.java)?.toFloatOrNull()
                if (porcentaje != null) {
                    initViews()
                    waveView?.setProgress(porcentaje)
                    Log.d("HomeFragment", "Porcentaje actualizado: $porcentaje%")
                    sendForegroundService(porcentaje)
                } else {
                    Log.e("HomeFragment", "El porcentaje es nulo o no válido")
                }
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


    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
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