package com.example.cleanlake.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.cleanlake.Model.RiwayatModel
import com.example.cleanlake.R

class RiwayatAdapter(
    private val context: Context,
    private val listRiwayat: MutableList<RiwayatModel>
) : RecyclerView.Adapter<RiwayatAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view as CardView
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDeskripsi: TextView = view.findViewById(R.id.tvDeskripsi)
        val phLayout: View = view.findViewById(R.id.itemPh)
        val suhuLayout: View = view.findViewById(R.id.itemSuhu)
        val tdsLayout: View = view.findViewById(R.id.itemTds)
        val kekeruhanLayout: View = view.findViewById(R.id.itemKekeruhan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_riwayat, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = listRiwayat[position]
        holder.tvTanggal.text = data.timestamp ?: "-"
        holder.tvStatus.text = data.status ?: "-"
        holder.tvDeskripsi.text = data.deskripsi ?: "-"

        // Ubah warna status
        when (data.status) {
            "AMAN" -> holder.tvStatus.setBackgroundResource(R.drawable.bg_status_safe)
            "BAHAYA" -> holder.tvStatus.setBackgroundResource(R.drawable.bg_status_danger)
            else -> holder.tvStatus.setBackgroundResource(R.drawable.bg_status_warning)
        }

        // Isi nilai-nilai grid
        setGrid(holder.phLayout, "pH", data.pH, data.pH_min, data.pH_max)
        setGrid(holder.suhuLayout, "Suhu (°C)", data.Suhu, data.Suhu_min, data.Suhu_max)
        setGrid(holder.tdsLayout, "TDS (ppm)", data.TDS, data.TDS_min, data.TDS_max)
        setGrid(holder.kekeruhanLayout, "Kekeruhan (NTU)", data.Kekeruhan, data.Kekeruhan_min, data.Kekeruhan_max)

        // Animasi lembut tiap item muncul
        val anim = AnimationUtils.loadAnimation(context, R.anim.fade_in)
        holder.cardView.startAnimation(anim)
    }

    @SuppressLint("SetTextI18n")
    private fun setGrid(view: View, label: String, value: Double?, min: Double?, max: Double?) {
        val tvLabel = view.findViewById<TextView>(R.id.tvLabel)
        val tvValue = view.findViewById<TextView>(R.id.tvValue)
        val tvRange = view.findViewById<TextView>(R.id.tvRange)

        tvLabel.text = label
        tvValue.text = value?.toString() ?: "-"
        tvRange.text = "Min: ${min ?: "-"} | Max: ${max ?: "-"}"
    }

    override fun getItemCount(): Int = listRiwayat.size
}
