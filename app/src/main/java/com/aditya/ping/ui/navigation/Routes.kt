package com.aditya.ping.ui.navigation

object Routes {
    const val HOME = "home"
    const val ADD = "add"
    const val EDIT = "edit/{id}"
    const val SETTINGS = "settings"
    const val SAVED_PLACES = "saved_places"
    const val LISTS = "lists"

    fun edit(id: Long) = "edit/$id"
}
