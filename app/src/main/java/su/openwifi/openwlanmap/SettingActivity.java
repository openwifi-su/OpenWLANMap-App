package su.openwifi.openwlanmap;

import static su.openwifi.openwlanmap.MainActivity.PREF_OWN_BSSID;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class SettingActivity extends AppCompatActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_setting);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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
        builder.setMessage(getString(R.string.ownbssid) +"\n"
        + PreferenceManager.getDefaultSharedPreferences(this).getString(PREF_OWN_BSSID,getString(R.string.error_ownbssid)));
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
}