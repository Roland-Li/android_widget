package com.fragments.bottom_sheets

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.speech.RecognitionListener
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.R
import com.activity.BookshelfActivity_
import com.analytics.EventLoggingService
import com.analytics.events.VoiceAddWidgetAnalyticEvents
import com.data.shared_models.ManualItem
import com.data.shared_models.ShoppingItem
import com.data.shared_models.Store
import com.handlers.ShoppingListHandler
import com.handlers.ShoppingListHandler_
import com.helpers.dimens.BookshelfDimens
import com.helpers.dimens.BookshelfDimens_
import com.helpers.threading.BackgroundExecutor
import com.models.Constants
import com.models.UserData
import com.models.UserData_
import com.utils.Utils
import com.utils.speech.SpeechRecognizerManager
import com.widgets.RecognitionProgressView
import org.jetbrains.annotations.Nullable
import java.util.ArrayList


/**
 * This Bottom Sheet allows users to add items via voice recognition and provides appropriate
 * visual feedback for actions
 */

class VoiceAddBottomSheet : BaseBottomSheet() {

  companion object {
    val TAG: String = VoiceAddBottomSheet::class.java.simpleName
  }

  /**
   * ----- Views -----
   */

  // Outermost containers
  private lateinit var backLayout : CoordinatorLayout
  private lateinit var scrollView : NestedScrollView

  // Listen screen
  private lateinit var listeningLayout : LinearLayout
  private lateinit var resultText : TextView
  private lateinit var descriptionText : TextView
  private lateinit var recognitionView : RecognitionProgressView

  // Result screen
  private lateinit var resultLayout : LinearLayout
  private lateinit var retryButton : Button
  private lateinit var mainResultLayout : LinearLayout
  private var altResultLayouts = ArrayList<LinearLayout>()

  private lateinit var shoppingListIcon : ImageView
  private lateinit var searchIcon : ImageView

  private var handler : Handler? = null

  /**
   * ----- Private variables  -----
   */
  private var speechManager : SpeechRecognizerManager? = null

  private var currentLayout : LinearLayout? = null
  private var addedResult : Boolean = false
  private var curRank : Int = 0
  private var closed : Boolean = false
  private var animatedOpen: Boolean = false

  private var resultStrings : ArrayList<String>? = null


  // Get handler & userData for adding the new manual items
  private val shoppingListHandler: ShoppingListHandler by lazy {
    ShoppingListHandler_.getInstance_(context)
  }

  private val userData: UserData by lazy {
    UserData_.getInstance_(context)
  }

  private val bookshelfDimens: BookshelfDimens by lazy {
    BookshelfDimens_.getInstance_(context)
  }

  /* *********************
   * OVERRIDE METHODS
   * ******************* */

  /**
   * Set Bottom Sheet theme
   */
  override fun getTheme(): Int = R.style.BottomSheet_Widget

  override fun onCreate(args: Bundle?) {
    // Restore state
    restoreSavedInstanceState(args)

    super.onCreate(args)
  }

  /**
   * Bottom Sheet Dialog Created
   *
   * @param args a mapping from String keys to various values
   * @return created Dialog
   */
  override fun onCreateDialog(args: Bundle?): Dialog {
    val bottomSheet = super.onCreateDialog(args) as BottomSheetDialog

    bottomSheet.setOnShowListener { dialog ->
      val internalBottomSheet = (dialog as BottomSheetDialog)
          .findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
      val bottomSheetBehavior = BottomSheetBehavior.from(internalBottomSheet)
      bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
      bottomSheetBehavior.setBottomSheetCallback(bottomSheetBehaviorCallback)
    }

    return bottomSheet
  }

  /**
   * Bottom Sheet behaviour callback to dismiss Bottom Sheet on new state
   */
  private val bottomSheetBehaviorCallback = object : Utils.BottomSheetBehaviorCallback() {
    override fun onStateChanged(bottomSheet: View, newState: Int) {
      if (newState != BottomSheetBehavior.STATE_DRAGGING
          && newState != BottomSheetBehavior.STATE_EXPANDED) {
        safeDismiss()
      }
    }
  }

  /**
   * View Created
   *
   * @param layoutInflater view inflater
   * @param viewGroup      Dialog view container
   * @param args           a mapping from String keys to various values
   * @return created view
   */
  @Nullable
  override fun onCreateView(layoutInflater: LayoutInflater, @Nullable viewGroup: ViewGroup?,
                            @Nullable args: Bundle?): View? {
    val view = layoutInflater.inflate(R.layout.bottom_sheet_voice_add, viewGroup, false)

    backLayout = view.findViewById(R.id.back_layout)
    scrollView = view.findViewById(R.id.scroll_view)

    listeningLayout = view.findViewById(R.id.listening_layout)
    recognitionView = view.findViewById(R.id.recognition_view)
    resultText = view.findViewById(R.id.partial_result_text)
    descriptionText = view.findViewById(R.id.description_text)

    resultLayout = view.findViewById(R.id.result_layout)

    mainResultLayout = view.findViewById(R.id.result_1)
    altResultLayouts.add(view.findViewById(R.id.result_2))
    altResultLayouts.add(view.findViewById(R.id.result_3))
    altResultLayouts.add(view.findViewById(R.id.result_4))

    searchIcon = view.findViewById(R.id.search_icon)
    shoppingListIcon = view.findViewById(R.id.shopping_list_icon)
    retryButton = view.findViewById(R.id.retry_button)

    setupListeners()

    return view
  }

  /**
   * Setups all the button clicks
   */
  private fun setupListeners(){
    speechManager = SpeechRecognizerManager(context!!, SpeechRecognitionListener())

    // Click to dismiss -  Only allow closing if the sheet is already animatedOpen
    backLayout.setOnClickListener { if(animatedOpen) safeDismiss() }

    // Set up result click listeners
    for (i in 0 until altResultLayouts.size){
      altResultLayouts[i].setOnClickListener {
        val mainText = mainResultLayout.getChildAt(0) as TextView
        val altText = altResultLayouts[i].getChildAt(0) as TextView

        curRank = i + 1 // Need to factor in the main result

        if (!resultStrings.isNullOrEmpty() && resultStrings!!.size > curRank){  // Safety check
          // Swap the underlying data (for state restoration)
          val textToSwap = resultStrings!![0]
          resultStrings!![0] = resultStrings!![curRank]
          resultStrings!![curRank] = textToSwap

          // Update the actual text in the views
          mainText.text = resultStrings!![0]
          altText.text = resultStrings!![curRank]
        }
      }
    }

    // Clicking on the main result sets the check and submits to add the manual item
    mainResultLayout.setOnClickListener {
      val textView = mainResultLayout.getChildAt(0) as TextView
      val imageView = mainResultLayout.getChildAt(1) as ImageView

      imageView.setImageDrawable(
          resources.getDrawable(R.drawable.ic_in_list_white, context!!.theme)
      )

      addManualItem(textView.text.toString())
    }

    // Sets the shopping list icon to link to the shopping list
    shoppingListIcon.setOnClickListener {
      startActivity(
          Intent(context, BookshelfActivity_::class.java).apply {
            action = Intent.ACTION_VIEW
          }.putExtra("sourceID", Constants.SRC_WIDGET)
              .putExtra("tab", BookshelfActivity_.TAB.SHOPPING_LIST.position)
      )

      dismiss()
    }

    // Sets the search icon to link to the search and pass through the query in the main result
    searchIcon.setOnClickListener {
      startActivity(
          Intent(context, BookshelfActivity_::class.java).apply {
            action = Intent.ACTION_VIEW
          }.putExtra("sourceID", Constants.SRC_WIDGET)
              .putExtra("q", (mainResultLayout.getChildAt(0) as TextView).text.toString())
              .putExtra("tab", BookshelfActivity_.TAB.SEARCH.position)
      )

      dismiss()
    }

    // Retry button simply swaps layouts back and runs the recognizer
    retryButton.setOnClickListener{ switchLayout(listeningLayout,::runRecognizer) }
  }

  /**
   * Create the handler and setup the initial view without animations
   */
  override fun onStart() {
    super.onStart()

    handler = Handler()

    if (updateResultLayout()){ // Checks and updates result layout
      // Manually set self to result layout and update it
      currentLayout = resultLayout
      resultLayout.alpha = 1f
      listeningLayout.alpha = 0f
    }
    else {
      // Manually start with the listening layout
      currentLayout = listeningLayout
      resultLayout.alpha = 0f
      listeningLayout.alpha = 1f

      // Trigger the listening after a delay
      handler!!.postDelayed({
        descriptionText.text = getString(R.string.voice_add_listening)
        runRecognizer()
      }, Constants.DELAY_FOCUS)
    }
  }

  /**
   * Perform cleanup to stop queued actions from occuring post destroy (doesn't clean up properly?)
   */
  override fun onDestroy() {
    handler?.removeCallbacksAndMessages(null) // Remove any queued actions
    handler = null
    speechManager!!.destroy() // Stops any pending recognition listens
    speechManager = null
    super.onDestroy()
    activity!!.finishAndRemoveTask()
  }

  /**
   * Reset the width of the bottom sheet
   */
  override fun onResume() {
    super.onResume()

    val bottomSheetWidth = bookshelfDimens.getBottomSheetWidth(
        resources.getDimension(R.dimen.bottom_sheet_max_width))

    if (bottomSheetWidth != null) {
      val lp = scrollView.layoutParams
      lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
      lp.width = bottomSheetWidth
      scrollView.layoutParams = lp
    }
  }

  /**
   * Restore Bottom Sheet state
   *
   * @param args a mapping from String keys to various values
   */
  @Suppress("UNCHECKED_CAST")
  private fun restoreSavedInstanceState(args: Bundle?) {
    if (args == null) return
    resultStrings = args.getSerializable("results") as ArrayList<String>
  }

  /**
   * Save Bottom Sheet state
   *
   * @param args a mapping from String keys to various values
   */
  override fun onSaveInstanceState(args: Bundle) {
    super.onSaveInstanceState(args)
    args.putSerializable("results", resultStrings)
  }

  /**
   * ----- Visual Helper Functions -----
   */

  /**
   * Handles animating the swap between layouts
   *
   * @param nextLayout  The next LinearLayout to display
   * @param callback    A function to call when the animation is over (set to null if not needed)
   */
  fun switchLayout(nextLayout : LinearLayout, callback : (()->Unit)?){
    if (addedResult || currentLayout == nextLayout) return

    // Hide the current layout; reset some views if needed
    val hide = ObjectAnimator.ofFloat(currentLayout!!, "alpha", 1f, 0f).apply{
      duration = 500
      addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          if (currentLayout == listeningLayout){
            currentLayout!!.visibility = View.GONE
          }
          else {
            // Going back to the listening layout, reset text etc
            resultText.text = ""
            descriptionText.text = getString(R.string.voice_add_listening)
            resultStrings = null
            currentLayout!!.visibility = View.INVISIBLE
          }

          nextLayout.visibility = View.VISIBLE
        }
      })
    }

    // Display the next layout, call the animation on end
    val show = ObjectAnimator.ofFloat(nextLayout, "alpha", 0f, 1f).apply{
      duration = 500
      addListener(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          currentLayout = nextLayout
          if (callback != null) callback()
        }
      })
    }

    // Queue the animations and play them
    AnimatorSet().apply{
      play(hide).before(show)
      start()
    }
  }


  /**
   * Updates the results in the ResultLayout to whatever is stored inside the resultStrings var
   *
   * @return Boolean    True if succeeded, false if failed
   */
  private fun updateResultLayout() : Boolean {
    if (resultStrings.isNullOrEmpty()) { return false }

    // Set the main result
    (mainResultLayout.getChildAt(0) as TextView).text = resultStrings!![0]

    // Loop through and set the alternate results
    for (i in 0 until altResultLayouts.size){
      val innerLayout = altResultLayouts[i]

      // Index is + 1 because we already handled the first result
      val resultIndex = i + 1

      val innerText = innerLayout.getChildAt(0) as TextView

      if (resultIndex >= resultStrings!!.size){ // No more results, set to disabled buttons
        innerLayout.alpha = 0.2f
        innerLayout.isEnabled = false
        innerText.text = ""
      }
      else { // Otherwise fill result
        innerLayout.alpha = 1f
        innerLayout.isEnabled = true
        innerText.text = resultStrings!![resultIndex]
      }
    }

    descriptionText.text = getString(R.string.voice_add_correction_main)

    return true
  }


  /**
   * ----- Other Helper Functions -----
   */

  /**
   * Starts the VoiceRecognizer and the animating UI
   */
  private fun runRecognizer(){
    if (closed) return
    // Assumes that permissions were already obtained by the parent activity
    recognitionView.play()
    speechManager!!.startListening()
  }

  /**
   * Handles adding the item to the shopping list, logging a tracking event, and closing after a delay
   */
  private fun addManualItem(title: String){
    if (addedResult) return // Prevent if add result queued

    addedResult = true

    // Post an event to track the add
    EventLoggingService.logEvent(
        context,
        VoiceAddWidgetAnalyticEvents.addWidgetAnalyticEvent(VoiceAddWidgetAnalyticEvents.ITEM_ADDED)
            .putResultRank(curRank.toLong())
    )

    BackgroundExecutor.execute(object : BackgroundExecutor.Task() {
      override fun execute() {
        // Add manual item
        val manualItem = ManualItem(title)
        val shoppingItem = ShoppingItem(
            manualItem, Store(Constants.MY_LIST_ID, getString(R.string.my_list)), userData.activeUserGroupID, userData.dateOffset.toDouble())

        shoppingListHandler.addManualItem(shoppingItem, Constants.SRC_SHOPPING_LIST)
      }
    })

    handler!!.postDelayed({ dismiss() }, Constants.DELAY_FOCUS)
  }


  /**
   * ----- Subclasses -----
   */

  /**
   * RecognitionListener receives calls from the SpeechRecognizer and updates UI accordingly
   */
  inner class SpeechRecognitionListener : RecognitionListener
  {
    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEvent(eventType: Int, params: Bundle?) {}

    override fun onRmsChanged(rmsdB: Float) {
      recognitionView.onRmsChanged(rmsdB)
    }

    override fun onBeginningOfSpeech() {
      recognitionView.onBeginningOfSpeech()
    }

    override fun onEndOfSpeech() {
      recognitionView.onEndOfSpeech()
    }

    /**
     * Called when the recognizer runs into an error
     * This function will display a human understandable message to the user and then automatically
     * retry listening
     *
     * @param error    Error code
     */
    override fun onError(error: Int) {
      speechManager!!.stopListening()
      recognitionView.stop()

      when(error){
        SpeechRecognizer.ERROR_AUDIO, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
          // No results, time out and close
          descriptionText.text = getString(R.string.voice_add_error_audio)
        }
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, SpeechRecognizer.ERROR_SERVER -> {
          // Network issue, inform user
          descriptionText.text = getString(R.string.error_no_internet)
        }
        SpeechRecognizer.ERROR_NO_MATCH -> {
          // Could not understand
          descriptionText.text = getString(R.string.voice_add_error_interpretation)
        }
        else -> {
          // Ignore and do nothing
          descriptionText.text = getString(R.string.error_default)
        }
      }

      resultText.text = ""
      descriptionText.setTextColor(ContextCompat.getColor(context!!, R.color.google_red))

      // Retries listening after displaying the error
      handler!!.postDelayed({
        descriptionText.text = getString(R.string.voice_add_listening)
        descriptionText.setTextColor(ContextCompat.getColor(context!!, R.color.default_text_rev))
        runRecognizer()
      }, 2500)
    }

    /**
     * Called occasionally by the recognizer- there is no guaranteed behavior
     * This function updates the display to show partial results and attempts to cut off
     * voice input after so many words, by processing the first result
     *
     * @param partialResults  The partial result bundle, which may be empty
     */
    override fun onPartialResults(partialResults: Bundle?) {
      // Preemptively analyze the results to see if we need to cut off the speaker
      val resultStrings = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: return

      // Safety check as it could be empty
      if (resultStrings.isEmpty()) return

      if (resultStrings[0].isNotBlank()) resultText.text = resultStrings[0]

      // If the first result has over 4 words, stop the listener
      // Note this doesn't necessarily trigger as soon as 4 words have been spoken; there is a delay
      if (resultStrings.size > 0 && resultStrings[0].split(' ').size >= 4) speechManager!!.interruptListening()
    }

    /**
     * Called once the recognizer is finished, returning the results
     * Includes up to 5 results and their related confidence scores
     * This function updates the UI to said results
     *
     * @param results   Result bundle sent from the recognizer
     */
    override fun onResults(results: Bundle?) {
      speechManager!!.stopListening()
      recognitionView.stop()

      if (results == null) return

      // Update result view text
      resultStrings = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

      // Update the result layout. If it failed, trigger error processing
      if (!updateResultLayout()){
        onError(-1) // Some kind of error, handle it
        return
      }

      // Switch the views
      switchLayout(resultLayout, null)
    }
  } // SpeechRecognitionListener
}
