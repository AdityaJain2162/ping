package com.aditya.ping.ui.navigation

object Routes {
    const val HOME = "home"
    const val ADD = "add"
    const val EDIT = "edit/{id}"
    const val SETTINGS = "settings"

    fun edit(id: Long) = "edit/$id"
}
