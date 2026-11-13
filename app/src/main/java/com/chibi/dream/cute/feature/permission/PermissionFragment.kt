package com.chibi.dream.cute.feature.permission

import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.chibi.dream.cute.R
import com.chibi.dream.cute.core.common.AppPermissions
import com.chibi.dream.cute.core.ui.applyStatusBarPadding
import com.chibi.dream.cute.core.ui.base.BaseFragment
import com.chibi.dream.cute.core.ui.collectWhenStarted
import com.chibi.dream.cute.core.ui.openAppSettings
import com.chibi.dream.cute.core.ui.setLightStatusBar
import com.chibi.dream.cute.core.ui.setOnSingleClickListener
import com.chibi.dream.cute.databinding.FragmentPermissionBinding
import com.chibi.dream.cute.databinding.ItemPermissionRowBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PermissionFragment : BaseFragment<FragmentPermissionBinding>(FragmentPermissionBinding::inflate) {

    private val viewModel: PermissionViewModel by viewModels()

    private val storageLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        onPermissionResult(AppPermissions.storage.orEmpty().asIterable(), AppPermissions.isStorageGranted(requireContext()))
    }

    private val notificationLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onPermissionResult(listOfNotNull(AppPermissions.notification), granted)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyStatusBarPadding()
        setLightStatusBar(true)

        binding.rowMedia.tvName.setText(R.string.permission_storage)
        binding.rowNotification.tvName.setText(R.string.permission_notification)
        // Android 12 trở xuống: chỉ dòng Storage. Android 13 trở lên: chỉ dòng Notification
        binding.rowMedia.root.isVisible = AppPermissions.storage != null
        binding.rowNotification.root.isVisible = AppPermissions.notification != null

        binding.rowMedia.root.setOnSingleClickListener {
            val permissions = AppPermissions.storage
            if (permissions != null && !AppPermissions.isStorageGranted(requireContext())) {
                storageLauncher.launch(permissions)
            }
        }
        binding.rowNotification.root.setOnSingleClickListener {
            val permission = AppPermissions.notification
            if (permission != null && !AppPermissions.isNotificationGranted(requireContext())) {
                notificationLauncher.launch(permission)
            }
        }
        binding.tvContinue.setOnSingleClickListener { viewModel.completeOnboarding() }

        collectWhenStarted(viewModel.onboardingCompleted) {
            findNavController().navigate(R.id.action_permission_to_home)
        }
    }

    override fun onResume() {
        super.onResume()
        // Người dùng có thể vừa cấp quyền trong Settings
        renderStates()
    }

    private fun renderStates() {
        val context = requireContext()
        binding.rowMedia.setGranted(AppPermissions.isStorageGranted(context))
        binding.rowNotification.setGranted(AppPermissions.isNotificationGranted(context))
    }

    private fun ItemPermissionRowBinding.setGranted(granted: Boolean) {
        tvName.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0, 0, if (granted) R.drawable.ic_radio_on else R.drawable.ic_radio_off, 0,
        )
    }

    /** Bị từ chối vĩnh viễn (không còn hiện hộp thoại hệ thống) -> mở trang cài đặt của app. */
    private fun onPermissionResult(permissions: Iterable<String>, granted: Boolean) {
        renderStates()
        val canAskAgain = permissions.any { shouldShowRequestPermissionRationale(it) }
        if (!granted && !canAskAgain) requireContext().openAppSettings()
    }
}
