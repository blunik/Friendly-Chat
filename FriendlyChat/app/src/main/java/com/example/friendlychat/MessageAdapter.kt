package com.example.friendlychat

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide

class MessageAdapter(context: Context, resource: Int, objects: List<FriendlyMessage>)
    : ArrayAdapter<FriendlyMessage> (context, resource, objects){

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            convertView = (context as Activity).layoutInflater.inflate(R.layout.item_message
                , parent, false)
        }
        val photoImageView: ImageView = convertView?.findViewById(R.id.photoImageView) as ImageView
        val messageTextView: TextView = convertView?.findViewById(R.id.messageTextView) as TextView
        val authorTextView: TextView = convertView?.findViewById(R.id.nameTextView) as TextView

        val message: FriendlyMessage? = getItem(position)

        val isPhoto: Boolean = message?.getPhotoUrl() != null
        if (isPhoto){
            messageTextView.visibility = View.GONE
            photoImageView.visibility = View.VISIBLE
            Glide.with(photoImageView.context).load(message!!.getPhotoUrl())
                .into(photoImageView)
        } else {
            messageTextView.visibility = View.VISIBLE
            photoImageView.visibility = View.GONE
            messageTextView.text = message?.getText()
        }
        authorTextView.text = message!!.getName()
        return convertView
    }
}
