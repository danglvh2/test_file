package com.example.myapplication.ui.image.model

import android.net.Uri

data class SharedStoragePhoto(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val contentUri: Uri
)
