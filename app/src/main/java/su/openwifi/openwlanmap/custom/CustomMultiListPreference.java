package su.openwifi.openwlanmap.custom;

import android.content.Context;
import android.preference.MultiSelectListPreference;
import android.util.AttributeSet;
import java.util.Set;

/**
 * Created by tali on 06.06.18.
 */

public class CustomMultiListPreference extends MultiSelectListPreference {
  public CustomMultiListPreference(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public void setValues(Set<String> values) {
    super.setValues(values);
    if (!values.isEmpty()) {
      StringBuilder builder = new StringBuilder();
      for (String value : values) {
        builder.append(value);
        builder.append(",");
      }
      String summary = builder.toString();
      setSummary(summary.substring(0, summary.length() - 1));
    }
  }

  @Override
  protected void onDialogClosed(boolean positiveResult) {
    super.onDialogClosed(positiveResult);
    if (positiveResult && getValues().size() == 0) {
      setSummary("none");
    }
  }
}
