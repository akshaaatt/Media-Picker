package com.aemerse.dazzle.interfaces

import com.aemerse.dazzle.gallery.MediaModel

interface MediaClickInterface {
    fun onMediaClick(media: MediaModel)
    fun onMediaLongClick(media: MediaModel, intentFrom: String)
}