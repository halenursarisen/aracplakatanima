package com.example.kullaniciapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class UserListAdapter(
    private val context: Context,
    private val userList: MutableList<Pair<String, String>>
) : BaseAdapter() {

    override fun getCount(): Int = userList.size

    override fun getItem(position: Int): Any = userList[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view = convertView ?: LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_1, parent, false)
        val textViewUser = view.findViewById<TextView>(android.R.id.text1)

        val (userText, _) = userList[position]
        textViewUser.text = userText

        return view
    }
}
