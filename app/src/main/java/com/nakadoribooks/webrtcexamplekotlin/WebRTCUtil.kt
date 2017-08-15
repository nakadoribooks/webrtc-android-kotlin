package com.nakadoribooks.webrtcexamplekotlin

import org.webrtc.MediaConstraints


/**
 * Created by kawase on 2017/04/15.
 */

object WebRTCUtil {

    internal fun peerConnectionConstraints(): MediaConstraints {
        return audioVideoConstraints()
    }

    internal fun offerConnectionConstraints(): MediaConstraints {
        return audioVideoConstraints()
    }

    internal fun answerConnectionConstraints(): MediaConstraints {
        return audioVideoConstraints()
    }

    internal fun mediaStreamConstraints(): MediaConstraints {
        val constraints = MediaConstraints()

        return constraints
    }

    private fun audioVideoConstraints(): MediaConstraints {
        val constraints = MediaConstraints()
        constraints.mandatory.add(
                MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
        constraints.mandatory.add(
                MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))

        return constraints
    }

}
