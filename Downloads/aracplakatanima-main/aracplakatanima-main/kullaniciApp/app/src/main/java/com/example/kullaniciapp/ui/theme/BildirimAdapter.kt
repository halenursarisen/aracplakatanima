package com.example.kullaniciapp.ui.theme

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.kullaniciapp.R

class BildirimAdapter(
    private val bildirimList: MutableList<Bildirim>
) : RecyclerView.Adapter<BildirimAdapter.BildirimViewHolder>() {

    class BildirimViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textMessage: TextView = itemView.findViewById(R.id.textMessage)
        val textTime: TextView = itemView.findViewById(R.id.textTime)
        val buttonEdit: ImageButton = itemView.findViewById(R.id.buttonEdit)
        val buttonDelete: ImageButton = itemView.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BildirimViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bildirim, parent, false)
        return BildirimViewHolder(view)
    }

    override fun onBindViewHolder(holder: BildirimViewHolder, position: Int) {
        val bildirim = bildirimList[position]

        holder.textMessage.text = bildirim.mesaj
        holder.textTime.text = bildirim.zaman

        // Bildirim tipine göre edit butonuna ikon ve renk veriyoruz
        when (bildirim.tip) {
            "success" -> {
                holder.buttonEdit.setImageResource(R.drawable.ic_success)
                holder.buttonEdit.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.green))
            }
            "warning" -> {
                holder.buttonEdit.setImageResource(R.drawable.ic_warning)
                holder.buttonEdit.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.orange))
            }
            "system" -> {
                holder.buttonEdit.setImageResource(R.drawable.ic_system)
                holder.buttonEdit.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.blue))
            }
            else -> {
                holder.buttonEdit.setImageResource(R.drawable.ic_info)
                holder.buttonEdit.setColorFilter(ContextCompat.getColor(holder.itemView.context, R.color.gray))
            }
        }

        // Animasyon örneği (buttonEdit'e uygulanıyor)
        val blinkAnimation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.blink)
        holder.buttonEdit.startAnimation(blinkAnimation)

        // Silme işlemi (buttonDelete tıklanınca)
        holder.buttonDelete.setOnClickListener {
            bildirimList.removeAt(holder.adapterPosition)
            notifyItemRemoved(holder.adapterPosition)
        }
    }

    override fun getItemCount(): Int = bildirimList.size
}
