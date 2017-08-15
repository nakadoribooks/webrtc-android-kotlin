package com.nakadoribooks.webrtcexamplekotlin

import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager

import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.DataChannel
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.VideoCapturer
import org.webrtc.VideoRenderer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack

import java.util.Arrays

/**
 * Created by kawase on 2017/04/13.
 */

class WebRTC internal constructor(private val callbacks: WebRTCCallbacks) : PeerConnection.Observer, WebRTCInterface {

    private abstract class SkeletalSdpObserver : SdpObserver {

        override fun onCreateSuccess(sessionDescription: SessionDescription) {}
        override fun onSetSuccess() {}
        override fun onCreateFailure(s: String) {}
        override fun onSetFailure(s: String) {}

        companion object {

            private val TAG = "SkeletalSdpObserver"
        }
    }

    private var peerConnection: PeerConnection? = null

    init {

        // create PeerConnection
        val iceServers = Arrays.asList(PeerConnection.IceServer("stun:stun.l.google.com:19302"))
        peerConnection = factory!!.createPeerConnection(iceServers, WebRTCUtil.peerConnectionConstraints(), this)
        peerConnection!!.addStream(localStream)
    }

    override fun createOffer() {
        peerConnection!!.createOffer(object : SkeletalSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                peerConnection!!.setLocalDescription(object : SkeletalSdpObserver() {

                    override fun onSetSuccess() {
                        callbacks.onCreateOffer(sessionDescription.description)
                    }

                }, sessionDescription)
            }
        }, WebRTCUtil.offerConnectionConstraints())
    }

    override fun receiveOffer(sdp: String) {

        // setRemoteDescription
        val remoteDescription = SessionDescription(SessionDescription.Type.OFFER, sdp)
        peerConnection!!.setRemoteDescription(object : SkeletalSdpObserver() {
            override fun onSetSuccess() {

                // createAnswer
                peerConnection!!.createAnswer(object : SkeletalSdpObserver() {
                    override fun onCreateSuccess(sessionDescription: SessionDescription) {
                        peerConnection!!.setLocalDescription(object : SkeletalSdpObserver() {

                            override fun onSetSuccess() {
                                callbacks.onCreateAnswer(sessionDescription.description)
                            }

                        }, sessionDescription)
                    }
                }, WebRTCUtil.answerConnectionConstraints())

            }

            override fun onSetFailure(s: String) {
                Log.d("WebRTC", " ------------ onSetFailure ----------------")
                Log.d("WebRTC", s)
            }
        }, remoteDescription)
    }

    override fun receiveAnswer(sdp: String) {
        val remoteDescription = SessionDescription(SessionDescription.Type.ANSWER, sdp)
        peerConnection!!.setRemoteDescription(object : SkeletalSdpObserver() {
            override fun onSetSuccess() {

            }
        }, remoteDescription)
    }

    override fun receiveCandidate(sdp: String, sdpMid: String, sdpMLineIndex: Int) {
        val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        peerConnection!!.addIceCandidate(iceCandidate)
    }

    override fun close() {
        peerConnection!!.removeStream(WebRTC.localStream)
        peerConnection!!.close()
        peerConnection = null
    }

    // PeerConnection.Observer -----

    override fun onSignalingChange(signalingState: PeerConnection.SignalingState) {}
    override fun onIceConnectionChange(iceConnectionState: PeerConnection.IceConnectionState) {}
    override fun onIceConnectionReceivingChange(b: Boolean) {}
    override fun onRemoveStream(mediaStream: MediaStream) {}
    override fun onDataChannel(dataChannel: DataChannel) {}
    override fun onRenegotiationNeeded() {}
    override fun onAddTrack(rtpReceiver: RtpReceiver, mediaStreams: Array<MediaStream>) {}
    override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {}
    override fun onIceGatheringChange(iceGatheringState: PeerConnection.IceGatheringState) {}

    override fun onIceCandidate(iceCandidate: IceCandidate) {
        callbacks.onIceCandidate(iceCandidate.sdp, iceCandidate.sdpMid, iceCandidate.sdpMLineIndex)
    }

    override fun onAddStream(mediaStream: MediaStream) {
        callbacks.onAddedStream(mediaStream)
    }

    companion object {

        private val TAG = "WebRTC"
        private var factory: PeerConnectionFactory? = null
        internal var localStream: MediaStream? = null
        private var videoCapturer: VideoCapturer? = null
        private var eglBase: EglBase? = null

        private var localVideoTrack: VideoTrack? = null
        private val localRenderer: VideoRenderer? = null

        // interface -----------------

        internal fun setup(activity: Activity, eglBase: EglBase) {
            WebRTC.eglBase = eglBase

            // initialize Factory
            PeerConnectionFactory.initializeAndroidGlobals(activity.applicationContext, true)
            val options = PeerConnectionFactory.Options()
            factory = PeerConnectionFactory(options)
            factory!!.setVideoHwAccelerationOptions(eglBase.eglBaseContext, eglBase.eglBaseContext)

            var localStream = factory!!.createLocalMediaStream("android_local_stream")
            this.localStream = localStream

            // videoTrack
            videoCapturer = createCameraCapturer(Camera2Enumerator(activity))
            val localVideoSource = factory!!.createVideoSource(videoCapturer!!)
            localVideoTrack = factory!!.createVideoTrack("android_local_videotrack", localVideoSource)
            localStream.addTrack(localVideoTrack!!)

            // audioTrack
            val audioSource = factory!!.createAudioSource(WebRTCUtil.mediaStreamConstraints())
            val audioTrack = factory!!.createAudioTrack("android_local_audiotrack", audioSource)
            localStream.addTrack(audioTrack)

            val displayMetrics = DisplayMetrics()
            val windowManager = activity.application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager.defaultDisplay.getRealMetrics(displayMetrics)
            val videoWidth = displayMetrics.widthPixels
            val videoHeight = displayMetrics.heightPixels

            videoCapturer!!.startCapture(videoWidth, videoHeight, 30)
        }

        // implements -------------

        private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
            return createBackCameraCapturer(enumerator)
        }

        private fun createBackCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
            val deviceNames = enumerator.deviceNames

            for (deviceName in deviceNames) {
                if (!enumerator.isFrontFacing(deviceName)) {
                    val videoCapturer = enumerator.createCapturer(deviceName, null)

                    if (videoCapturer != null) {
                        return videoCapturer
                    }
                }
            }

            return null
        }
    }
}



