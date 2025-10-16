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

    private var lokasiDipilih = "BatasKota"
    private var ambangListener: ValueEventListener? = null

    // untuk simpan lokasi yang aktif
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

        binding.btnSimpanAmbang.setOnClickListener {
            simpanDataAmbang(lokasiDipilih)
        }

        // Lokasi default
        selectLocation(binding.cardViewBatasKota, binding.tvBatasKota, "BatasKota", "BATAS KOTA")

        return binding.root
    }

    /** =========================
     * 🔹 Label di setiap parameter
     * ========================= */
    private fun setupParameterLabels() {
        binding.itemPh.tvParameter.text = "pH"
        binding.itemSuhu.tvParameter.text = "Suhu (°C)"
        binding.itemTds.tvParameter.text = "TDS (ppm)"
        binding.itemKekeruhan.tvParameter.text = "Kekeruhan (NTU)"
    }

    /** =========================
     * 🔹 Setup card lokasi + animasi klik
     * ========================= */
    private fun setupLokasiCards() {
        binding.apply {
            cardViewBatasKota.setOnClickListener {
                selectLocation(cardViewBatasKota, tvBatasKota, "Batas Kota", "BATAS KOTA")
            }
            cardViewDanauSentani.setOnClickListener {
                selectLocation(cardViewDanauSentani, tvDanauSentani, "Danau Sentani", "DANAU SENTANI")
            }
            cardViewKampungHarapan.setOnClickListener {
                selectLocation(cardViewKampungHarapan, tvKampungHarapan, "Kampung Harapan", "KAMPUNG HARAPAN")
            }
        }
    }

    /** =========================
     * 🔹 Fungsi pilih lokasi (ada animasi & highlight)
     * ========================= */
    private fun selectLocation(card: CardView, textView: TextView, lokasi: String, label: String) {
        val context = requireContext()

        // animasi
        val scaleUp = AnimationUtils.loadAnimation(context, R.anim.scale_up)
        val fadeIn = AnimationUtils.loadAnimation(context, R.anim.fade_in)

        // reset lokasi sebelumnya
        selectedCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_off))
        selectedText?.setTextColor(ContextCompat.getColor(context, R.color.black))

        // ubah warna card aktif
        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_on))
        textView.setTextColor(ContextCompat.getColor(context, R.color.white))
        card.startAnimation(scaleUp)

        // simpan card aktif
        selectedCard = card
        selectedText = textView

        // ubah label lokasi utama
        binding.tvLokasi.text = label
        binding.tvLokasi.startAnimation(fadeIn)

        lokasiDipilih = lokasi
        loadDataAmbang(lokasi)
    }

    /** =========================
     * 🔹 Load data dari Firebase (safe binding)
     * ========================= */
    private fun loadDataAmbang(lokasi: String) {
        ambangListener?.let { database.child(lokasi).removeEventListener(it) }

        val fadeOut = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_out)
        val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
        binding.dataContainer.startAnimation(fadeOut)

        ambangListener = database.child(lokasi).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val safeBinding = _binding ?: return

                fun setValue(item: ItemAmbangBatasBinding, key: String) {
                    item.etMin.setText(snapshot.child("$key/min").getValue(Double::class.java)?.toString() ?: "")
                    item.etMax.setText(snapshot.child("$key/max").getValue(Double::class.java)?.toString() ?: "")
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
                if (isAdded && _binding != null) {
                    Toast.makeText(requireContext(), "Gagal memuat data", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    /** =========================
     * 🔹 Simpan data ambang batas ke Firebase
     * ========================= */
    private fun simpanDataAmbang(lokasi: String) {
        val safeBinding = _binding ?: return

        fun getDouble(value: String) = value.toDoubleOrNull()

        safeBinding.apply {
            val phMin = getDouble(itemPh.etMin.text.toString())
            val phMax = getDouble(itemPh.etMax.text.toString())
            val suhuMin = getDouble(itemSuhu.etMin.text.toString())
            val suhuMax = getDouble(itemSuhu.etMax.text.toString())
            val tdsMin = getDouble(itemTds.etMin.text.toString())
            val tdsMax = getDouble(itemTds.etMax.text.toString())
            val kekeruhanMin = getDouble(itemKekeruhan.etMin.text.toString())
            val kekeruhanMax = getDouble(itemKekeruhan.etMax.text.toString())

            if (listOf(phMin, phMax, suhuMin, suhuMax, tdsMin, tdsMax, kekeruhanMin, kekeruhanMax).any { it == null }) {
                Toast.makeText(requireContext(), "Isi semua nilai dengan benar", Toast.LENGTH_SHORT).show()
                return
            }

            val ambangData = mapOf(
                "pH/min" to phMin!!,
                "pH/max" to phMax!!,
                "Suhu/min" to suhuMin!!,
                "Suhu/max" to suhuMax!!,
                "TDS/min" to tdsMin!!,
                "TDS/max" to tdsMax!!,
                "Kekeruhan/min" to kekeruhanMin!!,
                "Kekeruhan/max" to kekeruhanMax!!
            )

            database.child(lokasi).updateChildren(ambangData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Data ambang batas tersimpan", Toast.LENGTH_SHORT).show()
                    val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_up)
                    btnSimpanAmbang.startAnimation(anim)
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Gagal menyimpan data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        ambangListener?.let { database.child(lokasiDipilih).removeEventListener(it) }
        ambangListener = null
        _binding = null
    }
}
