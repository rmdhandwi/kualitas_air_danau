package com.example.cleanlake.Fragment

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

    private lateinit var database: DatabaseReference
    private var lokasiDipilih = "Yoka"
    private var ambangListener: ValueEventListener? = null

    private var selectedCard: CardView? = null
    private var selectedText: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAmbangBinding.inflate(inflater, container, false)
        database = FirebaseDatabase.getInstance().getReference("AmbangBatas")

        setupParameterLabels()
        setupLokasiCards()

        // Lokasi default
        selectLocation(binding.cardViewYoka, binding.tvYoka, "Yoka", "YOKA")

        binding.btnSimpanAmbang.setOnClickListener {
            simpanDataAmbang(lokasiDipilih)
        }

        return binding.root
    }

    // ==============================
    // 🔹 Label Parameter
    // ==============================
    private fun setupParameterLabels() = binding.apply {
        itemPh.tvParameter.text = "pH"
        itemSuhu.tvParameter.text = "Suhu (°C)"
        itemTds.tvParameter.text = "TDS (ppm)"
        itemKekeruhan.tvParameter.text = "Kekeruhan (NTU)"
    }

    // ==============================
    // 🔹 Setup Lokasi (Card)
    // ==============================
    private fun setupLokasiCards() = binding.apply {
        cardViewYoka.setOnClickListener {
            selectLocation(cardViewYoka, tvYoka, "Yoka", "YOKA")
        }
        cardViewBatasKota.setOnClickListener {
            selectLocation(cardViewBatasKota, tvBatasKota, "Batas_Kota", "BATAS KOTA")
        }
        cardViewYobeh.setOnClickListener {
            selectLocation(cardViewYobeh, tvYobeh, "Yobeh", "YOBEH")
        }
    }

    // ==============================
    // 🔹 Pilih Lokasi & Load Data
    // ==============================
    private fun selectLocation(card: CardView, textView: TextView, lokasi: String, label: String) {
        val context = requireContext()
        val scaleUp = AnimationUtils.loadAnimation(context, R.anim.scale_up)
        val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)

        // Reset tampilan card sebelumnya
        selectedCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_off))
        selectedText?.setTextColor(ContextCompat.getColor(context, R.color.black))

        // Aktifkan card baru
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_on))
        textView.setTextColor(ContextCompat.getColor(context, R.color.white))
        card.startAnimation(scaleUp)

        selectedCard = card
        selectedText = textView

        binding.tvLokasi.text = label
        binding.tvLokasi.startAnimation(fadeIn)

        lokasiDipilih = lokasi
        loadDataAmbang(lokasi)
    }

    // ==============================
    // 🔹 Load Data Ambang dari Firebase
    // ==============================
    private fun loadDataAmbang(lokasi: String) {
        // Hapus listener lama biar gak leak
        ambangListener?.let { database.child(lokasi).removeEventListener(it) }

        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)

        binding.dataContainer.startAnimation(fadeOut)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return

                val safeBinding = binding

                fun setValue(item: ItemAmbangBatasBinding, key: String) {
                    val min = snapshot.child("$key/min").getValue(Double::class.java)
                    val max = snapshot.child("$key/max").getValue(Double::class.java)
                    item.etMin.setText(min?.toString() ?: "")
                    item.etMax.setText(max?.toString() ?: "")
                }

                safeBinding.apply {
                    setValue(itemPh, "pH")
                    setValue(itemSuhu, "Suhu")
                    setValue(itemTds, "TDS")
                    setValue(itemKekeruhan, "Kekeruhan")
                    dataContainer.startAnimation(fadeIn)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                if (!isAdded || _binding == null) return
                Toast.makeText(requireContext(), "Gagal memuat data: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        ambangListener = listener
        database.child(lokasi).addValueEventListener(listener)
    }

    // ==============================
    // 🔹 Simpan Data ke Firebase
    // ==============================
    private fun simpanDataAmbang(lokasi: String) {
        val safeBinding = _binding ?: return

        fun parseDouble(value: String): Double? {
            val parsed = value.toDoubleOrNull()
            return if (parsed != null && parsed >= 0) parsed else null
        }

        val dataMap = mapOf(
            "pH/min" to parseDouble(safeBinding.itemPh.etMin.text.toString()),
            "pH/max" to parseDouble(safeBinding.itemPh.etMax.text.toString()),
            "Suhu/min" to parseDouble(safeBinding.itemSuhu.etMin.text.toString()),
            "Suhu/max" to parseDouble(safeBinding.itemSuhu.etMax.text.toString()),
            "TDS/min" to parseDouble(safeBinding.itemTds.etMin.text.toString()),
            "TDS/max" to parseDouble(safeBinding.itemTds.etMax.text.toString()),
            "Kekeruhan/min" to parseDouble(safeBinding.itemKekeruhan.etMin.text.toString()),
            "Kekeruhan/max" to parseDouble(safeBinding.itemKekeruhan.etMax.text.toString())
        )

        if (dataMap.values.any { it == null }) {
            Toast.makeText(requireContext(), "Isi semua nilai dengan angka valid!", Toast.LENGTH_SHORT).show()
            return
        }

        database.child(lokasi).updateChildren(dataMap.mapValues { it.value as Any })
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_up)
                binding.btnSimpanAmbang.startAnimation(anim)
                Toast.makeText(requireContext(), "✅ Data ambang batas tersimpan", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "❌ Gagal menyimpan data", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Pastikan listener dilepas agar tidak leak
        ambangListener?.let { database.child(lokasiDipilih).removeEventListener(it) }
        ambangListener = null
        _binding = null
    }
}
