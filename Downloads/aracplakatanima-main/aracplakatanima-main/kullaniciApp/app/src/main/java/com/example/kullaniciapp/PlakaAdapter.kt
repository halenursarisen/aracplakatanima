package com.example.kullaniciapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlakaAdapter(
    private val plakaList: MutableList<String>,
    private val onPlakaClick: (String) -> Unit,
    private val onDeleteClick: (String, Int) -> Unit  // plaka + pozisyon
) : RecyclerView.Adapter<PlakaAdapter.PlakaViewHolder>() {

    inner class PlakaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val plakaText: TextView = itemView.findViewById(R.id.textPlaka)
        val detailsButton: ImageButton = itemView.findViewById(R.id.buttonDetails)
        val deleteButton: ImageButton = itemView.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlakaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plaka, parent, false)
        return PlakaViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlakaViewHolder, position: Int) {
        val plaka = plakaList[position]
        holder.plakaText.text = plaka

        // Detay (ok butonu) tıklaması
        holder.detailsButton.setOnClickListener {
            onPlakaClick(plaka)
        }

        // Silme (kırmızı çarpı) tıklaması
        holder.deleteButton.setOnClickListener {
            onDeleteClick(plaka, position)
        }
    }

    override fun getItemCount(): Int = plakaList.size
}
