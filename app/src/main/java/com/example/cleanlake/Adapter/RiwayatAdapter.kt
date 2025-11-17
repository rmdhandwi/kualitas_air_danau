package com.example.cleanlake.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
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
        val icStatus: ImageView = view.findViewById(R.id.icStatus)
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

        // Tampilkan tanggal & deskripsi dengan fallback
        holder.tvTanggal.text = data.timestamp ?: "-"
        holder.tvDeskripsi.text = data.deskripsi ?: "Tidak ada deskripsi"
        val status = data.status?.uppercase() ?: "-"

        // 💡 Tampilkan status + warna + ikon sesuai kondisi
        when (status) {
            "AMAN" -> {
                holder.tvStatus.text = "AMAN"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.safe))
                holder.icStatus.setImageResource(R.drawable.ic_checked)
                holder.icStatus.setColorFilter(ContextCompat.getColor(context, R.color.safe))
            }
            "PERINGATAN" -> {
                holder.tvStatus.text = "PERINGATAN"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.warning))
                holder.icStatus.setImageResource(R.drawable.ic_warning)
                holder.icStatus.setColorFilter(ContextCompat.getColor(context, R.color.warning))
            }
            "BAHAYA", "KRITIKAL" -> {
                holder.tvStatus.text = "BAHAYA"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.danger))
                holder.icStatus.setImageResource(R.drawable.ic_danger)
                holder.icStatus.setColorFilter(ContextCompat.getColor(context, R.color.danger))
            }
            else -> {
                holder.tvStatus.text = "-"
                holder.tvStatus.setTextColor(ContextCompat.getColor(context, R.color.gray_700))
                holder.icStatus.setImageResource(R.drawable.ic_info)
                holder.icStatus.setColorFilter(ContextCompat.getColor(context, R.color.gray_700))
            }
        }

        // 🧩 Isi nilai grid sensor dengan warna adaptif
        setGrid(holder.phLayout, "pH", data.pH, data.pH_min, data.pH_max)
        setGrid(holder.suhuLayout, "Suhu (°C)", data.Suhu, data.Suhu_min, data.Suhu_max)
        setGrid(holder.tdsLayout, "TDS (ppm)", data.TDS, data.TDS_min, data.TDS_max)
        setGrid(holder.kekeruhanLayout, "Kekeruhan (NTU)", data.Kekeruhan, data.Kekeruhan_min, data.Kekeruhan_max)

        // ✨ Animasi halus (tanpa berulang saat scroll)
        if (!holder.itemView.isShown) {
            holder.itemView.alpha = 0f
            holder.itemView.animate()
                .alpha(1f)
                .setDuration(400)
                .start()
        }
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
