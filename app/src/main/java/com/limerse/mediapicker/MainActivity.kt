package com.limerse.mediapicker

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.limerse.dazzle.Dazzle
import com.limerse.dazzle.Dazzle.Companion.PICKED_MEDIA_LIST
import com.limerse.dazzle.Dazzle.Companion.REQUEST_CODE_PICKER
import com.limerse.dazzle.utils.DazzleOptions
import com.limerse.mediapicker.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import com.limerse.dazzle.DazzleGallery

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.github -> {
                    startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/akshaaatt")))
                }
            }
            false
        }

        val dazzleOptions = DazzleOptions.init().apply {
                maxCount = 5                        //maximum number of images/videos to be picked
                maxVideoDuration = 10               //maximum duration for video capture in seconds
                allowFrontCamera = true             //allow front camera use
                excludeVideos = false               //exclude or include video functionalities
                cropEnabled = true
        }

        binding.selectMedia.setOnClickListener {
            Dazzle.startPicker(this, dazzleOptions)    //this -> context of Activity or Fragment
        }
        binding.selectGallery.setOnClickListener {
            DazzleGallery.startPicker(this, dazzleOptions)         }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PICKER){

            val mImageList = data?.getStringArrayListExtra(PICKED_MEDIA_LIST) as ArrayList //List of selected/captured images/videos
            mImageList.map {
                val inflate = LayoutInflater.from(this).inflate(R.layout.scrollitem_image, binding.imageContainer,false)
                val iv = inflate as ImageView


                Glide.with(this).load(it).into(iv)
                binding.imageContainer.addView(iv)
            }
        }
    }
}