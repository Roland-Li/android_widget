package com.activity

import android.Manifest
import android.graphics.Color
import android.graphics.Point
import android.os.Bundle
import android.widget.LinearLayout
import com.R
import com.utils.Utils
import com.utils.theme.ThemeUtils
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.fragments.bottom_sheets.VoiceAddBottomSheet
import com.fragments.dialogs.OpenDialog
import com.fragments.dialogs.RecordingDialog
import com.models.UserData
import com.models.UserData_

/*
 * Activity that handles creating the slide-up widget bottom sheet,
 * as well as all the user dialogues required to inform the user of required
 * set up steps before they can use it.
 * Will terminate on pressing back/home/recents button; otherwise, killed by children
 */
class VoiceAddActivity : BaseActivity() {

  // Used by other classes for debugging etc
  companion object {
    val TAG: String = VoiceAddActivity::class.java.simpleName
  }

  /**
   * ----- Views -----
   */

  // Outermost containers
  private lateinit var backLayout : LinearLayout

  /**
   * ----- Private variables  -----
   */

  val AUDIO_PERMISSION_SRC = 1608
  private  var receiver : VoiceAddActivity.InnerReceiver? = null
  lateinit var voiceAddBottomSheet: VoiceAddBottomSheet

  private var openDialog : OpenDialog? = null
  private var recordingDialog : RecordingDialog? = null

  private val userData: UserData by lazy {
    UserData_.getInstance_(applicationContext)
  }

  /**
   * ----- Initializers and Lifecycle -----
   */

  private fun bindViews() {
    window.statusBarColor = Color.HSVToColor(0, floatArrayOf(0f,0f,0f))

    backLayout = findViewById(R.id.background)

    // Force the window to be screen-sized
    var size = Point()
    windowManager.defaultDisplay.getSize(size)
    backLayout.layoutParams.height = size.y
    backLayout.layoutParams.width = size.x
  }

  override fun onCreate(args: Bundle?) {
    super.onCreate(args)

    setContentView(R.layout.activity_voice_add)
    bindViews()
    setTheme(ThemeUtils.getActivityTheme(R.style.Activity_Transparent))
    createBottomSheet()

    // Removes the open animation
    overridePendingTransition(0,0)
  }

  override fun onStart() {
    super.onStart()

    // Create a filter to start listening for dialog closes (to kill the activity)
    val filter = IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)
    receiver = InnerReceiver()
    applicationContext.registerReceiver(receiver, filter)
  }

  override fun onResume() {
    super.onResume()

    // Open dialog was opened and the user added location
    if (openDialog != null && userData.hasLocationData()){
      openDialog!!.safeDismiss()
      openDialog = null
      createBottomSheet()
    }
    // Recording dialog was opened and user set permission
    else if(recordingDialog != null &&
        ContextCompat.checkSelfPermission(applicationContext,
            android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
      recordingDialog!!.safeDismiss()
      recordingDialog = null
      createBottomSheet()
    }
  }

  override fun onDestroy() {
    cleanup()
    super.onDestroy()
  }

  override fun onBackPressed() {
    // Close app
    finishAndRemoveTask()
  }

  private fun cleanup(){
    if(receiver != null) {
      applicationContext.unregisterReceiver(receiver)
      receiver = null
    }
  }

  // Always call this when killing the activity to remove it from memory stack
  override fun finishAndRemoveTask() {
    super.finishAndRemoveTask()

    // Kills the slide up animation
    overridePendingTransition(0, R.anim.design_bottom_sheet_slide_out)
  }

  /**
   * ----- Visual Helper Functions -----
   */

  /**
   * Creates the bottom sheet, but first performs two checks:
   * 1) Has the user set their location/created an account on the app? If not, prompt to do so
   * 2) Has the user enabled voice recognition? If not, prompt to do so
   */
  private fun createBottomSheet(){
    // Perform initial check for whether or not user has already set their location/user
    if (!userData.hasLocationData()){
      // Open a dialog explaining the need for the permission
      val tag = OpenDialog.TAG
      val frag = supportFragmentManager.findFragmentByTag(tag)
      if (!OpenDialog::class.java.isInstance(frag)) {
        // Create dialog
        openDialog = OpenDialog()

        try {
          openDialog!!.isCancelable = false // Manually set this or it won't work
          openDialog!!.show(supportFragmentManager, tag)
        } catch (e: IllegalStateException) {
          Utils.e(TAG, "Failed to show on boarding location dialog", e)
        }
      }
    }
    // Perform a permission check as a prereq to creating the bottom sheet
    else if(ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
      // We don't have location permission, open standard permission request
      if (!userData.requestedRecordingPermission ||
          (Utils.isAtLeastM() && shouldShowRequestPermissionRationale(Manifest.permission.RECORD_AUDIO)))
      {
        // We haven't requested the permission or the user has denied it before
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            AUDIO_PERMISSION_SRC)
      }

      // User selected "Never ask again", show custom dialog instead
      else {
        val tag = RecordingDialog.TAG
        val frag = supportFragmentManager.findFragmentByTag(tag)
        if (!RecordingDialog::class.java.isInstance(frag)) {
          // Create dialog
          recordingDialog = RecordingDialog()

          try {
            recordingDialog!!.isCancelable = false // Manually set this or it won't work
            recordingDialog!!.show(supportFragmentManager, tag)
          } catch (e: IllegalStateException) {
            Utils.e(TAG, "Failed to show on boarding location dialog", e)
          }
        }
      }
    }
    else {
      // Actually create the bottom sheet
      val tag = VoiceAddBottomSheet.TAG
      val frag = supportFragmentManager.findFragmentByTag(tag)
      if (!VoiceAddBottomSheet::class.java.isInstance(frag)) {
        // Create Dialog
        voiceAddBottomSheet = VoiceAddBottomSheet()

        try {
          voiceAddBottomSheet.show(supportFragmentManager, tag)

          // Make the navbar white only if we have the bottomsheet open
          window.navigationBarColor = ContextCompat.getColor(applicationContext,R.color.white)

        } catch (e: IllegalStateException) {
          Utils.e(TAG, "Failed to show voice add bottom sheet", e)
        }
      }
    }
  }

  /**
   * ----- Other Helper Functions -----
   */


  /* 
   * Function that recieves a permission result
   * Granted: creates the bottom sheet if granted
   * Otherwise: kills the activity
  */ 
  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    when (requestCode){
      AUDIO_PERMISSION_SRC ->
      {
        // Remember that we requested the permission; may change the dialog we show later on
        userData.setRequestedRecordingPermission()

        // If permission was given, try to open the dialog; otherwise close
        if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
          // Try again
          createBottomSheet()
        } else {
          finishAndRemoveTask()
        }
      }
    }
  }

  /**
   * ----- Subclasses -----
   */

  /**
   * Receiver to intercept "recent apps" button press
   * Manually closes the bottom sheet to remove it from recents
   */
 inner class InnerReceiver : BroadcastReceiver() {
   val SYSTEM_DIALOG_REASON_KEY = "reason"
   val SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps"

   override fun onReceive(context: Context?, intent: Intent?) {
     val action = intent!!.action
     if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
       val reason = intent.getStringExtra(SYSTEM_DIALOG_REASON_KEY)
       if (reason != null) {
         if (reason.equals(SYSTEM_DIALOG_REASON_RECENT_APPS)) {
           finishAndRemoveTask()
         }
       }
     }
   }
 }

}
