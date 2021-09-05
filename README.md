<h1 align="center">Media-Picker</h1>

<p align="center">
    <a href="https://github.com/akshaaatt/Media-Picker/commits/master">
    <img src="https://img.shields.io/github/last-commit/akshaaatt/Media-Picker.svg?style=flat-square&logo=github&logoColor=white"
         alt="GitHub last commit">
    <a href="https://github.com/akshaaatt/Media-Picker/issues">
    <img src="https://img.shields.io/github/issues-raw/akshaaatt/Media-Picker.svg?style=flat-square&logo=github&logoColor=white"
         alt="GitHub issues">
    <a href="https://github.com/akshaaatt/Media-Picker/pulls">
    <img src="https://img.shields.io/github/issues-pr-raw/akshaaatt/Media-Picker.svg?style=flat-square&logo=github&logoColor=white"
         alt="GitHub pull requests">
    <img src="https://PlayBadges.pavi2410.me/badge/downloads?id=com.limerse.social">
    <img src="https://PlayBadges.pavi2410.me/badge/ratings?id=com.limerse.social">
</p>
      
<p align="center">
  <a href="#features">Features</a> •
  <a href="#development">Development</a> •
  <a href="#usage">Usage</a> •
  <a href="#license">License</a> •
  <a href="#contribution">Contribution</a>
</p>

---

![vid](https://github.com/akshaaatt/Media-Picker/blob/master/promo/vid.gif)
[![1611683708571.jpg](https://i.postimg.cc/CLcGscL2/1611683708571.jpg)](https://postimg.cc/4mhHJbcv)
[![1611683708587.jpg](https://i.postimg.cc/sDjPWkyS/1611683708587.jpg)](https://postimg.cc/gr76FBxk)

# Media-Picker

Android Library for easing Media Selection to your apps with support for Images and Videos with a beautiful sample app.

## Features

* Written in Kotlin
* Pick Multiple Images and Videos
* Restrict User to Pick no of Images and Videos
* Capture Images and Videos
* Latest CameraX API

## Gradle Dependency

* Add the JitPack repository to your project's build.gradle file

```
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

* Add the dependency in your app's build.gradle file

```
dependencies {
    implementation 'com.github.akshaaatt:Media-Picker:1.0.2'
}
```

## Usage

#### Initialization

```kotlin
 val dazzleOptions =
            DazzleOptions.init().apply {
                maxCount = 5                        //maximum number of images/videos to be picked
                maxVideoDuration = 10               //maximum duration for video capture in seconds
                allowFrontCamera = true             //allow front camera use
                excludeVideos = false               //exclude or include video functionalities
            }

        binding.selectMedia.setOnClickListener {
            Dazzle.startPicker(this, dazzleOptions)    //this -> context of Activity or Fragment
        }
```

#### Callback

```kotlin
  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_PICKER){
            val mImageList = data?.getStringArrayListExtra(PICKED_MEDIA_LIST) as ArrayList //List of selected/captured images/videos
            mImageList.map {
                Log.e(TAG, "onActivityResult: $it" )
            }
        }
    }
```

## Contribution

You are most welcome to contribute to this project.
Always looking forward for your support! 
