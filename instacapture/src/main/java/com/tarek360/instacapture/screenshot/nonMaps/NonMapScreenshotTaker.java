package com.tarek360.instacapture.screenshot.nonMaps;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import com.tarek360.instacapture.exception.ScreenCapturingFailedException;
import com.tarek360.instacapture.utility.Logger;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tarek on 5/17/16.
 */
public final class NonMapScreenshotTaker {

  private NonMapScreenshotTaker() {
  }

  /**
   * Capture screenshot for the current activity and return bitmap of it.
   *
   * @param activity current activity.
   * @param ignoredViews from the screenshot.
   * @return Bitmap of screenshot.
   * @throws ScreenCapturingFailedException if unexpected error is occurred during capturing
   * screenshot
   */
  public static Bitmap getScreenshotBitmap(Activity activity, View[] ignoredViews) {
    if (activity == null) {
      throw new IllegalArgumentException("Parameter activity cannot be null.");
    }

    final List<RootViewInfo> viewRoots = FieldHelper.getRootViews(activity);
    Logger.d("viewRoots count: " + viewRoots.size());
    View main = activity.getWindow().getDecorView();

    final Bitmap bitmap;
    try {
      bitmap = Bitmap.createBitmap(main.getWidth(), main.getHeight(), Bitmap.Config.ARGB_8888);
    } catch (final IllegalArgumentException e) {
      return null;
    }

    drawRootsToBitmap(viewRoots, bitmap, ignoredViews);

    return bitmap;
  }

  private static void drawRootsToBitmap(List<RootViewInfo> viewRoots, Bitmap bitmap,
      View[] ignoredViews) {
    count = 0;
    for (RootViewInfo rootData : viewRoots) {
      drawRootToBitmap(rootData, bitmap, ignoredViews);
    }
  }

  private static int count = 0;

  private static void drawRootToBitmap(final RootViewInfo rootViewInfo, Bitmap bitmap,
      View[] ignoredViews) {
    count++;
    if (count == 1) {
      return;
    }
    // support dim screen
    if ((rootViewInfo.getLayoutParams().flags & WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        == WindowManager.LayoutParams.FLAG_DIM_BEHIND) {
      Canvas dimCanvas = new Canvas(bitmap);

      int alpha = (int) (255 * rootViewInfo.getLayoutParams().dimAmount);
      dimCanvas.drawARGB(alpha, 0, 0, 0);
    }

    final Canvas canvas = new Canvas(bitmap);
    canvas.translate(rootViewInfo.getRect().left, rootViewInfo.getRect().top);

    int[] ignoredViewsVisibility = null;
    if (ignoredViews != null) {
      ignoredViewsVisibility = new int[ignoredViews.length];
    }

    if (ignoredViews != null) {
      for (int i = 0; i < ignoredViews.length; i++) {
        if (ignoredViews[i] != null) {
          ignoredViewsVisibility[i] = ignoredViews[i].getVisibility();
          ignoredViews[i].setVisibility(View.INVISIBLE);
        }
      }
    }

    Log.d("zxzx", "rootViewInfo.getView(): " + rootViewInfo.getView());

    //Draw undrawable views
    rootViewInfo.getView().draw(canvas);
    drawUnDrawableViews(rootViewInfo.getView(), canvas);

    if (ignoredViews != null) {
      for (int i = 0; i < ignoredViews.length; i++) {
        if (ignoredViews[i] != null) {
          ignoredViews[i].setVisibility(ignoredViewsVisibility[i]);
        }
      }
    }
  }

  private static ArrayList<View> drawUnDrawableViews(View v, Canvas canvas) {

    if (!(v instanceof ViewGroup)) {
      ArrayList<View> viewArrayList = new ArrayList<>();
      viewArrayList.add(v);
      return viewArrayList;
    }

    ArrayList<View> result = new ArrayList<>();

    ViewGroup viewGroup = (ViewGroup) v;
    for (int i = 0; i < viewGroup.getChildCount(); i++) {

      View child = viewGroup.getChildAt(i);

      ArrayList<View> viewArrayList = new ArrayList<>();
      viewArrayList.add(v);
      viewArrayList.addAll(drawUnDrawableViews(child, canvas));

      Log.d("zxzx", "child: " + child);

      if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH
          && child instanceof TextureView) {
        drawTextureView((TextureView) child, canvas);
      }

      result.addAll(viewArrayList);
    }
    return result;
  }

  @RequiresApi(api = Build.VERSION_CODES.ICE_CREAM_SANDWICH)
  private static void drawTextureView(TextureView textureView, Canvas canvas) {
    Logger.d("Drawing TextureView");

    int[] textureViewLocation = new int[2];
    textureView.getLocationOnScreen(textureViewLocation);
    textureView.setDrawingCacheEnabled(true);
    Bitmap textureViewBitmap = textureView.getBitmap();
    if (textureViewBitmap != null) {
      Paint paint = new Paint();
      paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_ATOP));
      canvas.drawBitmap(textureViewBitmap, textureViewLocation[0], textureViewLocation[1], paint);
      textureViewBitmap.recycle();
      textureView.destroyDrawingCache();
      textureView.setDrawingCacheEnabled(false);
    }
  }
}

