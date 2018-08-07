package su.openwifi.openwlanmap.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.ViewGroup;
import su.openwifi.openwlanmap.R;

public class HudView extends ViewGroup {
  private Paint paintConfig;
  private String value;
  private SharedPreferences sharedPreferences;

  /**
   * Constructor.
   * @param context : app context
   * @param sharedPreferences : sharePreference to check overlay setting
   */
  public HudView(Context context, SharedPreferences sharedPreferences) {
    super(context);
    this.sharedPreferences = sharedPreferences;
    paintConfig = new Paint();
    paintConfig.setAntiAlias(true);
    paintConfig.setTextSize(125);
    paintConfig.setColor(getResources().getColor(R.color.not_define));
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

  /**
   * This method changes the color of overlay view after given mode.
   * @param mode : last method used to define location
   */
  public void setMode(WifiLocator.LOC_METHOD mode) {
    switch (mode) {
      case GPS:
        paintConfig.setColor(getResources().getColor(R.color.gps));
        break;
      case LIBWLOCATE:
        paintConfig.setColor(getResources().getColor(R.color.libwlocate));
        break;
      case NOT_DEFINE:
        paintConfig.setColor(getResources().getColor(R.color.not_define));
        break;
      default:
        break;
    }
  }
}
