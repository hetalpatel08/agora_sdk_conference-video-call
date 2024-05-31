package com.example.conferencevideocall.activity

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.conferencevideocall.R
import com.example.conferencevideocall.adpter.VideoAdapter
import com.example.conferencevideocall.databinding.ActivityMainBinding
import io.agora.rtc.IRtcEngineEventHandler
import io.agora.rtc.RtcEngine
import io.agora.rtc.video.VideoCanvas
import io.agora.rtc.video.VideoEncoderConfiguration

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private lateinit var agoraEngine: RtcEngine
    private lateinit var adapter: VideoAdapter
    private val uIds = mutableListOf<Int>()
    private val participantsNames = mutableMapOf<Int, String>()
    private var isJoined = false
    private var isVideoEnabled = true
    private var isVideoHeld = false
    private val APP_ID = "5d7b925933b343d19ec110d0d895456b"
    private val token =
        "YOUR_AGORA_TOKEN"
    private val channelName = "a-meet"
    private val PERMISSION_REQ_ID_RECORD_AUDIO = 22
    /*todo:for video calling  request*/

    private val PERMISSION_REQ_ID_CAMERA = 23
    private var hostName: String = ""
    private var localUid: Int = -1


    private val rtcEventHandler = object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            runOnUiThread {
                localUid = uid
                participantsNames[uid] = hostName
                isJoined = true

            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                binding.tvParticiantsTitle.visibility = View.VISIBLE

                if (uid != localUid) {
                    val audienceName = "Participants $uid" //user name
                    uIds.add(uid)
                    participantsNames[uid] = audienceName
                    adapter.notifyDataSetChanged()
                }
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                uIds.remove(uid)
                participantsNames.remove(uid)
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO, PERMISSION_REQ_ID_RECORD_AUDIO)
            &&
            checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)
        ) {
            initializeAgoraEngine()
            setupLocalVideo()
            setupRecyclerView()
            setupClickListeners()

        }
    }

    private fun setupRecyclerView() {

        binding.recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        adapter = VideoAdapter(this, agoraEngine, uIds, participantsNames)
        binding.recyclerView.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.ivJoin.setOnClickListener {
            if (isJoined) leaveChannel() else joinChannel()
            binding.ivJoin.isEnabled = !isJoined
            hideKeyboard()
        }

        binding.ivDecline.setOnClickListener {
            leaveChannel()
            binding.ivJoin.isEnabled = true // Enable the Join button
        }

        binding.rlVideoHold.setOnClickListener {
            if (isVideoHeld) {
                agoraEngine.muteLocalVideoStream(false)
                println("Video resumed")
            } else {
                agoraEngine.muteLocalVideoStream(true)
                println("Video on hold")
            }
            isVideoHeld = !isVideoHeld
        }

        binding.ivFlipCamera.setOnClickListener {
            agoraEngine.switchCamera()
        }

        binding.ivDisableVideo.setOnClickListener {
            isVideoEnabled = !isVideoEnabled
            agoraEngine.muteLocalVideoStream(!isVideoEnabled)
            binding.ivDisableVideo.setImageResource(if (isVideoEnabled) R.drawable.ic_enable_video else R.drawable.ic_disable_video)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        var view = currentFocus ?: View(this)
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun initializeAgoraEngine() {
        try {
            agoraEngine = RtcEngine.create(baseContext, APP_ID, rtcEventHandler)
            agoraEngine.enableVideo()//for video call
            agoraEngine.setVideoEncoderConfiguration(
                VideoEncoderConfiguration(
                    VideoEncoderConfiguration.VD_1280x720, // Set resolution to 1280x720
                    VideoEncoderConfiguration.FRAME_RATE.FRAME_RATE_FPS_30, // Set frame rate to 30 FPS
                    VideoEncoderConfiguration.STANDARD_BITRATE,
                    VideoEncoderConfiguration.ORIENTATION_MODE.ORIENTATION_MODE_ADAPTIVE
                )
            )
        } catch (e: Exception) {
            throw RuntimeException("Check the error.")
        }
    }

    private fun setupLocalVideo() {
        val surfaceView = RtcEngine.CreateRendererView(this)
        surfaceView.setZOrderMediaOverlay(true)
        binding.localVideoContainer.addView(surfaceView)
        agoraEngine.setupLocalVideo(VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0))
    }

    override fun onDestroy() {
        super.onDestroy()
        RtcEngine.destroy()
    }

    private fun joinChannel() {
        if (hostName.isNotEmpty()) {
            binding.tvCallerName.text = hostName
        } else {
            binding.tvCallerName.text = buildString {
                append("Hello... !!")
            }
        }

        binding.ivUserImage.visibility = View.GONE
        binding.localVideoContainer.visibility = View.VISIBLE
        binding.cdLocalVideo.visibility = View.VISIBLE
        binding.ivDisableVideo.visibility = View.VISIBLE
        binding.ivFlipCamera.visibility = View.VISIBLE

        binding.ivFlipCamera.setOnClickListener {
            agoraEngine.switchCamera()
            println("Camera switched")

        }

        binding.ivDisableVideo.setOnClickListener {
            if (isVideoEnabled) {
                agoraEngine.disableVideo()
                agoraEngine.muteLocalVideoStream(true)
                binding.ivDisableVideo.setImageResource(R.drawable.ic_disable_video)
            } else {
                agoraEngine.enableVideo()
                setupLocalVideo() // Setup local video again after enabling
                agoraEngine.muteLocalVideoStream(false)
                binding.ivDisableVideo.setImageResource(R.drawable.ic_enable_video)
            }
            isVideoEnabled = !isVideoEnabled
        }
        agoraEngine.joinChannel(token, channelName, "", 0)
    }

    private fun leaveChannel() {
        agoraEngine.leaveChannel()
        binding.localVideoContainer.removeAllViews()
        binding.ivUserImage.visibility = View.VISIBLE
        binding.localVideoContainer.visibility = View.GONE
        binding.cdLocalVideo.visibility = View.GONE
        uIds.clear()
        participantsNames.clear()
        adapter.notifyDataSetChanged()
        isJoined = false
        setupLocalVideo() // Re-setup local video after leaving the channel
        binding.ivDisableVideo.visibility = View.GONE
        binding.ivFlipCamera.visibility = View.GONE
    }

    private fun checkSelfPermission(permission: String, requestCode: Int): Boolean {
        return if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQ_ID_RECORD_AUDIO -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    /*todo: for video call*/

                    checkSelfPermission(Manifest.permission.CAMERA, PERMISSION_REQ_ID_CAMERA)
                    initializeAgoraEngine()
                    setupRecyclerView()
                    setupClickListeners()

                } else {
                    finish()
                }
            }
            /*todo: for video call*/

            PERMISSION_REQ_ID_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initializeAgoraEngine()
                    setupLocalVideo()
                    setupRecyclerView()
                    setupClickListeners()
                } else {
                    finish()
                }
            }
        }
    }
}