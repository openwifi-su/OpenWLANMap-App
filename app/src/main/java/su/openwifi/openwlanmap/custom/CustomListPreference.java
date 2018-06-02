package su.openwifi.openwlanmap.custom;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;

/**
 * Created by tali on 31.05.18.
 */

public class CustomListPreference extends ListPreference {
        public CustomListPreference(Context context) { super(context); }
        public CustomListPreference(Context context, AttributeSet attrs) { super(context, attrs); }
        @Override
        public void setValue(String value) {
            super.setValue(value);
            setSummary(getEntry());
        }
}
