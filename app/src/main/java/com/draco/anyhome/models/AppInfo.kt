package com.draco.anyhome.models

data class AppInfo(
    var label: String,
    var id: String,
    var isCurrentHome: Boolean = false
)