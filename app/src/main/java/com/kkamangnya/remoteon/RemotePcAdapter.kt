package com.kkamangnya.remoteon

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

class RemotePcAdapter(
    private val onWake: (RemotePc) -> Unit,
    private val onPing: (RemotePc) -> Unit,
    private val onDelete: (RemotePc) -> Unit,
    private val onEdit: (RemotePc) -> Unit
) : ListAdapter<RemotePc, RemotePcAdapter.RemotePcViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RemotePcViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_remote_pc, parent, false)
        return RemotePcViewHolder(view)
    }

    override fun onBindViewHolder(holder: RemotePcViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class RemotePcViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val root = itemView
        private val nameText: TextView = itemView.findViewById(R.id.pcNameText)
        private val macText: TextView = itemView.findViewById(R.id.pcMacText)
        private val ipText: TextView = itemView.findViewById(R.id.pcIpText)
        private val broadcastText: TextView = itemView.findViewById(R.id.pcBroadcastText)
        private val statusText: TextView = itemView.findViewById(R.id.pcStatusText)
        private val statusDot: ImageView = itemView.findViewById(R.id.pcStatusDot)
        private val wakeButton: Button = itemView.findViewById(R.id.wakeButton)
        private val pingButton: Button = itemView.findViewById(R.id.pingButton)
        private val deleteButton: Button = itemView.findViewById(R.id.deleteButton)

        fun bind(pc: RemotePc) {
            root.setOnClickListener { onEdit(pc) }
            nameText.text = pc.name
            macText.text = pc.macAddress
            ipText.text = pc.ipAddress
            broadcastText.text = pc.broadcastAddress
            statusText.text = when (pc.connectionState) {
                ConnectionState.Online -> "온라인"
                ConnectionState.Offline -> "오프라인"
                ConnectionState.Unknown -> "대기"
            }
            val color = when (pc.connectionState) {
                ConnectionState.Online -> R.color.state_online
                ConnectionState.Offline -> R.color.state_offline
                ConnectionState.Unknown -> R.color.state_unknown
            }
            statusDot.setColorFilter(ContextCompat.getColor(itemView.context, color))
            wakeButton.setOnClickListener { onWake(pc) }
            pingButton.setOnClickListener { onPing(pc) }
            deleteButton.setOnClickListener { onDelete(pc) }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<RemotePc>() {
        override fun areItemsTheSame(oldItem: RemotePc, newItem: RemotePc): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: RemotePc, newItem: RemotePc): Boolean = oldItem == newItem
    }
}

