package com.alphelios.dazzle.interfaces

import com.alphelios.dazzle.gallery.MediaModel

interface MediaClickInterface {
    fun onMediaClick(media: MediaModel)
    fun onMediaLongClick(media: MediaModel, intentFrom: String)
}