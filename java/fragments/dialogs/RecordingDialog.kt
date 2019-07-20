package com.fragments.dialogs

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.R
import com.utils.Utils
import org.jetbrains.annotations.Nullable

/**
 * Simple dialog for the purpose of requesting the user to allow recording
 * Yes: Opens the application
 * No: Closes the dialog and the base activity
 */
class RecordingDialog : BaseDialog() {

  /* *********************
   * COMPANION OBJECT
   * ******************* */

  companion object {
    val TAG: String = RecordingDialog::class.java.simpleName
  }

  /* *********************
   * CLASS VARIABLES
   * ******************* */

  private lateinit var leftButton: MaterialButton
  private lateinit var rightButton: MaterialButton

  /**
   * Set Bottom Sheet theme
   */
  override fun getTheme(): Int = R.style.Dialog_Day

  /**
   * View Created
   *
   * @param layoutInflater view inflater
   * @param viewGroup      Dialog view container
   * @param args           a mapping from String keys to various values
   * @return the created view
   */
  @Nullable
  override fun onCreateView(layoutInflater: LayoutInflater, @Nullable viewGroup: ViewGroup?,
                            @Nullable args: Bundle?): View? {
    val view = layoutInflater.inflate(R.layout.dialog_recording, viewGroup, false)

    // Get views
    leftButton = view.findViewById(R.id.left_button)
    rightButton = view.findViewById(R.id.right_button)

    // Setup button clicks
    setClickListeners()

    return view
  }

  private fun setClickListeners(){
    leftButton.setOnClickListener {
      activity!!.finishAndRemoveTask()
    }

    rightButton.setOnClickListener{
      // Create settings intent
      startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
          .setData(Uri.fromParts("package", activity!!.applicationContext.packageName, null)))
    }
  }

  /**
   * Dialog Created
   */
  override fun onCreateDialog(args: Bundle?): Dialog {
    val dialog = super.onCreateDialog(args)
    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
    dialog.setCancelable(false)
    dialog.setCanceledOnTouchOutside(false)
    return dialog
  }

  override fun onResume() {
    super.onResume()

    try {
      val window = dialog?.window

      if (window != null) {
        val dialogWidth = resources.getDimension(R.dimen.dialog_width)
        if (dialogWidth < 0) {
          window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        } else {
          window.setLayout(dialogWidth.toInt(), LinearLayout.LayoutParams.WRAP_CONTENT)
        }
      }
    } catch (e: NullPointerException) {
      Utils.e(LocationDialog.TAG, "null location dialog", e)
      safeDismiss()
      return
    }
  }
}
