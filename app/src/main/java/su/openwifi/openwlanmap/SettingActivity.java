package su.openwifi.openwlanmap;

import static su.openwifi.openwlanmap.MainActivity.PREF_OWN_BSSID;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import su.openwifi.openwlanmap.service.ResourceManager;
import su.openwifi.openwlanmap.service.ServiceController;

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
    final boolean pref_use_map = PreferenceManager
        .getDefaultSharedPreferences(SettingActivity.this).getBoolean("pref_use_map", false);
    final int pref_upload = Integer.parseInt(PreferenceManager
        .getDefaultSharedPreferences(this).getString("pref_upload_mode", "0"));
    hideIfAnonym(pref_privacy);
    hideIfUsingMap(pref_use_map);
    hideIfManual(pref_upload);
    if (sharedPreferenceChangeListener == null) {
      sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
          if (key.equalsIgnoreCase("pref_privacy")) {
            hideIfAnonym(sharedPreferences.getBoolean(key, false));
          } else if (key.equalsIgnoreCase("pref_in_team")) {
            final Preference pref_team = fragment.findPreference("pref_team");
            hideIfNotInTeam(sharedPreferences.getBoolean(key, false), pref_team);
          } else if (key.equalsIgnoreCase("pref_use_map")) {
            hideIfUsingMap(sharedPreferences.getBoolean(key, false));
          } else if (key.equalsIgnoreCase("pref_battery")) {
            final String pref_battery = sharedPreferences.getString(key, "");
            ResourceManager.allowBattery = Integer.parseInt(pref_battery);
          } else if (key.equalsIgnoreCase("pref_kill_ap_no_gps")) {
            final String pref_kill_ap_no_gps = sharedPreferences.getString(key, "");
            ResourceManager.allowNoLocation = Integer.parseInt(pref_kill_ap_no_gps);
          } else if (key.equalsIgnoreCase("pref_upload_mode")) {
            final int pref_upload = Integer.parseInt(sharedPreferences.getString(key, ""));
            hideIfManual(pref_upload);
          } else if(key.equalsIgnoreCase("pref_upload_entry")){
            ServiceController.numberOfApToUpload = Integer.parseInt(sharedPreferences.getString(key, "5000"));
          }
        }
      };
    }
  }

  private void hideIfManual(int prefUpload) {
    final ListPreference pref_upload_count = (ListPreference) fragment.findPreference("pref_upload_entry");
    final int pref_upload_entry = Integer.parseInt(PreferenceManager
        .getDefaultSharedPreferences(this).getString("pref_upload_entry", "5000"));
    if (prefUpload == 0) {
      pref_upload_count.setEnabled(false);
      ServiceController.numberOfApToUpload = -1;
    } else {
      pref_upload_count.setEnabled(true);
      ServiceController.numberOfApToUpload = pref_upload_entry;
    }
  }

  private void hideIfUsingMap(boolean useMap) {
    final Preference pref_show_list = fragment.findPreference("pref_show_list");
    if (useMap) {
      pref_show_list.setEnabled(false);
    } else {
      pref_show_list.setEnabled(true);
    }
  }

  private void hideIfAnonym(boolean isAnonym) {
    final Preference pref_team = fragment.findPreference("pref_team");
    final Preference pref_team_tag = fragment.findPreference("pref_team_tag");
    final Preference pref_in_team = fragment.findPreference("pref_in_team");
    if (isAnonym) {
      pref_team.setEnabled(false);
      pref_team_tag.setEnabled(false);
      pref_in_team.setEnabled(false);
    } else {
      pref_team_tag.setEnabled(true);
      pref_in_team.setEnabled(true);
      final boolean pref_in_team_status = PreferenceManager
          .getDefaultSharedPreferences(this).getBoolean("pref_in_team", false);
      hideIfNotInTeam(pref_in_team_status, pref_team);
    }
  }

  private void hideIfNotInTeam(boolean prefInTeamStatus, Preference prefTeam) {
    if (prefInTeamStatus) {
      prefTeam.setEnabled(true);
    } else {
      prefTeam.setEnabled(false);
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