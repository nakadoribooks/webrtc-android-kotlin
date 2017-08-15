package com.nakadoribooks.webrtcexamplekotlin

import android.Manifest
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.widget.GridLayout

import org.webrtc.EglBase
import org.webrtc.MediaStream
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoRenderer
import org.webrtc.VideoTrack

import java.util.ArrayList
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private var wamp: WampInterface? = null

    private val connectionList = ArrayList<Connection>()

    private val userId = UUID.randomUUID().toString().substring(0, 8)
    private val eglBase = EglBase.create()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupWamp()

        // checkPermission → onRequestPermissionsResult → startWebRTC
        checkPermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        if (requestCode != REQUEST_CODE_CAMERA_PERMISSION) {
            return
        }

        WebRTC.setup(this, eglBase)

        // show localView
        val localStream = WebRTC.localStream
        val renderer = findViewById(R.id.local_render_view) as SurfaceViewRenderer
        val localRenderer = setupRenderer(renderer)
        localStream!!.videoTracks.first.addRenderer(localRenderer)

        wamp!!.connect()
    }

    private fun checkPermission() {
        val permissioins = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        ActivityCompat.requestPermissions(this, permissioins, REQUEST_CODE_CAMERA_PERMISSION)
    }

    private fun setupWamp() {
        // wamp
        val roomId = "abcdef" // とりあえず固定
        wamp = Wamp(this, roomId, userId, object : WampCallbacks {
            override fun onOpen() {
                wamp!!.publishCallme()
            }

            override fun onReceiveAnswer(targetId: String, sdp: String) {

                val connection = findConnection(targetId)
                if (connection == null) {
                    Log.d("onReceiveAnswer", "not found connection")
                    return
                }

                connection!!.receiveAnswer(sdp)
            }

            override fun onReceiveOffer(targetId: String, sdp: String) {
                val connection = createConnection(targetId)
                connection.receiveOffer(sdp)
            }

            override fun onIceCandidate(targetId: String, candidate: String, sdpMid: String, sdpMLineIndex: Int) {
                val connection = createConnection(targetId)
                connection.receiveCandidate(candidate, sdpMid, sdpMLineIndex)
            }

            override fun onReceiveCallme(targetId: String) {
                val connection = createConnection(targetId)
                connection.publishOffer()
            }

            override fun onCloseConnection(targetId: String) {

            }
        })
    }

    private var remoteIndex = 0

    private fun createConnection(targetId: String): Connection {
        val connection = Connection(userId, targetId, wamp!!, object : ConnectionCallbacks {
            override fun onAddedStream(mediaStream: MediaStream) {
                if (mediaStream.videoTracks.size == 0) {
                    Log.e("createConnection", "noVideoTracks")
                    return
                }

                val remoteVideoTrack = mediaStream.videoTracks.first

                this@MainActivity.runOnUiThread {
                    val remoteRenderer = SurfaceViewRenderer(this@MainActivity)

                    val row = remoteIndex / 2
                    val col = remoteIndex % 2

                    val params = GridLayout.LayoutParams()
                    params.columnSpec = GridLayout.spec(col, 1)
                    params.rowSpec = GridLayout.spec(row, 1)
                    params.width = 500
                    params.height = 500
                    params.leftMargin = 10
                    params.rightMargin = 10
                    params.topMargin = 10

                    remoteRenderer.layoutParams = params

                    val videoRenderer = setupRenderer(remoteRenderer)
                    remoteVideoTrack.addRenderer(videoRenderer)

                    val remoteViewContainer = this@MainActivity.findViewById(R.id.remote_view_container) as GridLayout
                    remoteViewContainer.addView(remoteRenderer)

                    remoteIndex = remoteIndex + 1
                }
            }
        })

        connectionList.add(connection)
        return connection
    }

    private fun findConnection(targetId: String): Connection? {

        var i = 0
        val max = connectionList.size
        while (i < max) {
            val connection = connectionList[i]
            if (connection.targetId().equals(targetId)) {
                return connection
            }
            i++
        }

        Log.d("not found", connectionList.toString())
        return null
    }

    private fun setupRenderer(renderer: SurfaceViewRenderer): VideoRenderer {

        renderer.init(eglBase.eglBaseContext, null)
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
        renderer.setZOrderMediaOverlay(true)
        renderer.setEnableHardwareScaler(true)

        return VideoRenderer(renderer)
    }

    companion object {

        private val REQUEST_CODE_CAMERA_PERMISSION = 1
    }

}
