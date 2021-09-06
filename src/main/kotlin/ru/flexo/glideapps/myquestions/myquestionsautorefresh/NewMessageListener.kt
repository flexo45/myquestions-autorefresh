package ru.flexo.glideapps.myquestions.myquestionsautorefresh

import java.util.*

interface NewMessageListener {
    fun onNewMessage(messages: List<GlideSignInMassage>)
}

data class GlideSignInMassage(val receivingDate: Date, val link: String)