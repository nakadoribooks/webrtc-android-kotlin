package com.nakadoribooks.webrtcexamplekotlin

import android.app.Activity
import android.util.Log

import com.fasterxml.jackson.databind.ObjectMapper

import org.json.JSONObject

import java.util.concurrent.TimeUnit

import rx.android.app.AppObservable
import rx.functions.Action1
import ws.wamp.jawampa.PubSubData
import ws.wamp.jawampa.WampClient
import ws.wamp.jawampa.WampClientBuilder


/**
 * Created by kawase on 2017/04/15.
 */

class Wamp internal constructor(private val activity: Activity, private val roomId: String, private val userId: String, private val callbacks: WampCallbacks) : WampInterface {

    private fun roomTopic(base: String): String {
        return base.replace("[roomId]", roomId)
    }

    internal fun answerTopic(userId: String): String {
        return roomTopic(Topic.Answer.string.replace("[userId]", userId))
    }

    internal fun offerTopic(userId: String): String {
        return roomTopic(Topic.Offer.string.replace("[userId]", userId))
    }

    internal fun candidateTopic(userId: String): String {
        return roomTopic(Topic.Candidate.string.replace("[userId]", userId))
    }

    internal fun callmeTopic(): String {
        return roomTopic(Topic.Callme.string)
    }

    internal fun closeTopic(userId: String): String {
        return roomTopic(Topic.Callme.string)
    }

    private lateinit var client: WampClient

    // interface -------

    override fun connect() {
        val builder = WampClientBuilder()

        try {
            builder
                    .withUri(HandshakeEndpint)
                    .withRealm("realm1")
                    .withInfiniteReconnects()
                    .withReconnectInterval(3, TimeUnit.SECONDS)
            client = builder.build()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        AppObservable.bindActivity(activity, client.statusChanged())
                .subscribe({ status ->
                    if (status === WampClient.Status.Connected) {
                        onConnectWamp()
                    }
                }, { }, { })

        client.open()
    }

    override fun publishCallme() {
        val callmeTopic = callmeTopic()
        val mapper = ObjectMapper()
        val args = mapper.createObjectNode()
        args.put("targetId", userId)
        client.publish(callmeTopic, userId)
    }

    override fun publishOffer(targetId: String, sdp: String) {
        val topic = offerTopic(targetId)
        client.publish(topic, this.userId, sdp)
    }

    override fun publishAnswer(targetId: String, sdp: String) {
        val topic = answerTopic(targetId)
        client.publish(topic, this.userId, sdp)
    }

    override fun publishCandidate(targetId: String, candidate: String) {
        val topic = candidateTopic(targetId)
        client.publish(topic, this.userId, candidate)
    }

    // implements --------

    private fun onConnectWamp() {

        // ▼ subscribe -----

        // offer
        val offerTopic = offerTopic(userId)
        client.makeSubscription(offerTopic).subscribe({ arg0 ->
            /*       ここから ------------ */
            val args = arg0.arguments()
            val targetId = args.get(0).asText()

            val node = args.get(1)

            val sdpString = node.asText()
            try {
                val obj = JSONObject(sdpString)
                val s = obj.getString("sdp")
                callbacks.onReceiveOffer(targetId, s)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, { arg0 -> arg0.printStackTrace() })

        // answer
        val answerTopic = answerTopic(userId)
        client.makeSubscription(answerTopic).subscribe({ arg0 ->
            val args = arg0.arguments()
            val targetId = args.get(0).asText()

            val node = args.get(1)
            val sdpString = node.asText()

            try {
                val obj = JSONObject(sdpString)
                val s = obj.getString("sdp")
                callbacks.onReceiveAnswer(targetId, s)
            } catch (e: Exception) {
                e.printStackTrace()
                Log.d("receiveAnswer 5", "")
            }
        }, { arg0 -> arg0.printStackTrace() })

        // candidate
        val candidateTopic = candidateTopic(userId)
        client.makeSubscription(candidateTopic).subscribe(Action1<PubSubData> { arg0 ->
            val targetId = arg0.arguments().get(0).asText()
            val jsonString = arg0.arguments().get(1).asText()
            try {
                val json = JSONObject(jsonString)
                var sdp: String? = null
                if (!json.has("candidate")) {
                    return@Action1
                }
                sdp = json.getString("candidate")
                val sdpMid = json.getString("sdpMid")
                val sdpMLineIndex = json.getInt("sdpMLineIndex")

                callbacks.onIceCandidate(targetId, sdp, sdpMid, sdpMLineIndex)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, Action1<Throwable> { arg0 -> arg0.printStackTrace() })

        // callme
        val callmeTopic = callmeTopic()
        client.makeSubscription(callmeTopic).subscribe(Action1<PubSubData> { arg0 ->
            val args = arg0.arguments()
            val targetId = args.get(0).asText()

            if (targetId == userId) {
                Log.d("onCallme", "cancel")
                return@Action1
            }

            callbacks.onReceiveCallme(targetId)
        }, Action1<Throwable> { arg0 -> arg0.printStackTrace() })

        // close
        val closeTopic = closeTopic(userId)
        client.makeSubscription(closeTopic).subscribe({ arg0 ->
            val args = arg0.arguments()
            val targetId = args.get(0).asText()

            callbacks.onCloseConnection(targetId)
        }, { arg0 -> arg0.printStackTrace() })

        callbacks.onOpen()
    }

    companion object {

        private val HandshakeEndpint = "wss://nakadoribooks-webrtc.herokuapp.com"
    }


}
