package com.example.myapplication.ui.image

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.FragmentImageBinding
import com.example.myapplication.utils.PermissionUtils
import com.example.myapplication.utils.PermissionUtils.allPermissionGranted
import com.example.myapplication.utils.PermissionUtils.launchMultiplePermission
import com.example.myapplication.utils.PermissionUtils.registerPermission
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ImageFragment : Fragment() {

    private var _binding: FragmentImageBinding? = null
    private val binding get() = _binding!!

    private val mViewModel: ImageViewModel by viewModels()

    private var imageCapture: ImageCapture? = null

    private lateinit var cameraExecutor: ExecutorService

    private val CAPTURE_IMAGE_REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val captureImagePermissionLauncher = registerPermission { onCaptureImageResult(it) }
    private fun onCaptureImageResult(state: PermissionUtils.PermissionState) {
        when (state) {
            PermissionUtils.PermissionState.Denied, PermissionUtils.PermissionState.PermanentlyDenied -> {}
            PermissionUtils.PermissionState.Granted -> {
                startCamera()
            }
        }
    }

    private val SHOW_IMAGE_REQUIRED_PERMISSIONS = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()
    private val showImagePermissionLauncher = registerPermission { onShowImageResult(it) }
    private fun onShowImageResult(state: PermissionUtils.PermissionState) {
        when (state) {
            PermissionUtils.PermissionState.Denied, PermissionUtils.PermissionState.PermanentlyDenied -> {}
            PermissionUtils.PermissionState.Granted -> {}
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageBinding.inflate(inflater, container, false)

        cameraExecutor = Executors.newSingleThreadExecutor()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (requireContext().allPermissionGranted(CAPTURE_IMAGE_REQUIRED_PERMISSIONS)) {
            startCamera()
        } else {
            captureImagePermissionLauncher.launchMultiplePermission(
                CAPTURE_IMAGE_REQUIRED_PERMISSIONS
            )
        }

        setOnClick()
    }

    private fun setOnClick() {
        binding.btnCaptureImage.setOnClickListener { takePhoto() }

        binding.btnShowImage.setOnClickListener {
            if (requireContext().allPermissionGranted(SHOW_IMAGE_REQUIRED_PERMISSIONS)) {
                loadAllImages()
            } else {
                showImagePermissionLauncher.launchMultiplePermission(SHOW_IMAGE_REQUIRED_PERMISSIONS)
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
            }

            imageCapture =
                ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setFlashMode(ImageCapture.FLASH_MODE_AUTO).build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val contentValues = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME, SimpleDateFormat(
                    "yyyy-MM-dd-HH-mm-ss-SSS", Locale.US
                ).format(System.currentTimeMillis())
            )
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            requireActivity().contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        imageCapture.takePicture(outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(e: ImageCaptureException) {}

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {}
            })
    }

    private fun loadAllImages() {
        val allImages = mutableListOf<String>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC"
        val cursor: Cursor? = requireContext().contentResolver.query(uri, projection, null, null, sortOrder)
        cursor?.let {
            while (cursor.moveToNext()) {
                allImages.add(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)))
            }
            cursor.close()
        }
        if (allImages.isNotEmpty()) {
            // Example load first image
            Glide.with(requireContext()).load(allImages[0]).into(binding.imageView)
        }
        Log.e("haidang", "allImages = $allImages")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        _binding = null
    }
}