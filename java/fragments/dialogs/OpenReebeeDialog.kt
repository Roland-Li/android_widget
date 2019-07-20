package com.fragments.dialogs

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import com.R
import com.utils.Utils
import org.jetbrains.annotations.Nullable

/**
 * Simple dialog for the purpose of informing the user to open the app for setup
 * Yes: Opens the application
 * No: Closes the dialog and the base activity
 */
class OpenDialog : BaseDialog() {

  /* *********************
   * COMPANION OBJECT
   * ******************* */

  companion object {
    val TAG: String = OpenDialog::class.java.simpleName
  }

  /* *********************
   * CLASS VARIABLES
   * ******************* */

  /**
   * Views
   */
  private lateinit var noButton: MaterialButton
  private lateinit var yesButton: MaterialButton

  /* *********************
   * OVERRIDE METHODS
   * ******************* */

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
    val view = layoutInflater.inflate(R.layout.dialog_open, viewGroup, false)

    // Get views
    noButton = view.findViewById(R.id.no_button)
    yesButton = view.findViewById(R.id.yes_button)

    // Setup view
    setClickListeners()

    return view
  }

  /**
   * Set click listeners
   */
  private fun setClickListeners() {
    // Set no thanks button click
    noButton.setOnClickListener {
      activity!!.finishAndRemoveTask()
    }

    // Set yes button click
    yesButton.setOnClickListener {
      // Create settings intent
      startActivity(Intent(context!!.packageManager.getLaunchIntentForPackage(context!!.packageName)))
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

  /**
   * Dialog Resumed
   */
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
      Utils.e(TAG, "null auto delete expired opt dialog", e)
      safeDismiss()
    }
  }
}
