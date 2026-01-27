package com.example.cleanlake.Fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
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
    private lateinit var lokasiRef: DatabaseReference
    private lateinit var adapter: RiwayatAdapter
    private val listRiwayat = mutableListOf<RiwayatModel>()

    private var selectedCard: CardView? = null
    private var selectedText: TextView? = null

    private var lokasiTerpilih = "L001"
    private var dataListener: ValueEventListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRiwayatBinding.inflate(inflater, container, false)

        dbRef = FirebaseDatabase.getInstance().getReference("Riwayat")
        lokasiRef = FirebaseDatabase.getInstance().getReference("Lokasi")

        setupRecycler()
        setupLokasiButtons()
        loadNamaLokasi()

        // Default lokasi
        selectLocation(binding.cardViewLokasi1, binding.tvLokasi1, "L001")

        return binding.root
    }

    private fun setupRecycler() {
        adapter = RiwayatAdapter(requireContext(), listRiwayat)
        binding.rvRiwayat.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRiwayat.adapter = adapter
    }

    private fun setupLokasiButtons() = binding.apply {
        cardViewLokasi1.setOnClickListener {
            selectLocation(cardViewLokasi1, tvLokasi1, "L001")
        }
        cardViewLokasi2.setOnClickListener {
            selectLocation(cardViewLokasi2, tvLokasi2, "L002")
        }
        cardViewLokasi3.setOnClickListener {
            selectLocation(cardViewLokasi3, tvLokasi3, "L003")
        }
    }

    private fun loadNamaLokasi() {
        lokasiRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return

                binding.tvLokasi1.text =
                    snapshot.child("L001/nama").getValue(String::class.java) ?: "Lokasi 1"
                binding.tvLokasi2.text =
                    snapshot.child("L002/nama").getValue(String::class.java) ?: "Lokasi 2"
                binding.tvLokasi3.text =
                    snapshot.child("L003/nama").getValue(String::class.java) ?: "Lokasi 3"
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun selectLocation(card: CardView, textView: TextView, lokasiId: String) {
        val context = requireContext()

        val scaleUp = AnimationUtils.loadAnimation(context, R.anim.scale_up)
        val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)
        val slideFadeIn = AnimationUtils.loadAnimation(context, R.anim.slide_fase_in)

        selectedCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_off))
        selectedText?.setTextColor(ContextCompat.getColor(context, R.color.black))

        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_on))
        textView.setTextColor(ContextCompat.getColor(context, R.color.white))
        card.startAnimation(scaleUp)

        selectedCard = card
        selectedText = textView
        lokasiTerpilih = lokasiId

        // Animasi container ambang
        binding.rvRiwayat.startAnimation(fadeOut)
        binding.rvRiwayat.postDelayed({
            loadData(lokasiId)
            binding.rvRiwayat.startAnimation(slideFadeIn)
        }, 150)
    }

    private fun loadData(lokasiId: String) {
        dataListener?.let { dbRef.child(lokasiId).removeEventListener(it) }

        val listener = object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return

                listRiwayat.clear()

                for (dataSnap in snapshot.children) {

                    // 🔒 Skip field string seperti "lokasi"
                    if (!dataSnap.hasChild("timestamp")) continue

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
        dbRef.child(lokasiId).addValueEventListener(listener)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        dataListener?.let { dbRef.child(lokasiTerpilih).removeEventListener(it) }
        dataListener = null
        _binding = null
    }
}

