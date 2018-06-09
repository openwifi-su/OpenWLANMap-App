package su.openwifi.openwlanmap;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.Nullable;

/**
 * Created by tali on 30.05.18.
 */

public class SettingFragment extends PreferenceFragment {
  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //Load preference from xml
    addPreferencesFromResource(R.xml.preferences);
  }
}
