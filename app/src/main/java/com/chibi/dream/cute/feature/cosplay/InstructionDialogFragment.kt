package com.chibi.dream.cute.feature.cosplay

import android.os.Bundle
import android.view.View
import com.chibi.dream.cute.core.ui.base.BaseDialogFragment
import com.chibi.dream.cute.core.ui.setOnSingleClickListener
import com.chibi.dream.cute.databinding.DialogInstructionBinding

/** Luật chơi Cosplay (Figma 3:77): thẻ giấy rách + nút X bên dưới, nền dim. Bấm ra ngoài không đóng. */
class InstructionDialogFragment : BaseDialogFragment<DialogInstructionBinding>(DialogInstructionBinding::inflate) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(false)
        binding.btnClose.setOnSingleClickListener { dismiss() }
    }

    companion object {
        const val TAG = "InstructionDialog"
    }
}
