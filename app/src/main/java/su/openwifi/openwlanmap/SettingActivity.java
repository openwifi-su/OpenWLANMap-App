package su.openwifi.openwlanmap;

import static su.openwifi.openwlanmap.MainActivity.PREF_OWN_BSSID;
import static su.openwifi.openwlanmap.MainActivity.PREF_RANKING;
import static su.openwifi.openwlanmap.MainActivity.PREF_SORT_METHOD;
import static su.openwifi.openwlanmap.MainActivity.PREF_TOTAL_AP;
import static su.openwifi.openwlanmap.MainActivity.SORT_BY_TIME;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import su.openwifi.openwlanmap.service.ResourceManager;
import su.openwifi.openwlanmap.service.ServiceController;

public class SettingActivity extends AppCompatActivity {
  private SharedPreferences.OnSharedPreferenceChangeListener sharedPreferenceChangeListener;
  private PreferenceFragment fragment;
  private SharedPreferences sharedP;
  private static final int REQUEST_SEARCH_FILE = 103;
  private static final int REQUEST_CREATE_FILE = 104;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    //Do not use savedInstanceState to avoid XML InflateException while rotating
    super.onCreate(null);
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
    sharedP = PreferenceManager
        .getDefaultSharedPreferences(SettingActivity.this);
    final boolean pref_use_map = sharedP.getBoolean("pref_use_map", false);
    final int pref_upload = Integer.parseInt(sharedP.getString("pref_upload_mode", "0"));
    final boolean pref_in_team = sharedP.getBoolean("pref_in_team", false);
    hideIfNotInTeam(pref_in_team);
    hideIfUsingMap(pref_use_map);
    hideIfManual(pref_upload);
    if (sharedPreferenceChangeListener == null) {
      sharedPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
          switch (key.toLowerCase()) {
            case "pref_in_team":
              hideIfNotInTeam(sharedPreferences.getBoolean(key, false));
              break;
            case "pref_use_map":
              hideIfUsingMap(sharedPreferences.getBoolean(key, false));
              break;
            case "pref_battery":
              final String pref_battery = sharedPreferences.getString(key, "");
              ServiceController.allowBattery = Integer.parseInt(pref_battery);
              if ((ServiceController.allowBattery != 0)
                  && ((ServiceController.resourceManager == null)
                  || (!ServiceController.resourceManager.isAlive()))) {
                Log.e("SETT", "restart resource manger bat=" + ServiceController.allowBattery);
                ServiceController.resourceManager = new ResourceManager(SettingActivity.this);
                ServiceController.resourceManager.start();
              }
              break;
            case "pref_kill_ap_no_gps":
              final String pref_kill_ap_no_gps = sharedPreferences.getString(key, "");
              ServiceController.allowNoLocation = Integer.parseInt(pref_kill_ap_no_gps);
              if ((ServiceController.allowNoLocation != 0)
                  && ((ServiceController.resourceManager == null)
                  || (!ServiceController.resourceManager.isAlive()))) {
                Log.e("SETT", "restart resource manger=" + ServiceController.allowNoLocation);
                ServiceController.resourceManager = new ResourceManager(SettingActivity.this);
                ServiceController.resourceManager.start();
              }
              break;
            case "pref_upload_mode":
              final int pref_upload = Integer.parseInt(sharedPreferences.getString(key, ""));
              hideIfManual(pref_upload);
              break;
            case "pref_upload_entry":
              ServiceController.numberOfApToUpload =
                  Integer.parseInt(
                      sharedPreferences.getString(key, "5000"));
              break;
            case "pref_show_counter":
              ServiceController.showCounterWrapper
                  .setShouldShow(sharedPreferences.getBoolean(key, false));
              break;
            case "pref_team":
              ServiceController.teamId = sharedPreferences.getString(key, "");
              if (!Utils.checkBssid(ServiceController.teamId)) {
                showAlert(getString(R.string.wrong_id_format));
              }
              break;
            case "pref_team_tag":
              ServiceController.tag = sharedPreferences.getString(key, "");
              break;
            case "pref_public_data":
              if (sharedPreferences.getBoolean(key, true)) {
                ServiceController.mode |= 1;
              } else {
                ServiceController.mode &= 2;
              }
              break;
            case "pref_publish_map":
              if (sharedPreferences.getBoolean(key, false)) {
                ServiceController.mode |= 2;
              } else {
                ServiceController.mode &= 1;
              }
              break;
            default:
              break;
          }
        }
      };
    }
  }

  private void showAlert(String msg) {
    AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
    builder.setMessage(msg);
    builder.setPositiveButton(R.string.closeDialog, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.dismiss();
      }
    });
    // Create and show the AlertDialog
    AlertDialog alertDialog = builder.create();
    alertDialog.show();
  }

  private void hideIfManual(int prefUpload) {
    final ListPreference pref_upload_count =
        (ListPreference) fragment.findPreference("pref_upload_entry");
    final int pref_upload_entry = Integer.parseInt(sharedP.getString("pref_upload_entry", "5000"));
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

  private void hideIfNotInTeam(boolean prefInTeamStatus) {
    final Preference pref_team = fragment.findPreference("pref_team");
    if (prefInTeamStatus) {
      pref_team.setEnabled(true);
      ServiceController.teamId = sharedP.getString("pref_team", "");
    } else {
      pref_team.setEnabled(false);
      ServiceController.teamId = "";
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
        //save programmatical prefs
        final String ownBssid = sharedP.getString(PREF_OWN_BSSID, "");
        final String ranking = sharedP.getString(PREF_RANKING, "");
        final long totalAps = sharedP.getLong(PREF_TOTAL_AP, 0L);
        final String sortMethod = sharedP.getString(PREF_SORT_METHOD, SORT_BY_TIME);
        //reset pref
        sharedP.edit().clear().commit();
        //reset programmatical prefs
        Utils.addPreference(sharedP, PREF_OWN_BSSID, ownBssid);
        Utils.addPreference(sharedP, PREF_RANKING, ranking);
        Utils.addPreferenceLong(sharedP, PREF_TOTAL_AP, totalAps);
        Utils.addPreference(sharedP, PREF_SORT_METHOD, sortMethod);
        ServiceController.allowNoLocation = 0;
        ServiceController.allowBattery = 0;
        ServiceController.showCounterWrapper.setShouldShow(false);
        ServiceController.mode = 0;
        ServiceController.tag = "";
        Toast.makeText(this, getString(R.string.reset_alert), Toast.LENGTH_SHORT).show();
        finish();
        startActivity(getIntent());
        break;
      case R.id.setting_export:
        Intent intentExport = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intentExport.setType("*/*");
        intentExport.putExtra(Intent.EXTRA_TITLE, "OWMAP_V2.export");
        startActivityForResult(intentExport, REQUEST_CREATE_FILE);
        break;
      case R.id.setting_import:
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        //require getType, otherwise ActivityNotFoundException
        intent.setType("*/*");
        startActivityForResult(intent, REQUEST_SEARCH_FILE);
        break;
      case R.id.setting_ownbssid:
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.ownbssid) + "\n"
            + sharedP.getString(PREF_OWN_BSSID, getString(R.string.error_ownbssid)));
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

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (requestCode) {
      case REQUEST_SEARCH_FILE:
        if (resultCode == Activity.RESULT_OK) {
          if (data != null) {
            Uri uri = data.getData();
            try {
              InputStream ins = getContentResolver().openInputStream(uri);
              DataInputStream dataInputStream = new DataInputStream(ins);
              final int version = dataInputStream.readByte();
              if (version == 1) {
                //no need scan flag and stored values
                dataInputStream.readInt();
                dataInputStream.readInt();
                //read rank and point
                int points = dataInputStream.readInt();
                int rank = dataInputStream.readInt();
                //no need openwlan and free hotspot data
                dataInputStream.readInt();
                dataInputStream.readInt();
                //no need telemetry data
                int i = 0;
                while (i < 6) {
                  dataInputStream.readFloat();
                  i++;
                }
                //read own bssid
                final byte[] bytes = new byte[12];
                dataInputStream.read(bytes);
                ServiceController.ownId = new String(bytes);
                ServiceController.ranking = rank
                    + "(" + points + " "
                    + getString(R.string.point) + ")";
                Utils.addPreference(sharedP, PREF_RANKING, ServiceController.ranking);
                Utils.addPreference(sharedP, PREF_OWN_BSSID, ServiceController.ownId);
                showAlert(getString(R.string.import_ok));
              } else if (version == 2) {
                ServiceController.ownId = dataInputStream.readUTF();
                ServiceController.ranking = dataInputStream.readUTF();
                ServiceController.tag = dataInputStream.readUTF();
                final boolean inTeam = dataInputStream.readBoolean();
                if (inTeam) {
                  ServiceController.teamId = dataInputStream.readUTF();
                  Utils.addPreference(sharedP, "pref_team", ServiceController.teamId);
                }
                Utils.addPreference(sharedP, PREF_RANKING, ServiceController.ranking);
                Utils.addPreference(sharedP, PREF_OWN_BSSID, ServiceController.ownId);
                Utils.addPreference(sharedP, "pref_team_tag", ServiceController.tag);
                Utils.addPreferenceBoolean(sharedP, "pref_in_team", inTeam);
                AlertDialog.Builder builder = new AlertDialog.Builder(SettingActivity.this);
                builder.setMessage(getString(R.string.import_ok));
                builder.setPositiveButton(R.string.closeDialog,
                    new DialogInterface.OnClickListener() {
                      public void onClick(DialogInterface dialog, int id) {
                        finish();
                        startActivity(getIntent());
                        dialog.dismiss();
                      }
                    });
                // Create and show the AlertDialog
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
              }
            } catch (Exception e) {
              showAlert(getString(R.string.import_error) + " " + e.toString());
            }
          }
        }
        break;
      case REQUEST_CREATE_FILE:
        if (resultCode == Activity.RESULT_OK) {
          if (data != null) {
            ParcelFileDescriptor pfd = null;
            try {
              pfd = getContentResolver()
                  .openFileDescriptor(data.getData(), "w");
              FileOutputStream fileOutputStream =
                  new FileOutputStream(pfd.getFileDescriptor());
              DataOutputStream out = new DataOutputStream(fileOutputStream);
              out.writeByte(2);
              out.writeUTF(ServiceController.ownId);
              out.writeUTF(ServiceController.ranking);
              out.writeUTF(ServiceController.tag);
              final boolean pref_in_team = sharedP.getBoolean("pref_in_team", false);
              out.writeBoolean(pref_in_team);
              if (pref_in_team) {
                out.writeUTF(ServiceController.teamId);
              }
              out.close();
              fileOutputStream.close();
              pfd.close();
              showAlert(getString(R.string.export_ok));
            } catch (Exception e) {
              showAlert(getString(R.string.export_error) + " " + e.toString());
            }
          }
        }
        break;
      default:
        break;
    }
  }
}