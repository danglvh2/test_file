package com.example.myapplication.ui.file

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.myapplication.databinding.FragmentFileBinding
import com.example.myapplication.utils.PermissionUtils
import com.example.myapplication.utils.PermissionUtils.launchMultiplePermission
import com.example.myapplication.utils.PermissionUtils.registerPermission


class FileFragment : Fragment() {

    private var _binding: FragmentFileBinding? = null
    private val binding get() = _binding!!

    private val mViewModel: FileViewModel by viewModels()

    private var uri: Uri? = null

    private val chooseFileRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                uri = result.data?.data
                uri?.let {
                    val inputStream = requireContext().contentResolver.openInputStream(it)
                    inputStream?.let {
                        val string = inputStream.bufferedReader().use { it.readText() }
                        binding.tvContent.text = string
                    }
                }
            }
        }

    private val createFilePermissionLauncher = registerPermission { onCreateFileResult(it) }
    private fun onCreateFileResult(state: PermissionUtils.PermissionState) {
        when (state) {
            PermissionUtils.PermissionState.Denied, PermissionUtils.PermissionState.PermanentlyDenied -> {}
            PermissionUtils.PermissionState.Granted -> editFile()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setOnClick()
    }

    private fun setOnClick() {
        binding.btnRead.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            chooseFileRequest.launch(intent)
        }

        binding.btnSave.setOnClickListener {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                createFilePermissionLauncher.launchMultiplePermission(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            } else {
                editFile()
            }

        }
    }

    private fun editFile() {
        val data = binding.edtContent.text.toString()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}