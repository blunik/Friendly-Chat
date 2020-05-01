package com.example.friendlychat

class FriendlyMessage {

    private var text:String? = null
    private var name: String? = null
    private var photoUrl: String? = null
    constructor() {}

    constructor(text: String?, name: String?, photoUrl: String?) {
        this.text = text
        this.name = name
        this.photoUrl = photoUrl
    }

    fun getText(): String? {
        return text
    }

    fun getName(): String? {
        return name
    }
   fun getPhotoUrl(): String? {
       return photoUrl
   }

}