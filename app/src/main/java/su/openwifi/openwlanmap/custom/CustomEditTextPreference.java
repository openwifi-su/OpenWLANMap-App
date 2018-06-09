package su.openwifi.openwlanmap.custom;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Created by tali on 31.05.18.
 */

public class CustomEditTextPreference extends EditTextPreference {
  public CustomEditTextPreference(Context context) {
    super(context);
  }

  public CustomEditTextPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public CharSequence getSummary() {
    return getText();
  }
}
