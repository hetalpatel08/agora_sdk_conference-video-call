package com.example.conferencevideocall.adpter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.conferencevideocall.R
import io.agora.rtc.Constants.RENDER_MODE_HIDDEN
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas

class  VideoAdapter(
    private val context: Context,
    private val rtcEngine: RtcEngine,
    private val uids: List<Int>,
    private val userNames: Map<Int, String>

) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_video, parent, false)
        return VideoViewHolder(view)
    }


    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val uid = uids[position]
        holder.bind(uid)
    }

    override fun getItemCount(): Int = uids.size
    inner class VideoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val videoContainer: FrameLayout = itemView.findViewById(R.id.videoContainer)
        private val userNameTextView: TextView = itemView.findViewById(R.id.user_name)

        fun bind(uid: Int) {
            val surfaceView = RtcEngine.CreateRendererView(context)
            surfaceView.setZOrderMediaOverlay(true)
            videoContainer.removeAllViews() // Clear any existing views
            videoContainer.addView(surfaceView)

            rtcEngine.setupRemoteVideo(VideoCanvas(surfaceView, RENDER_MODE_HIDDEN, uid))
            userNameTextView.text = userNames[uid] ?: "Unknown User"
        }
    }
}