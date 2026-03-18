package com.muuu.unshort.ui.dialog

import android.app.Activity
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.muuu.unshort.R
import com.muuu.unshort.changelog.ChangelogEntry

/**
 * 앱 업데이트 후 첫 실행 시 변경 내용을 보여주는 BottomSheet
 */
object ChangelogBottomSheet {

    fun show(activity: Activity, changelog: ChangelogEntry, onDismiss: () -> Unit) {
        val bottomSheetDialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(R.layout.bottom_sheet_changelog, null)

        val title = view.findViewById<TextView>(R.id.changelogTitle)
        title.text = activity.getString(R.string.changelog_title, changelog.versionName)

        val container = view.findViewById<LinearLayout>(R.id.changelogItemsContainer)
        val inflater = LayoutInflater.from(activity)
        changelog.changes.forEach { stringResId ->
            val itemView = inflater.inflate(R.layout.item_changelog, container, false) as TextView
            itemView.text = "•  ${activity.getString(stringResId)}"
            container.addView(itemView)
        }

        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.changelogConfirmButton)
            .setOnClickListener {
                bottomSheetDialog.dismiss()
            }

        bottomSheetDialog.setOnDismissListener {
            onDismiss()
        }

        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.show()
    }
}
