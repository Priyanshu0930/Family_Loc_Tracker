package com.familytracker.app.model

data class User(
    val uid: String = "",
    val name: String = "",
    val gender: String = "",
    val relation: String = "",
    val age: Int = 0,
    val fcmToken: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationSharingEnabled: Boolean = false,
    val groupId: String = ""
)
