package com.limerse.dazzle

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE
import android.util.Log
import android.view.View
import android.view.View.*
import android.webkit.MimeTypeMap
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.limerse.dazzle.databinding.ActivityDazzleGalleryBinding
import com.limerse.dazzle.gallery.BottomSheetMediaRecyclerAdapter
import com.limerse.dazzle.gallery.BottomSheetMediaRecyclerAdapter.Companion.HEADER
import com.limerse.dazzle.gallery.BottomSheetMediaRecyclerAdapter.Companion.SPAN_COUNT
import com.limerse.dazzle.gallery.InstantMediaRecyclerAdapter
import com.limerse.dazzle.gallery.MediaModel
import com.limerse.dazzle.interfaces.MediaClickInterface
import com.limerse.dazzle.interfaces.PermissionCallback
import com.limerse.dazzle.utils.DazzleOptions
import com.limerse.dazzle.utils.GeneralUtils.getStringDate
import com.limerse.dazzle.utils.GeneralUtils.manipulateBottomSheetVisibility
import com.limerse.dazzle.utils.HeaderItemDecoration
import com.limerse.dazzle.utils.MediaConstants.IMAGE_VIDEO_URI
import com.limerse.dazzle.utils.MediaConstants.getFileFromUri
import com.limerse.dazzle.utils.MediaConstants.getImageVideoCursor
import com.limerse.dazzle.utils.PermissionUtils
import com.theartofdev.edmodo.cropper.CropImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.collections.ArrayList
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class DazzleGallery : AppCompatActivity() {

    private lateinit var mBinding: ActivityDazzleGalleryBinding
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var mDazzleOptions: DazzleOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityDazzleGalleryBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mDazzleOptions = intent?.getSerializableExtra(PICKER_OPTIONS) as DazzleOptions


        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        window.decorView.setOnApplyWindowInsetsListener { _, insets ->
            insets
        }
        Handler(Looper.getMainLooper()).postDelayed({ getMedia() }, 500)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == RESULT_OK) {
                val savedUri = result.uri
                val mimeType = MimeTypeMap.getSingleton()
                    .getMimeTypeFromExtension(savedUri.toFile().extension)
                MediaScannerConnection.scanFile(
                    this@DazzleGallery,
                    arrayOf(savedUri.toFile().absolutePath),
                    arrayOf(mimeType)
                ) { _, uri ->
                    Log.d(TAG, "Image capture scanned into media store: $uri")
                }
                val mPathList = ArrayList<String>()
                mPathList.add(savedUri.toString())

                val intent = Intent()
                intent.putExtra(PICKED_MEDIA_LIST, mPathList)
                setResult(Activity.RESULT_OK, intent)
                finish()

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                val error = result.error
                setResult(Activity.RESULT_CANCELED, intent)
                finish()
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()

    }

    private val galleryImageList = ArrayList<MediaModel>()
    private var mBottomMediaAdapter: BottomSheetMediaRecyclerAdapter? = null

    private fun getMedia() {

        CoroutineScope(Dispatchers.Main).launch {
            val cursor: Cursor? = withContext(Dispatchers.IO) {
                getImageVideoCursor(this@DazzleGallery, mDazzleOptions.excludeVideos)
            }

            if (cursor != null) {

                Log.e(TAG, "getMedia: ${cursor.count}")

                val index = cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)
                val dateIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED)
                val typeIndex = cursor.getColumnIndex(MediaStore.Files.FileColumns.MEDIA_TYPE)

                var headerDate = ""

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(index)
                    val path = ContentUris.withAppendedId(IMAGE_VIDEO_URI, id)
                    val mediaType = cursor.getInt(typeIndex)
                    val longDate = cursor.getLong(dateIndex)
                    val mediaDate = getStringDate(this@DazzleGallery, longDate)

                    if (!headerDate.equals(mediaDate, true)) {
                        headerDate = mediaDate
                        galleryImageList.add(MediaModel(null, mediaType, headerDate))
                    }

                    galleryImageList.add(MediaModel(path, mediaType, ""))
                }

                handleBottomSheet()
            }

        }
    }

    private val mMediaClickListener = object : MediaClickInterface {
        override fun onMediaClick(media: MediaModel) {
            pickImages()
        }

        override fun onMediaLongClick(media: MediaModel, intentFrom: String) {

            if (intentFrom == BottomSheetMediaRecyclerAdapter::class.java.simpleName) {
                if (mBottomMediaAdapter?.imageCount!! > 0) {
                    mBinding.textViewImageCount.text = mBottomMediaAdapter?.imageCount?.toString()
                    mBinding.textViewTopSelect.text = String.format(
                        getString(R.string.images_selected),
                        mBottomMediaAdapter?.imageCount?.toString()
                    )
                    showTopViews()
                } else hideTopViews()

            }
        }

    }

    private fun showTopViews() {
        mBinding.constraintCheck.visibility = VISIBLE
        mBinding.textViewOk.visibility = VISIBLE

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBinding.constraintBottomSheetTop.setBackgroundColor(
                resources.getColor(
                    R.color.colorPrimary,
                    null
                )
            )
            DrawableCompat.setTint(
                mBinding.imageViewBack.drawable,
                resources.getColor(R.color.colorWhite, null)
            )
        } else {
            mBinding.constraintBottomSheetTop.setBackgroundColor(
                ContextCompat.getColor(
                    applicationContext,
                    R.color.colorPrimary
                )
            )
            DrawableCompat.setTint(
                mBinding.imageViewBack.drawable,
                ContextCompat.getColor(applicationContext, R.color.colorWhite)
            )
        }
    }

    private fun hideTopViews() {
        mBinding.constraintCheck.visibility = GONE
        mBinding.textViewOk.visibility = GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBinding.constraintBottomSheetTop.setBackgroundColor(
                resources.getColor(R.color.colorWhite, null)
            )
            DrawableCompat.setTint(
                mBinding.imageViewBack.drawable,
                resources.getColor(R.color.colorBlack, null)
            )
        } else {
            mBinding.constraintBottomSheetTop.setBackgroundColor(
                ContextCompat.getColor(
                    applicationContext,
                    R.color.colorWhite
                )
            )
            DrawableCompat.setTint(
                mBinding.imageViewBack.drawable,
                ContextCompat.getColor(applicationContext, R.color.colorBlack)
            )
        }
    }


    private fun handleBottomSheet() {
        mBottomMediaAdapter =
            BottomSheetMediaRecyclerAdapter(
                galleryImageList,
                mMediaClickListener,
                this@DazzleGallery
            )
        mBottomMediaAdapter?.maxCount = mDazzleOptions.maxCount

        val layoutManager = GridLayoutManager(this, SPAN_COUNT)
        mBinding.recyclerViewBottomSheetMedia.layoutManager = layoutManager

        layoutManager.spanSizeLookup = object : SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (mBottomMediaAdapter?.getItemViewType(position) == HEADER) {
                    SPAN_COUNT
                } else 1
            }
        }

        mBinding.recyclerViewBottomSheetMedia.adapter = mBottomMediaAdapter
        mBinding.recyclerViewBottomSheetMedia.addItemDecoration(
            HeaderItemDecoration(
                mBottomMediaAdapter!!,
                this
            )
        )
        
        var count = 0
        galleryImageList.map { mediaModel ->
            if (mediaModel.isSelected) count++
        }
        mBottomMediaAdapter?.imageCount = count
        mBottomMediaAdapter?.notifyDataSetChanged()


        mBinding.constraintCheck.setOnClickListener { pickImages() }
        mBinding.textViewOk.setOnClickListener { pickImages() }
        mBinding.imageViewBack.setOnClickListener {
            onBackPressed()
        }

    }

    private fun pickImages() {
        val mPathList = ArrayList<String>()
        val mPath = ArrayList<Uri>()
        galleryImageList.map { mediaModel ->
            if (mediaModel.isSelected) {
                mPathList.add(
                    getFileFromUri(
                        contentResolver,
                        mediaModel.mMediaUri!!,
                        cacheDir
                    ).path
                )
                if (mediaModel.mMediaType == MEDIA_TYPE_IMAGE) {
                    mPath.add(mediaModel.mMediaUri!!)
                }
            }
        }

        if (mDazzleOptions.cropEnabled && mPath.size == 1) {
            CropImage.activity(mPath[0])
                .setInitialCropWindowPaddingRatio(0f).start(this@DazzleGallery);
        } else {
            val intent = Intent()
            intent.putExtra(PICKED_MEDIA_LIST, mPathList)
            setResult(RESULT_OK, intent)
            finish()
        }
    }

    override fun onBackPressed() {
        when {
            mBottomMediaAdapter?.imageCount != null && mBottomMediaAdapter?.imageCount!! > 0 -> removeSelection()
            else -> super.onBackPressed()
        }
    }

    private fun removeSelection() {
        mBottomMediaAdapter?.imageCount = 0
        for (i in 0 until galleryImageList.size) galleryImageList[i].isSelected = false
        mBottomMediaAdapter?.notifyDataSetChanged()
        mBinding.constraintCheck.visibility = GONE
        mBinding.textViewOk.visibility = GONE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mBinding.constraintBottomSheetTop.setBackgroundColor(
                resources.getColor(
                    R.color.colorWhite,
                    null
                )
            )
            DrawableCompat.setTint(
                mBinding.imageViewBack.drawable,
                resources.getColor(R.color.colorBlack, null)
            )
        } else {
            mBinding.constraintBottomSheetTop.setBackgroundColor(
                ContextCompat.getColor(
                    applicationContext,
                    R.color.colorWhite
                )
            )
            DrawableCompat.setTint(
                mBinding.imageViewBack.drawable,
                ContextCompat.getColor(applicationContext, R.color.colorBlack)
            )
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }
    
    companion object {
        private const val TAG = "Picker"
       const val REQUEST_CODE_PICKER = 10
        const val PICKER_OPTIONS = "PICKER_OPTIONS"
        const val PICKED_MEDIA_LIST = "PICKED_MEDIA_LIST"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        @JvmStatic
        fun startPicker(fragment: Fragment, mDazzleOptions: DazzleOptions) {
            PermissionUtils.checkForCameraWritePermissions(fragment, object : PermissionCallback {
                override fun onPermission(approved: Boolean) {
                    val mPickerIntent = Intent(fragment.activity, DazzleGallery::class.java)
                    mPickerIntent.putExtra(PICKER_OPTIONS, mDazzleOptions)
                    mPickerIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    fragment.startActivityForResult(mPickerIntent, REQUEST_CODE_PICKER)
                }
            })
        }

        @JvmStatic
        fun startPicker(activity: FragmentActivity, mDazzleOptions: DazzleOptions) {
            PermissionUtils.checkForCameraWritePermissions(activity, object : PermissionCallback {
                override fun onPermission(approved: Boolean) {
                    val mPickerIntent = Intent(activity, DazzleGallery::class.java)
                    mPickerIntent.putExtra(PICKER_OPTIONS, mDazzleOptions)
                    mPickerIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    activity.startActivityForResult(mPickerIntent, REQUEST_CODE_PICKER)
                }
            })
        }
    }
}