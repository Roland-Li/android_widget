package com.widgets;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.R;
import com.widgets.animators.BarParamsAnimator;
import com.widgets.animators.IdleAnimator;
import com.widgets.animators.RmsAnimator;
import com.widgets.animators.RotatingAnimator;
import com.widgets.animators.TransformAnimator;

import java.util.ArrayList;
import java.util.List;

import androidx.core.content.ContextCompat;

/**
 * RecognitionProgressView handles managing and animating a group of RecognitionBars
 */
public class RecognitionProgressView extends View {

  public static final int BARS_COUNT = 5;

  private static final int CIRCLE_RADIUS_DP = 4;
  private static final int CIRCLE_SPACING_DP = 12;
  private static final int ROTATION_RADIUS_DP = 25;
  private static final int IDLE_FLOATING_AMPLITUDE_DP = 3;

  private static final int[] DEFAULT_BARS_HEIGHT_DP = { 42, 36, 47, 34, 44 };

  private static final float MDPI_DENSITY = 1.5f;

  private final List<RecognitionBar> recognitionBars = new ArrayList<>();
  private Paint paint;
  private BarParamsAnimator animator;

  private int radius;
  private int spacing;
  private int rotationRadius;
  private int amplitude;

  private float density;

  private boolean isSpeaking;
  private boolean animating;

  private int barColor = -1;
  private int[] barColors;
  private int[] barMaxHeights;

  public RecognitionProgressView(Context context) {
    super(context);
    init();
  }

  public RecognitionProgressView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public RecognitionProgressView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  /**
   * ---- PROPERTY INITIALIZERS ----
   */

  private void init() {
    paint = new Paint();
    paint.setFlags(Paint.ANTI_ALIAS_FLAG);
    paint.setColor(Color.GRAY);

    density = getResources().getDisplayMetrics().density;

    radius = (int) (CIRCLE_RADIUS_DP * density);
    spacing = (int) (CIRCLE_SPACING_DP * density);
    rotationRadius = (int) (ROTATION_RADIUS_DP * density);
    amplitude = (int) (IDLE_FLOATING_AMPLITUDE_DP * density);

    if (density <= MDPI_DENSITY) {
      amplitude *= 2;
    }

    // Set default bar colors
    barColors = new int[]{
        ContextCompat.getColor(getContext(), R.color.primary),
        ContextCompat.getColor(getContext(), R.color.primary_75),
        ContextCompat.getColor(getContext(), R.color.primary),
        ContextCompat.getColor(getContext(), R.color.primary_75),
        ContextCompat.getColor(getContext(), R.color.primary),
    };
  }

  private void initBars() {
    final List<Integer> heights = initBarHeights();
    int firstCirclePosition = getMeasuredWidth() / 2 -
        2 * spacing -
        4 * radius;
    for (int i = 0; i < BARS_COUNT; i++) {
      int x = firstCirclePosition + (2 * radius + spacing) * i;
      RecognitionBar bar = new RecognitionBar(x, getMeasuredHeight() / 2, 2 * radius, heights.get(i), radius);
      recognitionBars.add(bar);
    }
  }

  private List<Integer> initBarHeights() {
    final List<Integer> barHeights = new ArrayList<>();
    if (barMaxHeights == null) {
      for (int i = 0; i < BARS_COUNT; i++) {
        barHeights.add((int) (DEFAULT_BARS_HEIGHT_DP[i] * density));
      }
    } else {
      for (int i = 0; i < BARS_COUNT; i++) {
        barHeights.add((int) (barMaxHeights[i] * density));
      }
    }
    return barHeights;
  }

  private void resetBars() {
    for (RecognitionBar bar : recognitionBars) {
      bar.setX(bar.getStartX());
      bar.setY(bar.getStartY());
      bar.setHeight(radius * 2);
      bar.update();
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);
    if (recognitionBars.isEmpty()) {
      initBars();
    } else if (changed) {
      recognitionBars.clear();
      initBars();
    }
  }

  /**
   * This function handles the actual animation by updating RecognitionBar properties and
   * redrawing them on the screen
   */
  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (recognitionBars.isEmpty()) {
      return;
    }

    // Updates the recognition bars based off the current animator
    if (animating) {
      animator.animate();
    }

    // Draws the bars
    for (int i = 0; i < recognitionBars.size(); i++) {
      RecognitionBar bar = recognitionBars.get(i);
      if (barColors != null) {
        paint.setColor(barColors[i]);
      } else if (barColor != -1) {
        paint.setColor(barColor);
      }
      canvas.drawRoundRect(bar.getRect(), radius, radius, paint);
    }

    if (animating) {
      invalidate();
    }
  }

  /**
   * ---- ANIMATION TRIGGER FUNCTIONS (Call these!) ----
   */

  /**
   * Starts animating view
   */
  public void play() {
    startIdleInterpolation();
    animating = true;
    invalidate();
  }

  /**
   * Stops animating view
   */
  public void stop() {
    if (animator != null) {
      animator.stop();
      animator = null;
    }
    animating = false;
    resetBars();
  }

  // Call these through RecognitionListener
  public void onBeginningOfSpeech() {
    isSpeaking = true;
  }

  public void onRmsChanged(float rmsdB) {
    if (animator == null || rmsdB < 1f) {
      return;
    }
    if (!(animator instanceof RmsAnimator) && isSpeaking) {
      startRmsInterpolation();
    }
    if (animator instanceof RmsAnimator) {
      ((RmsAnimator) animator).onRmsChanged(rmsdB);
    }
  }

  public void onEndOfSpeech() {
    isSpeaking = false;
    startTransformInterpolation();
  }

  private void startIdleInterpolation() {
    animator = new IdleAnimator(recognitionBars, amplitude);
    animator.start();
  }

  private void startRmsInterpolation() {
    resetBars();
    animator = new RmsAnimator(recognitionBars);
    animator.start();
  }

  private void startTransformInterpolation() {
    resetBars();
    animator = new TransformAnimator(recognitionBars, getWidth() / 2, getHeight() / 2, rotationRadius);
    animator.start();
    ((TransformAnimator) animator).setOnInterpolationFinishedListener(new TransformAnimator.OnInterpolationFinishedListener() {
      @Override
      public void onFinished() {
        startRotateInterpolation();
      }
    });
  }

  private void startRotateInterpolation() {
    animator = new RotatingAnimator(recognitionBars, getWidth() / 2, getHeight() / 2);
    animator.start();
  }


  /**
   * ---- HELPERS FOR MANUALLY SETTING PROPERTIES ----
   */

  /**
   * Set one color to all bars in view
   */
  public void setSingleColor(int color) {
    barColor = color;
  }

  /**
   * Set different colors to bars in view
   *
   * @param colors - array with size = {@link #BARS_COUNT}
   */
  public void setColors(int[] colors) {
    if (colors == null) return;

    barColors = new int[BARS_COUNT];
    if (colors.length < BARS_COUNT) {
      System.arraycopy(colors, 0, barColors, 0, colors.length);
      for (int i = colors.length; i < BARS_COUNT; i++) {
        barColors[i] = colors[0];
      }
    } else {
      System.arraycopy(colors, 0, barColors, 0, BARS_COUNT);
    }
  }

  /**
   * Set sizes of bars in view
   *
   * @param heights - array with size = {@link #BARS_COUNT},
   * if not set uses default bars heights
   */
  public void setBarMaxHeightsInDp(int[] heights) {
    if (heights == null) return;

    barMaxHeights = new int[BARS_COUNT];
    if (heights.length < BARS_COUNT) {
      System.arraycopy(heights, 0, barMaxHeights, 0, heights.length);
      for (int i = heights.length; i < BARS_COUNT; i++) {
        barMaxHeights[i] = heights[0];
      }
    } else {
      System.arraycopy(heights, 0, barMaxHeights, 0, BARS_COUNT);
    }
  }

  /**
   * Set radius of circle
   *
   * @param radius - Default value = {@link #CIRCLE_RADIUS_DP}
   */
  public void setCircleRadiusInDp(int radius) {
    this.radius = (int) (radius * density);
  }

  /**
   * Set spacing between circles
   *
   * @param spacing - Default value = {@link #CIRCLE_SPACING_DP}
   */
  public void setSpacingInDp(int spacing) {
    this.spacing = (int) (spacing * density);
  }

  /**
   * Set idle animation amplitude
   *
   * @param amplitude - Default value = {@link #IDLE_FLOATING_AMPLITUDE_DP}
   */
  public void setIdleStateAmplitudeInDp(int amplitude) {
    this.amplitude = (int) (amplitude * density);
  }

  /**
   * Set rotation animation radius
   *
   * @param radius - Default value = {@link #ROTATION_RADIUS_DP}
   */
  public void setRotationRadiusInDp(int radius) {
    this.rotationRadius = (int) (radius * density);
  }

}
