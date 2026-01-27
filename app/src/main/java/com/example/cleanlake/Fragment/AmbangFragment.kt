package com.example.cleanlake.Fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.cleanlake.R
import com.example.cleanlake.databinding.FragmentAmbangBinding
import com.example.cleanlake.databinding.ItemAmbangBatasBinding
import com.google.firebase.database.*

class AmbangFragment : Fragment() {

    private var _binding: FragmentAmbangBinding? = null
    private val binding get() = _binding!!

    private lateinit var ambangRef: DatabaseReference
    private lateinit var rootRef: DatabaseReference

    private var lokasiDipilih = "L001"
    private var ambangListener: ValueEventListener? = null
    private var lokasiListener: ValueEventListener? = null

    private var selectedCard: CardView? = null
    private var selectedText: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAmbangBinding.inflate(inflater, container, false)
        rootRef = FirebaseDatabase.getInstance().reference
        ambangRef = rootRef.child("AmbangBatas")

        setupParameterLabels()
        setupLokasiCards()
        loadNamaLokasi()

        selectLocation(binding.cardViewLokasi1, binding.tvLokasi1, "L001")

        binding.btnSimpanAmbang.setOnClickListener { simpanDataAmbang(lokasiDipilih) }
        binding.btnSimpanNamaLokasi.setOnClickListener { simpanNamaLokasi() }

        return binding.root
    }

    private fun simpanNamaLokasi() {
        val namaBaru = binding.etNamaLokasi.text.toString().trim()
        if (namaBaru.isEmpty()) {
            Toast.makeText(requireContext(), "Nama tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf(
            "AmbangBatas/$lokasiDipilih/lokasi" to namaBaru,
            "Lokasi/$lokasiDipilih/nama" to namaBaru,
            "Riwayat/$lokasiDipilih/lokasi" to namaBaru
        )

        rootRef.updateChildren(updates)
            .addOnSuccessListener {
                binding.etNamaLokasi.text.clear()
                Toast.makeText(requireContext(), "Nama lokasi diperbarui", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Gagal memperbarui nama lokasi", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadNamaLokasi() {
        lokasiListener?.let { ambangRef.removeEventListener(it) }

        lokasiListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return

                binding.tvLokasi1.text = snapshot.child("L001/lokasi").getValue(String::class.java) ?: "Lokasi 1"
                binding.tvLokasi2.text = snapshot.child("L002/lokasi").getValue(String::class.java) ?: "Lokasi 2"
                binding.tvLokasi3.text = snapshot.child("L003/lokasi").getValue(String::class.java) ?: "Lokasi 3"
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        ambangRef.addValueEventListener(lokasiListener!!)
    }

    @SuppressLint("SetTextI18n")
    private fun setupParameterLabels() = binding.apply {
        itemPh.tvParameter.text = "pH"
        itemSuhu.tvParameter.text = "Suhu (°C)"
        itemTds.tvParameter.text = "TDS (ppm)"
        itemKekeruhan.tvParameter.text = "Kekeruhan (NTU)"
    }

    private fun setupLokasiCards() = binding.apply {
        cardViewLokasi1.setOnClickListener { selectLocation(it as CardView, tvLokasi1, "L001") }
        cardViewLokasi2.setOnClickListener { selectLocation(it as CardView, tvLokasi2, "L002") }
        cardViewLokasi3.setOnClickListener { selectLocation(it as CardView, tvLokasi3, "L003") }
    }

    private fun selectLocation(card: CardView, textView: TextView, lokasiId: String) {
        val context = requireContext()

        val scaleUp = AnimationUtils.loadAnimation(context, R.anim.scale_up)
        val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)
        val slideFadeIn = AnimationUtils.loadAnimation(context, R.anim.slide_fase_in)

        // Reset sebelumnya
        selectedCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_off))
        selectedText?.setTextColor(ContextCompat.getColor(context, R.color.black))

        // Aktifkan yang baru
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_on))
        textView.setTextColor(ContextCompat.getColor(context, R.color.white))
        card.startAnimation(scaleUp)

        selectedCard = card
        selectedText = textView
        lokasiDipilih = lokasiId

        // Animasi container ambang
        binding.dataContainer.startAnimation(fadeOut)
        binding.dataContainer.postDelayed({
            loadDataAmbang(lokasiId)
            binding.dataContainer.startAnimation(slideFadeIn)
        }, 150)
    }


    private fun loadDataAmbang(lokasi: String) {
        ambangListener?.let { ambangRef.child(lokasi).removeEventListener(it) }

        ambangListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return

                fun setValue(item: ItemAmbangBatasBinding, key: String) {
                    item.etMin.setText(snapshot.child("$key/min").getValue(Double::class.java)?.toString() ?: "")
                    item.etMax.setText(snapshot.child("$key/max").getValue(Double::class.java)?.toString() ?: "")
                }

                binding.apply {
                    setValue(itemPh, "pH")
                    setValue(itemSuhu, "Suhu")
                    setValue(itemTds, "TDS")
                    setValue(itemKekeruhan, "Kekeruhan")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || _binding == null) return
                Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
            }
        }

        ambangRef.child(lokasi).addValueEventListener(ambangListener!!)
    }

    private fun simpanDataAmbang(lokasi: String) {
        if (!isAdded || _binding == null) return

        val data = mapOf(
            "pH/min" to binding.itemPh.etMin.text.toString().toDoubleOrNull(),
            "pH/max" to binding.itemPh.etMax.text.toString().toDoubleOrNull(),
            "Suhu/min" to binding.itemSuhu.etMin.text.toString().toDoubleOrNull(),
            "Suhu/max" to binding.itemSuhu.etMax.text.toString().toDoubleOrNull(),
            "TDS/min" to binding.itemTds.etMin.text.toString().toDoubleOrNull(),
            "TDS/max" to binding.itemTds.etMax.text.toString().toDoubleOrNull(),
            "Kekeruhan/min" to binding.itemKekeruhan.etMin.text.toString().toDoubleOrNull(),
            "Kekeruhan/max" to binding.itemKekeruhan.etMax.text.toString().toDoubleOrNull()
        )

        if (data.values.any { it == null }) {
            Toast.makeText(requireContext(), "Semua nilai harus diisi angka valid", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnSimpanAmbang.isEnabled = false

        ambangRef.child(lokasi).updateChildren(data as Map<String, Any>)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                binding.btnSimpanAmbang.isEnabled = true
                Toast.makeText(requireContext(), "Ambang tersimpan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                binding.btnSimpanAmbang.isEnabled = true
                Toast.makeText(requireContext(), "Gagal menyimpan", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        ambangListener?.let { ambangRef.child(lokasiDipilih).removeEventListener(it) }
        lokasiListener?.let { ambangRef.removeEventListener(it) }
        _binding = null
        super.onDestroyView()
    }
}

