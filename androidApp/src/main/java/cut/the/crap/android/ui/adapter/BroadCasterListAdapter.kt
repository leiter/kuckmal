package cut.the.crap.android.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import cut.the.crap.android.R
import cut.the.crap.android.model.Broadcaster

class BroadCasterListAdapter(
    context: Context,
    channels: List<Broadcaster>
) : ArrayAdapter<Broadcaster>(context, R.layout.list_element, channels) {

    private val inflater: LayoutInflater =
        context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val rowView = convertView ?: inflater.inflate(R.layout.list_element, parent, false)

        val imageView = rowView.findViewById<ImageView>(R.id.icon)
        val textView = rowView.findViewById<TextView>(R.id.senderName)

        // Get channel icon resource ID directly
        val iconRes = Broadcaster.Companion.getChannelIcon(position)
        imageView.setImageResource(iconRes)

        // Set the channel name text
        textView.text = Broadcaster.Companion.getChannelName(position)

        // Update activated state based on ListView's selection
        // The selector drawable responds to the activated state
        val listView = parent as? ListView
        rowView.isActivated = listView?.isItemChecked(position) == true

        return rowView
    }
}