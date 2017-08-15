package com.nakadoribooks.webrtcexamplekotlin

import org.json.JSONObject
import org.webrtc.MediaStream

/**
 * Created by kawase on 2017/08/11.
 */

class Connection internal constructor(private val myId: String, private val _targetId: String, private val wamp: WampInterface, private val callbacks: ConnectionCallbacks) : ConnectionInterface {
    private val webRTC: WebRTCInterface

    init {

        this.webRTC = WebRTC(object : WebRTCCallbacks {

            override fun onCreateOffer(sdp: String) {

                try {
                    val json = JSONObject()
                    json.put("sdp", sdp)
                    json.put("type", "offer")
                    wamp.publishOffer(targetId(), json.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            override fun onCreateAnswer(sdp: String) {

                try {
                    val json = JSONObject()
                    json.put("sdp", sdp)
                    json.put("type", "answer")
                    wamp.publishAnswer(targetId(), json.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }

            override fun onAddedStream(mediaStream: MediaStream) {
                callbacks.onAddedStream(mediaStream)
            }

            override fun onIceCandidate(sdp: String, sdpMid: String, sdpMLineIndex: Int) {

                val json = JSONObject()
                try {
                    json.put("type", "candidate")
                    json.put("candidate", sdp)
                    json.put("sdpMid", sdpMid)
                    json.put("sdpMLineIndex", sdpMLineIndex)

                    wamp.publishCandidate(targetId(), json.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        })
    }

    // ▼ interface

    override fun targetId(): String {
        return _targetId
    }

    override fun publishOffer() {
        webRTC.createOffer()
    }

    override fun receiveOffer(sdp: String) {
        webRTC.receiveOffer(sdp)
    }

    override fun receiveAnswer(sdp: String) {
        webRTC.receiveAnswer(sdp)
    }

    override fun receiveCandidate(candidate: String, sdpMid: String, sdpMLineIndex: Int) {
        webRTC.receiveCandidate(candidate, sdpMid, sdpMLineIndex)
    }

    override fun close() {
        webRTC.close()
    }

}


