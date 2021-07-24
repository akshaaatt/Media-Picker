package com.limerse.dazzle.interfaces

import com.limerse.dazzle.gallery.MediaModel

interface MediaClickInterface {
    fun onMediaClick(media: MediaModel)
    fun onMediaLongClick(media: MediaModel, intentFrom: String)
}