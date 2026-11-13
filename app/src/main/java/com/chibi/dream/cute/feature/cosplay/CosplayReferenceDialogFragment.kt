package com.chibi.dream.cute.feature.cosplay

import android.os.Bundle
import android.view.View
import com.chibi.dream.cute.R
import com.chibi.dream.cute.core.ui.base.BaseDialogFragment
import com.chibi.dream.cute.core.ui.loadImage
import com.chibi.dream.cute.core.ui.setOnSingleClickListener
import com.chibi.dream.cute.databinding.DialogCosplayScoreBinding

/**
 * Popup ảnh mẫu khi bấm thumbnail trong màn chơi (Figma 3:123): ảnh mẫu + "{percent}%" hoàn thành.
 * Chỉ để xem, đóng thì chơi tiếp, đồng hồ vẫn chạy.
 */
class CosplayReferenceDialogFragment : BaseDialogFragment<DialogCosplayScoreBinding>(DialogCosplayScoreBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        binding.ivResult.contentDescription = getString(R.string.cd_reference_image)
        binding.ivResult.loadImage(args.getString(ARG_IMAGE_URL), R.color.white)
        binding.tvPercent.text = getString(R.string.score_percent, args.getInt(ARG_PERCENT))
        binding.btnClose.setOnSingleClickListener { dismiss() }
    }


    companion object {
        const val TAG = "CosplayReferenceDialog"

        private const val ARG_PERCENT = "percent"
        private const val ARG_IMAGE_URL = "imageUrl"

        fun newInstance(percent: Int, imageUrl: String) = CosplayReferenceDialogFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_PERCENT, percent)
                putString(ARG_IMAGE_URL, imageUrl)
            }
        }
    }
}
