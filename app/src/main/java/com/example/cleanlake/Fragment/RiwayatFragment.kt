package com.example.cleanlake.Fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
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

    private var lokasiTerpilih = "Yoka"
    private var dataListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatBinding.inflate(inflater, container, false)
        dbRef = FirebaseDatabase.getInstance().getReference("Riwayat")

        setupRecycler()
        setupLokasiButtons()
        setActiveCard(binding.cardViewYoka) // default aktif
        loadData(lokasiTerpilih)

        return binding.root
    }

    /** 🔹 Setup RecyclerView */
    private fun setupRecycler() {
        adapter = RiwayatAdapter(requireContext(), listRiwayat)
        binding.rvRiwayat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRiwayat.adapter = adapter
    }

    /** 🔹 Setup tombol lokasi */
    private fun setupLokasiButtons() = binding.apply {
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        val scaleUp = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_up)

        fun handleSelection(card: CardView, lokasi: String, label: String) {
            lokasiTerpilih = lokasi
            tvLokasi.text = label
            tvLokasi.startAnimation(fadeIn)
            card.startAnimation(scaleUp)
            setActiveCard(card)
            loadData(lokasi)
        }

        cardViewYoka.setOnClickListener { handleSelection(cardViewYoka, "Yoka", "YOKA") }
        cardViewBatasKota.setOnClickListener { handleSelection(cardViewBatasKota, "Batas_Kota", "BATAS KOTA") }
        cardViewYobeh.setOnClickListener { handleSelection(cardViewYobeh, "Yobeh", "YOBEH") }
    }

    /** 🔹 Ubah warna card aktif */
    private fun setActiveCard(activeCard: CardView) {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.menu_on)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.menu_off)

        val allCards = listOf(
            binding.cardViewBatasKota,
            binding.cardViewYoka,
            binding.cardViewYobeh
        )

        for (card in allCards) {
            card.setCardBackgroundColor(inactiveColor)
        }
        activeCard.setCardBackgroundColor(activeColor)
    }

    /** 🔹 Ambil data Firebase sesuai lokasi */
    private fun loadData(lokasi: String) {
        // Bersihkan listener lama agar tidak leak
        dataListener?.let { dbRef.child(lokasi).removeEventListener(it) }

        val listener = object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return  // hindari crash kalau fragment sudah hilang

                listRiwayat.clear()
                for (dataSnap in snapshot.children) {
                    val data = dataSnap.getValue(RiwayatModel::class.java)
                    if (data != null) listRiwayat.add(data)
                }

                listRiwayat.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()

                binding.tvKosong.visibility =
                    if (listRiwayat.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || _binding == null) return
                binding.tvKosong.visibility = View.VISIBLE
                binding.tvKosong.text = "Gagal memuat data: ${error.message}"
            }
        }

        dataListener = listener
        dbRef.child(lokasi).addValueEventListener(listener)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Pastikan listener dilepas
        dataListener?.let { dbRef.child(lokasiTerpilih).removeEventListener(it) }
        dataListener = null
        _binding = null
    }
}
