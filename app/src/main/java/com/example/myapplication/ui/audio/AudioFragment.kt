package com.example.myapplication.ui.audio

import android.Manifest
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.databinding.FragmentAudioBinding
import com.example.myapplication.utils.PermissionUtils
import com.example.myapplication.utils.PermissionUtils.allPermissionGranted
import com.example.myapplication.utils.PermissionUtils.launchMultiplePermission
import com.example.myapplication.utils.PermissionUtils.registerPermission
import java.io.File

class AudioFragment : Fragment() {

    private var _binding: FragmentAudioBinding? = null
    private val binding get() = _binding!!

    private val GET_AUDIO_REQUIRED_PERMISSIONS = mutableListOf<String>().apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }.toTypedArray()
    private val getAudioPermissionLauncher = registerPermission { onGetAudioResult(it) }
    private fun onGetAudioResult(state: PermissionUtils.PermissionState) {
        when (state) {
            PermissionUtils.PermissionState.Denied, PermissionUtils.PermissionState.PermanentlyDenied -> {}
            PermissionUtils.PermissionState.Granted -> {}
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAudioBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setOnClick()
    }

    private fun setOnClick() {
        binding.btnGetAudio.setOnClickListener {
            if (requireContext().allPermissionGranted(GET_AUDIO_REQUIRED_PERMISSIONS)) {
                getAllAudio()
            } else {
                getAudioPermissionLauncher.launchMultiplePermission(GET_AUDIO_REQUIRED_PERMISSIONS)
            }
        }
    }

    private fun getAllAudio() {
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.AudioColumns.TITLE,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.AudioColumns.ALBUM_ID,
            MediaStore.Audio.Media.DATA
        )
        val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
        val cursor: Cursor? =
            requireContext().contentResolver.query(uri, projection, selection, null, null)
        val songsListFromPhone = mutableListOf<String>()
        cursor?.use {
            while (cursor.moveToNext()) {
                val songPath = it.getString(it.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                val songUri = Uri.fromFile(File(songPath)).toString()
                songsListFromPhone.add(songUri)
            }
        }
        Log.e("haidang", "audio = $songsListFromPhone")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}