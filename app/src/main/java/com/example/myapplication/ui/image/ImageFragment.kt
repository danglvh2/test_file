package com.example.myapplication.ui.image

import android.Manifest
import android.content.ContentValues
import android.content.Context.MODE_PRIVATE
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import com.example.myapplication.databinding.FragmentImageBinding
import com.example.myapplication.ui.image.model.InternalStoragePhoto
import com.example.myapplication.utils.PermissionUtils
import com.example.myapplication.utils.PermissionUtils.allPermissionGranted
import com.example.myapplication.utils.PermissionUtils.launchMultiplePermission
import com.example.myapplication.utils.PermissionUtils.registerPermission
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID

class ImageFragment : Fragment() {

    private var _binding: FragmentImageBinding? = null
    private val binding get() = _binding!!

    private val CAPTURE_IMAGE_REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    private val captureImagePermissionLauncher = registerPermission { onCaptureImageResult(it) }
    private fun onCaptureImageResult(state: PermissionUtils.PermissionState) {
        when (state) {
            PermissionUtils.PermissionState.Denied, PermissionUtils.PermissionState.PermanentlyDenied -> {}
            PermissionUtils.PermissionState.Granted -> {}
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
            PermissionUtils.PermissionState.Granted -> loadImageFromExternalStorage()
        }
    }

    private val captureImageInternalStorageLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let { savePhotoToInternalStorage(UUID.randomUUID().toString(), it) }
        }

    private val captureImageExternalStorageLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            bitmap?.let { savePhotoToExternalStorage(it) }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Capture Image Internal Storage
        binding.btnCaptureImageInternalStorage.setOnClickListener {
            if (requireContext().allPermissionGranted(CAPTURE_IMAGE_REQUIRED_PERMISSIONS)) {
                captureImageInternalStorageLauncher.launch()
            } else {
                captureImagePermissionLauncher.launchMultiplePermission(
                    CAPTURE_IMAGE_REQUIRED_PERMISSIONS
                )
            }
        }

        // Capture Image External Storage
        binding.btnCaptureImageExternalStorage.setOnClickListener {
            if (requireContext().allPermissionGranted(CAPTURE_IMAGE_REQUIRED_PERMISSIONS)) {
                captureImageExternalStorageLauncher.launch()
            } else {
                captureImagePermissionLauncher.launchMultiplePermission(
                    CAPTURE_IMAGE_REQUIRED_PERMISSIONS
                )
            }
        }

        // Show Image Internal Storage
        binding.btnShowImageInternalStorage.setOnClickListener { loadImageFromInternalStorage() }

        // Show Image External Storage
        binding.btnShowImageExternalStorage.setOnClickListener {
            if (requireContext().allPermissionGranted(SHOW_IMAGE_REQUIRED_PERMISSIONS)) {
                loadImageFromExternalStorage()
            } else {
                showImagePermissionLauncher.launchMultiplePermission(SHOW_IMAGE_REQUIRED_PERMISSIONS)
            }
        }
    }

    private fun savePhotoToInternalStorage(fileName: String, bitmap: Bitmap): Boolean {
        return try {
            requireContext().openFileOutput(
                "$fileName.jpg", MODE_PRIVATE
            ).use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
                    throw IOException("Couldn't save bitmap")
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun savePhotoToExternalStorage(bitmap: Bitmap): Boolean {
        val contentValues = ContentValues().apply {
            put(
                MediaStore.MediaColumns.DISPLAY_NAME,
                SimpleDateFormat(
                    "yyyy-MM-dd-HH-mm-ss-SSS",
                    Locale.US
                ).format(System.currentTimeMillis())
            )
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.WIDTH, bitmap.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures")
            }
        }
        return try {
            requireContext().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )?.also { uri ->
                requireContext().contentResolver.openOutputStream(uri)?.let { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)) {
                        throw IOException("Couldn't save bitmap")
                    }
                }
            } ?: throw IOException("Couldn't create MediaStore entry")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun loadImageFromInternalStorage() {
        val files = requireContext().filesDir.listFiles()
        val result = files?.filter { it.canRead() && it.isFile && it.name.endsWith(".jpg") }?.map {
            val bytes = it.readBytes()
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            InternalStoragePhoto(it.name, bmp)
        } ?: listOf()
        Log.e("haidang", "Image From Internal Storage = $result")
    }

    private fun loadImageFromExternalStorage() {
        val result = mutableListOf<String>()
        val uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection =
            arrayOf(MediaStore.MediaColumns.DATA, MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC"
        val cursor: Cursor? =
            requireContext().contentResolver.query(uri, projection, null, null, sortOrder)
        cursor?.let {
            while (cursor.moveToNext()) {
                result.add(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)))
            }
            cursor.close()
        }
        Log.e("haidang", "Image From External Storage = $result")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}