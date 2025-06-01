package com.example.kullaniciapp.ui.theme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kullaniciapp.R

class BildirimAdapter(
    private val bildirimList: MutableList<Bildirim>,
    private val onDeleteClick: ((position: Int) -> Unit)? = null
) : RecyclerView.Adapter<BildirimAdapter.BildirimViewHolder>() {

    class BildirimViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textViewMesaj: TextView = itemView.findViewById(R.id.textViewMesaj)
        val textViewZaman: TextView = itemView.findViewById(R.id.textViewZaman)
        val iconTip: ImageView = itemView.findViewById(R.id.iconTip)
        val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BildirimViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bildirim, parent, false)
        return BildirimViewHolder(view)
    }

    override fun onBindViewHolder(holder: BildirimViewHolder, position: Int) {
        val bildirim = bildirimList[position]
        val context = holder.itemView.context

        holder.textViewMesaj.text = bildirim.mesaj
        holder.textViewZaman.text = "🕒 ${bildirim.zaman}"

        // Tip ikonunu ve arka planı ayarla
        val backgroundColor: Int
        when (bildirim.tip.lowercase()) {
            "success" -> {
                holder.iconTip.setImageResource(R.drawable.ic_success)
                backgroundColor = ContextCompat.getColor(context, R.color.success_bg)
            }
            "warning" -> {
                holder.iconTip.setImageResource(R.drawable.ic_warning)
                backgroundColor = ContextCompat.getColor(context, R.color.warning_bg)
            }
            "system" -> {
                holder.iconTip.setImageResource(R.drawable.ic_system)
                backgroundColor = ContextCompat.getColor(context, R.color.system_bg)
            }
            else -> {
                holder.iconTip.setImageResource(R.drawable.ic_info)
                backgroundColor = ContextCompat.getColor(context, R.color.default_bg)
            }
        }
        holder.itemView.setBackgroundColor(backgroundColor)

        // Silme işlemi
        holder.buttonDelete.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onDeleteClick?.invoke(pos)
                bildirimList.removeAt(pos)
                notifyItemRemoved(pos)
            }
        }
    }

    override fun getItemCount(): Int = bildirimList.size
}
