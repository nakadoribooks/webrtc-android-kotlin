package com.nakadoribooks.webrtcexamplekotlin

import org.webrtc.MediaStream

/**
 * Created by kawase on 2017/08/12.
 */

internal interface ConnectionCallbacks {

    fun onAddedStream(mediaStream: MediaStream)

}

interface ConnectionInterface {

    fun targetId(): String
    fun publishOffer()
    fun receiveOffer(sdp: String)
    fun receiveAnswer(sdp: String)
    fun receiveCandidate(candidate: String, sdpMid: String, sdpMLineIndex: Int)
    fun close()

}
