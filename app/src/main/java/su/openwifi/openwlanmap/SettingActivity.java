package su.openwifi.openwlanmap;

import android.os.Bundle;
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
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return true;
    }
}