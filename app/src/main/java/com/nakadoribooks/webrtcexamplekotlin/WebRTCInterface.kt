package com.nakadoribooks.webrtcexamplekotlin

import org.webrtc.MediaStream

/**
 * Created by kawase on 2017/08/12.
 */

internal interface WebRTCCallbacks {

    fun onCreateOffer(sdp: String)
    fun onCreateAnswer(sdp: String)
    fun onAddedStream(mediaStream: MediaStream)
    fun onIceCandidate(sdp: String, sdpMid: String, sdpMLineIndex: Int)

}

interface WebRTCInterface {

    fun createOffer()
    fun receiveOffer(sdp: String)
    fun receiveAnswer(sdp: String)
    fun receiveCandidate(sdp: String, sdpMid: String, sdpMLineIndex: Int)
    fun close()

}
