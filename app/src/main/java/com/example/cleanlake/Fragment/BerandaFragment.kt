package com.example.cleanlake.Fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
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
    private lateinit var riwayatRef: DatabaseReference

    private var selectedCard: CardView? = null
    private var selectedTextView: TextView? = null
    private var dataListener: ValueEventListener? = null
    private var ambangListener: ValueEventListener? = null

    private var isMonitoringActive = false
    private var lokasiAktifId = "L001"

    private var alarmSedangAktif = false
    private var lokasiAlarmAktif: String? = null


    private var ambangData: MutableMap<String, Pair<Double?, Double?>> = mutableMapOf()
    private val lastSaveTimes = mutableMapOf<String, Long>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBerandaBinding.inflate(inflater, container, false)
        lokasiRef = FirebaseDatabase.getInstance().getReference("Lokasi")
        ambangRef = FirebaseDatabase.getInstance().getReference("AmbangBatas")
        riwayatRef = FirebaseDatabase.getInstance().getReference("Riwayat")
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup lokasi
        binding.cardViewLokasi1.setOnClickListener {
            selectLocation(binding.cardViewLokasi1, binding.tvLokasi1, "L001")
        }
        binding.cardViewLokasi2.setOnClickListener {
            selectLocation(binding.cardViewLokasi2, binding.tvLokasi2, "L002")
        }
        binding.cardViewLokasi3.setOnClickListener {
            selectLocation(binding.cardViewLokasi3, binding.tvLokasi3, "L003")
        }

        // Setup judul & ikon sensor
        binding.itemPh.root.findViewById<TextView>(R.id.title).text = "pH"
        binding.itemTds.root.findViewById<TextView>(R.id.title).text = "TDS"
        binding.itemSuhu.root.findViewById<TextView>(R.id.title).text = "Suhu"
        binding.itemKekeruhan.root.findViewById<TextView>(R.id.title).text = "Kekeruhan"

        binding.itemPh.root.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_ph)
        binding.itemTds.root.findViewById<ImageView>(R.id.icon).setImageResource(R.drawable.ic_tds)
        binding.itemSuhu.root.findViewById<ImageView>(R.id.icon)
            .setImageResource(R.drawable.ic_suhu)
        binding.itemKekeruhan.root.findViewById<ImageView>(R.id.icon)
            .setImageResource(R.drawable.ic_kekeruhan)

        // Pilihan lokasi default
        selectLocation(binding.cardViewLokasi1, binding.tvLokasi1, "L001")


        // Jalankan langsung pemuatan data lokasi default
        loadAmbangBatas("L001")
        loadNamaLokasi()
    }


    private fun selectLocation(card: CardView, textView: TextView, lokasiId: String) {
        // Matikan alarm saat ganti lokasi
        val stopIntent = Intent(requireContext(), AlertService::class.java).apply {
            action = AlertService.ACTION_STOP_ALARM
        }
        requireContext().startService(stopIntent)
        alarmSedangAktif = false
        lokasiAlarmAktif = null
        lokasiAktifId = lokasiId
        val context = requireContext()
        val scaleUp = AnimationUtils.loadAnimation(context, R.anim.scale_up)
        val fadeOut = AnimationUtils.loadAnimation(context, R.anim.fade_out)
        val slideFadeIn = AnimationUtils.loadAnimation(context, R.anim.slide_fase_in)

        selectedCard?.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_off))
        selectedTextView?.setTextColor(ContextCompat.getColor(context, R.color.black))

        card.setCardBackgroundColor(ContextCompat.getColor(context, R.color.menu_on))
        textView.setTextColor(ContextCompat.getColor(context, R.color.white))
        card.startAnimation(scaleUp)


        selectedCard = card
        selectedTextView = textView

        // Animasi container ambang
        binding.dataContainer.startAnimation(fadeOut)
        binding.dataContainer.postDelayed({
            loadSensorData(lokasiId)
            binding.dataContainer.startAnimation(slideFadeIn)
        }, 150)
    }


    private fun loadNamaLokasi() {
        val ref = FirebaseDatabase.getInstance().getReference("Lokasi")

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!isAdded || _binding == null) return  // 🔒 anti crash
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


    private fun loadAmbangBatas(lokasi: String) {
        ambangListener?.let { ambangRef.child(lokasi).removeEventListener(it) }

        ambangListener = ambangRef.child(lokasi).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ambangData.clear()

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

        val b = _binding ?: return
        b.progressLoading.visibility = View.VISIBLE
        b.dataContainer.visibility = View.GONE
        b.layoutStatusEvaluasi.visibility = View.GONE

        dataListener = lokasiRef.child(lokasi)
            .addValueEventListener(object : ValueEventListener {

                @SuppressLint("SetTextI18n")
                override fun onDataChange(snapshot: DataSnapshot) {
                    val b = _binding ?: return
                    if (!isAdded) return

                    b.progressLoading.visibility = View.GONE
                    b.dataContainer.visibility = View.VISIBLE
                    b.layoutStatusEvaluasi.visibility = View.VISIBLE

                    if (!snapshot.exists()) {
                        b.tvStatusEvaluasi.text = "Belum ada data sensor"
                        b.layoutStatusEvaluasi.setBackgroundResource(R.drawable.bg_status_warning)
                        b.icStatusEvaluasi.setImageResource(R.drawable.ic_info)
                        return
                    }

                    val fadeIn = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
                    b.dataContainer.startAnimation(fadeIn)

                    val params = listOf("pH", "Suhu", "TDS", "Kekeruhan")
                    val sensorEvaluations = mutableListOf<SensorEvaluation>()

                    for (param in params) {
                        val valueStr = snapshot.child(param).value?.toString() ?: "-"
                        val value = valueStr.toDoubleOrNull()
                        val (min, max) = ambangData[param] ?: (null to null)

                        val evaluation = SensorEvaluation(param, value, min, max)
                        sensorEvaluations.add(evaluation)

                        val statusView = getStatusView(param)
                        val minmaxView = getMinMaxView(param)
                        val valueView = getValueView(param)

                        valueView.text = when (param) {
                            "Suhu" -> "$valueStr°C"
                            "TDS" -> "$valueStr ppm"
                            "Kekeruhan" -> "$valueStr NTU"
                            else -> valueStr
                        }

                        minmaxView.text = "Min: ${min ?: "-"} | Max: ${max ?: "-"}"
                        statusView.text = evaluation.status

                        statusView.setBackgroundResource(
                            when (evaluation.status) {
                                "DALAM BATAS NORMAL" -> R.drawable.bg_status_safe
                                "DI BAWAH AMBANG", "DI ATAS AMBANG" -> R.drawable.bg_status_danger
                                else -> R.drawable.bg_status_warning
                            }
                        )
                    }

                    val lokasiStatus = evaluateWaterQuality(sensorEvaluations)
                    b.tvStatusEvaluasi.text = lokasiStatus

                    triggerAlert(lokasi, sensorEvaluations)
                    simpanKeRiwayat(lokasi, sensorEvaluations)
                }

                override fun onCancelled(error: DatabaseError) {
                    _binding?.progressLoading?.visibility = View.GONE
                }
            })
    }

    private fun simpanKeRiwayat(lokasi: String, sensorEvaluations: List<SensorEvaluation>) {
        val now = System.currentTimeMillis()
        val lastSave = lastSaveTimes[lokasi] ?: 0L

        // Hitung jumlah sensor yang error
        val triggered = sensorEvaluations.filter { it.isError() }
        val errorCount = triggered.size

        // 🔹 Tentukan interval berdasarkan kondisi
        val interval = if (errorCount == 0) 30_000L else 10_000L // 30 detik jika aman, 10 detik jika error

        // Batasi penyimpanan sesuai interval per lokasi
        if (now - lastSave < interval) return
        lastSaveTimes[lokasi] = now

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        // 🔹 Tentukan status umum
        val status = when {
            errorCount == 0 -> "AMAN"
            errorCount in 1..2 -> "PERINGATAN"
            else -> "KRITIKAL"
        }

        // 🔹 Deskripsi interaktif semua sensor
        val deskripsiBuilder = StringBuilder()
        deskripsiBuilder.append("Evaluasi kualitas air di $lokasi:\n")

        sensorEvaluations.forEachIndexed { index, s ->
            val kondisi = when {
                s.value == null -> "Data sensor ${s.name} tidak terbaca."
                s.value < (s.min ?: Double.MIN_VALUE) ->
                    "${s.name} terlalu rendah (${s.value} < ${s.min}) — indikasi kualitas menurun."
                s.value > (s.max ?: Double.MAX_VALUE) ->
                    "${s.name} terlalu tinggi (${s.value} > ${s.max}) — indikasi pencemaran air."
                else -> "${s.name}: ${s.value} (dalam batas normal)"
            }
            deskripsiBuilder.append("${index + 1}. $kondisi\n")
        }

        // 🔹 Siapkan data untuk Firebase
        val dataRiwayat = mutableMapOf<String, Any?>(
            "timestamp" to timestamp,
            "status" to status,
            "lokasi" to lokasi,
            "deskripsi" to deskripsiBuilder.toString().trim()
        )

        // Simpan semua nilai sensor (baik aman maupun error)
        sensorEvaluations.forEach { s ->
            dataRiwayat[s.name] = s.value
            dataRiwayat["${s.name}_min"] = s.min
            dataRiwayat["${s.name}_max"] = s.max
            dataRiwayat["${s.name}_status"] = s.status
        }

        // 🔹 Simpan ke Firebase di node sesuai lokasi
        val newKey = riwayatRef.child(lokasi).push().key ?: return
        riwayatRef.child(lokasi).child(newKey).setValue(dataRiwayat)
    }

    private fun triggerAlert(lokasiId: String, sensorEvaluations: List<SensorEvaluation>) {

        if (!isMonitoringActive || !isResumed) return

        val errorCount = sensorEvaluations.count { it.isError() }
        val triggered = sensorEvaluations.filter { it.isError() }.map { it.description }
        if (triggered.isEmpty()) return

        FirebaseDatabase.getInstance()
            .getReference("Lokasi")
            .child(lokasiId)
            .child("nama")
            .addListenerForSingleValueEvent(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    val namaLokasi = snapshot.getValue(String::class.java) ?: lokasiId

                    val intent = Intent(requireContext(), AlertService::class.java)
                    intent.putExtra("lokasi", namaLokasi)
                    intent.putStringArrayListExtra("triggeredSensors", ArrayList(triggered))

                    if (errorCount >= 2) {
                        // 🔊 Notif + Alarm
                        requireContext().startService(intent)
                        alarmSedangAktif = true
                    } else {
                        // 🔔 Notif saja (silent)
                        intent.putExtra("silent", true)
                        requireContext().startService(intent)

                        // Jika sebelumnya alarm bunyi, matikan
                        if (alarmSedangAktif) {
                            val stopIntent = Intent(requireContext(), AlertService::class.java).apply {
                                action = AlertService.ACTION_STOP_ALARM
                            }
                            requireContext().startService(stopIntent)
                            alarmSedangAktif = false
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun evaluateWaterQuality(sensorEvaluations: List<SensorEvaluation>): String {
        val errorCount = sensorEvaluations.count { it.isError() }
        val statusText: String
        val bgRes: Int
        val iconRes: Int

        when {
            errorCount == 0 -> {
                statusText = "Air Aman"
                bgRes = R.drawable.bg_status_safe
                iconRes = R.drawable.ic_checked
            }
            errorCount in 1..2 -> {
                statusText = "Perhatian: Beberapa parameter menyimpang"
                bgRes = R.drawable.bg_status_warning
                iconRes = R.drawable.ic_warning
            }
            else -> {
                statusText = "Kritikal: Indikasi Pencemaran Air!"
                bgRes = R.drawable.bg_status_danger
                iconRes = R.drawable.ic_danger
            }
        }

        binding.layoutStatusEvaluasi.setBackgroundResource(bgRes)
        binding.tvStatusEvaluasi.text = statusText
        binding.icStatusEvaluasi.setImageResource(iconRes)

        return statusText
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
        dataListener?.let { lokasiRef.removeEventListener(it) }
        ambangListener?.let { ambangRef.removeEventListener(it) }
        dataListener = null
        ambangListener = null
        _binding = null
        super.onDestroyView()
    }

    override fun onResume() {
        super.onResume()
        isMonitoringActive = true
    }

    override fun onPause() {
        super.onPause()
        isMonitoringActive = false

        val stopIntent = Intent(requireContext(), AlertService::class.java).apply {
            action = AlertService.ACTION_STOP_ALARM
        }
        requireContext().startService(stopIntent)
    }


}

// Data class evaluasi sensor akademis
data class SensorEvaluation(
    val name: String,
    val value: Double?,
    val min: Double?,
    val max: Double?
) {
    val status: String
        get() = when {
            value == null -> "DATA TIDAK TERSEDIA"
            value < (min ?: Double.MIN_VALUE) -> "DI BAWAH AMBANG"
            value > (max ?: Double.MAX_VALUE) -> "DI ATAS AMBANG"
            else -> "DALAM BATAS NORMAL"
        }

    val description: String
        get() = when (status) {
            "DI BAWAH AMBANG" -> "$name: $value < $min (indikasi kualitas menurun)"
            "DI ATAS AMBANG" -> "$name: $value > $max (indikasi pencemaran / kualitas turun)"
            "DALAM BATAS NORMAL" -> "$name: $value (Normal)"
            else -> "$name: Data tidak tersedia"
        }

    fun isError(): Boolean = status != "DALAM BATAS NORMAL"
}



