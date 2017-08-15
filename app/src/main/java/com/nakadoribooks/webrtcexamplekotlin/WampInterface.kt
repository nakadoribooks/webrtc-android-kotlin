package com.nakadoribooks.webrtcexamplekotlin

/**
 * Created by kawase on 2017/08/12.
 */

internal enum class Topic private constructor(val string: String) {

    Callme("com.nakadoribook.webrtc.[roomId].callme"),
    Close("com.nakadoribook.webrtc.[roomId].close"),
    Answer("com.nakadoribook.webrtc.[roomId].[userId].answer"),
    Offer("com.nakadoribook.webrtc.[roomId].[userId].offer"),
    Candidate("com.nakadoribook.webrtc.[roomId].[userId].candidate")
}

internal interface WampCallbacks {

    fun onOpen()
    fun onReceiveAnswer(targetId: String, sdp: String)
    fun onReceiveOffer(taretId: String, sdp: String)
    fun onIceCandidate(targetId: String, sdp: String, sdpMid: String, sdpMLineIndex: Int)
    fun onReceiveCallme(targetId: String)
    fun onCloseConnection(targetId: String)

}

interface WampInterface {

    fun connect()
    fun publishCallme()
    fun publishOffer(targetId: String, sdp: String)
    fun publishAnswer(targetId: String, sdp: String)
    fun publishCandidate(targetId: String, candidate: String)

}
