package com.example.cleanlake.Fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.cleanlake.R
import com.example.cleanlake.Service.AlertService
import com.example.cleanlake.databinding.FragmentBerandaBinding
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class BerandaFragment : Fragment() {

    private var _binding: FragmentBerandaBinding? = null
    private val binding get() = _binding!!

    private lateinit var lokasiRef: DatabaseReference
    private lateinit var ambangRef: DatabaseReference
    private lateinit var riwayatRef: DatabaseReference // 🔹 Tambahan

    private var selectedCard: CardView? = null
    private var selectedTextView: TextView? = null
    private var dataListener: ValueEventListener? = null
    private var ambangListener: ValueEventListener? = null

    private var ambangData: MutableMap<String, Pair<Double?, Double?>> = mutableMapOf()
    private var lastSaveTime = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBerandaBinding.inflate(inflater, container, false)
        lokasiRef = FirebaseDatabase.getInstance().getReference("Lokasi")
        ambangRef = FirebaseDatabase.getInstance().getReference("AmbangBatas")
        riwayatRef = FirebaseDatabase.getInstance().getReference("Riwayat") // 🔹 Tambahan
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardViewBatasKota.setOnClickListener {
            selectLocation(binding.cardViewBatasKota, binding.tvBatasKota, "Batas Kota")
        }
        binding.cardViewDanauSentani.setOnClickListener {
            selectLocation(binding.cardViewDanauSentani, binding.tvDanauSentani, "Danau Sentani")
        }
        binding.cardViewKampungHarapan.setOnClickListener {
            selectLocation(
                binding.cardViewKampungHarapan,
                binding.tvKampungHarapan,
                "Kampung Harapan"
            )
        }

        binding.itemPh.root.findViewById<TextView>(R.id.title).text = "pH"
        binding.itemTds.root.findViewById<TextView>(R.id.title).text = "TDS"
        binding.itemSuhu.root.findViewById<TextView>(R.id.title).text = "Suhu"
        binding.itemKekeruhan.root.findViewById<TextView>(R.id.title).text = "Kekeruhan"

        binding.itemPh.root.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_ph)
        binding.itemTds.root.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_tds)
        binding.itemSuhu.root.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_suhu)
        binding.itemKekeruhan.root.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_kekeruhan)

        // Default tampilkan lokasi pertama
        selectLocation(binding.cardViewBatasKota, binding.tvBatasKota, "Batas Kota")
    }

    private fun selectLocation(card: CardView, textView: TextView, lokasi: String) {
        val context = requireContext()
        val scaleUp = AnimationUtils.loadAnimation(context, R.anim.scale_up)
        card.startAnimation(scaleUp)

        selectedCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_off))
        selectedTextView?.setTextColor(ContextCompat.getColor(context, R.color.black))

        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_on))
        textView.setTextColor(ContextCompat.getColor(context, R.color.white))


        selectedCard = card
        selectedTextView = textView

        binding.tvLokasi.text = lokasi.uppercase()
        binding.tvLokasi.startAnimation(scaleUp)

        loadAmbangBatas(lokasi)
    }

    private fun loadAmbangBatas(lokasi: String) {
        ambangListener?.let { ambangRef.child(lokasi).removeEventListener(it) }

        ambangListener = ambangRef.child(lokasi).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ambangData.clear()
                val safeBinding = _binding ?: return

                listOf("pH", "Suhu", "TDS", "Kekeruhan").forEach { param ->
                    val min = snapshot.child("$param/min").getValue(Double::class.java)
                    val max = snapshot.child("$param/max").getValue(Double::class.java)
                    ambangData[param] = Pair(min, max)
                }

                loadSensorData(lokasi)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadSensorData(lokasi: String) {
        dataListener?.let { lokasiRef.child(lokasi).removeEventListener(it) }

        dataListener = lokasiRef.child(lokasi).addValueEventListener(object : ValueEventListener {
            @SuppressLint("SetTextI18n")
            override fun onDataChange(snapshot: DataSnapshot) {
                val safeBinding = _binding ?: return
                if (!snapshot.exists()) return

                val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
                safeBinding.dataContainer.startAnimation(fadeIn)

                val params = listOf("pH", "Suhu", "TDS", "Kekeruhan")

                var adaBahaya = false
                val nilaiData = mutableMapOf<String, Double?>()

                for (param in params) {
                    val valueStr = snapshot.child(param).value?.toString() ?: "-"
                    val value = valueStr.toDoubleOrNull()
                    nilaiData[param] = value

                    val (min, max) = ambangData[param] ?: (null to null)
                    val statusView = getStatusView(param)
                    val minmaxView = getMinMaxView(param)
                    val valueView = getValueView(param)

                    // Update tampilan
                    valueView.text = when (param) {
                        "Suhu" -> "$valueStr°C"
                        "TDS" -> "$valueStr ppm"
                        "Kekeruhan" -> "$valueStr NTU"
                        else -> valueStr
                    }
                    minmaxView.text = "Min: ${min ?: "-"} | Max: ${max ?: "-"}"

                    // Tentukan status
                    if (value == null || min == null || max == null) {
                        statusView.text = "DATA KOSONG"
                        statusView.setBackgroundResource(R.drawable.bg_status_warning)
                    } else if (value in min..max) {
                        statusView.text = "AMAN"
                        statusView.setBackgroundResource(R.drawable.bg_status_safe)
                    } else {
                        statusView.text = "BAHAYA"
                        statusView.setBackgroundResource(R.drawable.bg_status_danger)
                        adaBahaya = true

                        // 🔹 Kirim ke service untuk notifikasi & alarm
                        val intent = Intent(requireContext(), AlertService::class.java)
                        intent.putExtra("lokasi", lokasi)
                        intent.putExtra("parameter", param)
                        intent.putExtra("value", valueStr)
                        requireContext().startService(intent)
                    }

                }

                // 🔹 Simpan ke Riwayat setiap kali data berubah
                simpanKeRiwayat(lokasi, nilaiData, adaBahaya)
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // 🔹 Tambahan: Fungsi untuk simpan ke Firebase Riwayat
    // 🔹 Tambahan: Fungsi untuk simpan ke Firebase Riwayat lengkap dengan min-max
    private fun simpanKeRiwayat(
        lokasi: String,
        nilaiData: Map<String, Double?>,
        adaBahaya: Boolean
    ) {
        // Hanya simpan kalau ada bahaya
        if (!adaBahaya) return

        // Batasi penyimpanan minimal setiap 5 detik
        val now = System.currentTimeMillis()
        if (now - lastSaveTime < 5_000) return
        lastSaveTime = now

        val status = "BAHAYA"
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            .format(Date())

        val dataRiwayat = mutableMapOf<String, Any?>(
            "timestamp" to timestamp,
            "status" to status
        )

        // Simpan hanya parameter yang bahaya + deskripsi
        val deskripsiList = mutableListOf<String>()

        nilaiData.forEach { (param, value) ->
            val (min, max) = ambangData[param] ?: (null to null)
            if (value != null && min != null && max != null) {
                when {
                    value < min -> {
                        dataRiwayat[param] = value
                        dataRiwayat["${param}_min"] = min
                        dataRiwayat["${param}_max"] = max
                        deskripsiList.add("$param di bawah batas minimum ($value < $min)")
                    }
                    value > max -> {
                        dataRiwayat[param] = value
                        dataRiwayat["${param}_min"] = min
                        dataRiwayat["${param}_max"] = max
                        deskripsiList.add("$param melebihi batas maksimum ($value > $max)")
                    }
                }
            }
        }

        // Jika tidak ada yang bahaya, keluar
        if (deskripsiList.isEmpty()) return

        // Tambahkan deskripsi gabungan
        dataRiwayat["deskripsi"] = deskripsiList.joinToString(separator = "; ")

        val newKey = riwayatRef.child(lokasi).push().key ?: return
        riwayatRef.child(lokasi).child(newKey).setValue(dataRiwayat)
    }


    private fun getValueView(param: String): TextView = when (param) {
        "pH" -> binding.itemPh.root.findViewById(R.id.value)
        "Suhu" -> binding.itemSuhu.root.findViewById(R.id.value)
        "TDS" -> binding.itemTds.root.findViewById(R.id.value)
        "Kekeruhan" -> binding.itemKekeruhan.root.findViewById(R.id.value)
        else -> binding.itemPh.root.findViewById(R.id.value)
    }

    private fun getMinMaxView(param: String): TextView = when (param) {
        "pH" -> binding.itemPh.root.findViewById(R.id.minmax)
        "Suhu" -> binding.itemSuhu.root.findViewById(R.id.minmax)
        "TDS" -> binding.itemTds.root.findViewById(R.id.minmax)
        "Kekeruhan" -> binding.itemKekeruhan.root.findViewById(R.id.minmax)
        else -> binding.itemPh.root.findViewById(R.id.minmax)
    }

    private fun getStatusView(param: String): TextView = when (param) {
        "pH" -> binding.itemPh.root.findViewById(R.id.status)
        "Suhu" -> binding.itemSuhu.root.findViewById(R.id.status)
        "TDS" -> binding.itemTds.root.findViewById(R.id.status)
        "Kekeruhan" -> binding.itemKekeruhan.root.findViewById(R.id.status)
        else -> binding.itemPh.root.findViewById(R.id.status)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dataListener?.let { lokasiRef.removeEventListener(it) }
        ambangListener?.let { ambangRef.removeEventListener(it) }
        _binding = null
    }
}
