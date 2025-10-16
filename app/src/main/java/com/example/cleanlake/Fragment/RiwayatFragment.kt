package com.example.cleanlake.Fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cleanlake.Adapter.RiwayatAdapter
import com.example.cleanlake.Model.RiwayatModel
import com.example.cleanlake.R
import com.example.cleanlake.databinding.FragmentRiwayatBinding
import com.google.firebase.database.*

class RiwayatFragment : Fragment() {

    private var _binding: FragmentRiwayatBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbRef: DatabaseReference
    private lateinit var adapter: RiwayatAdapter
    private val listRiwayat = mutableListOf<RiwayatModel>()
    private var lokasiTerpilih = "Batas Kota"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatBinding.inflate(inflater, container, false)
        dbRef = FirebaseDatabase.getInstance().getReference("Riwayat")
        setupRecycler()
        setupLokasiButton()
        loadData(lokasiTerpilih)
        return binding.root
    }

    private fun setupRecycler() {
        adapter = RiwayatAdapter(requireContext(), listRiwayat)
        binding.rvRiwayat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRiwayat.adapter = adapter
    }

    private fun setupLokasiButton() {
        val scaleUp = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_up)
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)

        binding.cardViewBatasKota.setOnClickListener {
            lokasiTerpilih = "Batas Kota"
            binding.tvLokasi.text = lokasiTerpilih.uppercase()
            binding.tvLokasi.startAnimation(fadeIn)
            it.startAnimation(scaleUp)
            loadData(lokasiTerpilih)
        }

        binding.cardViewDanauSentani.setOnClickListener {
            lokasiTerpilih = "Danau Sentani"
            binding.tvLokasi.text = lokasiTerpilih.uppercase()
            binding.tvLokasi.startAnimation(fadeIn)
            it.startAnimation(scaleUp)
            loadData(lokasiTerpilih)
        }

        binding.cardViewKampungHarapan.setOnClickListener {
            lokasiTerpilih = "Kampung Harapan"
            binding.tvLokasi.text = lokasiTerpilih.uppercase()
            binding.tvLokasi.startAnimation(fadeIn)
            it.startAnimation(scaleUp)
            loadData(lokasiTerpilih)
        }
    }

    private fun loadData(lokasi: String) {
        dbRef.child(lokasi).addValueEventListener(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                listRiwayat.clear()
                for (dataSnap in snapshot.children) {
                    val data = dataSnap.getValue(RiwayatModel::class.java)
                    if (data != null) listRiwayat.add(data)
                }
                listRiwayat.reverse() // urutkan terbaru di atas
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
