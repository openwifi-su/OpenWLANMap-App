package su.openwifi.openwlanmap.custom;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Created by tali on 31.05.18.
 */

public class CustomEditTextPreferenceScanMode extends EditTextPreference {
        public CustomEditTextPreferenceScanMode(Context context) { super(context);}
        public CustomEditTextPreferenceScanMode(Context context, AttributeSet attrs) { super(context, attrs); }

        @Override
        public CharSequence getSummary() {
            return "Every "+getText()+" s";
        }
}
