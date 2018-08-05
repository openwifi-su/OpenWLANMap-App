package su.openwifi.openwlanmap.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.ViewGroup;
import su.openwifi.openwlanmap.R;

public class HudView extends ViewGroup {
  private final Paint paintConfig;
  private String value;
  private SharedPreferences sharedPreferences;

  public HudView(Context context, SharedPreferences sharedPreferences) {
    super(context);
    this.sharedPreferences = sharedPreferences;
    paintConfig = new Paint();
    paintConfig.setAntiAlias(true);
    paintConfig.setTextSize(125);
    paintConfig.setColor(getResources().getColor(R.color.colorPrimary));
    paintConfig.setStyle(Paint.Style.FILL);
    paintConfig.setTypeface(Typeface.DEFAULT_BOLD);
  }

  @Override
  protected void dispatchDraw(Canvas canvas) {
    if (!sharedPreferences.getBoolean("pref_show_counter", false)) {
      return;
    }
    super.dispatchDraw(canvas);
    canvas.drawText(this.value, 5, 120, paintConfig);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    // Do nothing
  }

  public void setValue(long ival) {
    value = "" + ival;
  }
}
