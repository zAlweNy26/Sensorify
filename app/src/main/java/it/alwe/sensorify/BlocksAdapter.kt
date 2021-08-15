package it.alwe.sensorify

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView

class BlocksAdapter(private val mContext: Context, private val entries: Array<Int>, private val icons: Array<Int>) : BaseAdapter() {
    private val layoutInflater = LayoutInflater.from(mContext)

    override fun getCount(): Int { return entries.size }

    override fun getItemId(position: Int): Long { return position.toLong() }

    override fun getItem(position: Int): Any { return position }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val currentView: View = layoutInflater.inflate(R.layout.home_block, parent, false)
        val blockInfo = currentView.findViewById<TextView>(R.id.home_block)
        val blockIcon = currentView.findViewById<ImageView>(R.id.blockIcon)
        blockInfo.text = mContext.getString(entries[position])
        blockIcon.setImageResource(icons[position])
        blockIcon.contentDescription = mContext.getString(entries[position])
        return currentView
        /*if (convertView == null) {
            val currentView: View = layoutInflater.inflate(R.layout.home_block, parent, false)
            val blockInfo = currentView.findViewById<TextView>(R.id.home_block)
            val blockIcon = currentView.findViewById<ImageView>(R.id.blockIcon)
            blockInfo.text = mContext.getString(entries[position])
            blockIcon.setImageResource(icons[position])
            blockIcon.contentDescription = mContext.getString(entries[position])
            return currentView
        }
        return convertView*/
    }
}