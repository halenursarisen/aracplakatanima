package com.example.kullaniciapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.example.kullaniciapp.R

class PlateAdapter(
    private val context: Context,
    private val plakaList: List<String>,
    private val onEditClick: (String) -> Unit
) : BaseAdapter() {

    override fun getCount(): Int = plakaList.size

    override fun getItem(position: Int): Any = plakaList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_plate, parent, false)
        val textViewPlate = view.findViewById<TextView>(R.id.textViewPlate)
        val imageViewEdit = view.findViewById<ImageView>(R.id.imageViewEdit)

        val plaka = plakaList[position]
        textViewPlate.text = plaka

        imageViewEdit.setOnClickListener {
            onEditClick(plaka)
        }

        return view
    }
}
