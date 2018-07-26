package su.openwifi.openwlanmap;

import static su.openwifi.openwlanmap.MainActivity.PREF_OWN_BSSID;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class SettingActivity extends AppCompatActivity {
  private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;
  private PreferenceFragment fragment;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_setting);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    //set up pref listener
    if (fragment == null) {
      fragment = new SettingFragment();
      getFragmentManager()
          .beginTransaction()
          .replace(android.R.id.content, fragment)
          .commit();
      getFragmentManager().executePendingTransactions();
    }
    final boolean pref_privacy = PreferenceManager
        .getDefaultSharedPreferences(this).getBoolean("pref_privacy", false);
    hideIfAnonym(pref_privacy);
    if (sharedPreferenceChangeListener == null) {
      sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
          if (key.equalsIgnoreCase("pref_privacy")) {
            hideIfAnonym(sharedPreferences.getBoolean(key, false));
          }
        }
      };
    }
  }

  private void hideIfAnonym(boolean isAnonym) {
    final Preference pref_team = fragment.findPreference("pref_team");
    final Preference pref_team_tag = fragment.findPreference("pref_team_tag");
    if (isAnonym) {
      pref_team.setEnabled(false);
      pref_team_tag.setEnabled(false);
    } else {
      pref_team.setEnabled(true);
      pref_team_tag.setEnabled(true);
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.setting_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.setting_reset:
        Toast.makeText(this, "Settings is reseted sucessfully", Toast.LENGTH_LONG).show();
        break;
      case R.id.setting_export:
        Toast.makeText(this, "Settings is exported sucessfully", Toast.LENGTH_LONG).show();
        break;
      case R.id.setting_import:
        Toast.makeText(this, "Settings is imported sucessfully", Toast.LENGTH_LONG).show();
        break;
      case R.id.setting_ownbssid:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.ownbssid) + "\n"
            + PreferenceManager.getDefaultSharedPreferences(this)
            .getString(PREF_OWN_BSSID, getString(R.string.error_ownbssid)));
        builder.setPositiveButton(R.string.closeDialog, new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int id) {
            dialog.dismiss();
          }
        });
        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        break;
      case android.R.id.home:
        finish();
        break;
      default:
        break;
    }
    return true;
  }

  @Override
  protected void onPause() {
    super.onPause();
    PreferenceManager.getDefaultSharedPreferences(this)
        .unregisterOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
  }

  @Override
  protected void onResume() {
    super.onResume();
    PreferenceManager.getDefaultSharedPreferences(this)
        .registerOnSharedPreferenceChangeListener(sharedPreferenceChangeListener);
  }
}