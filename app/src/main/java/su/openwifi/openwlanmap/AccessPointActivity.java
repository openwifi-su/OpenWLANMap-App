package su.openwifi.openwlanmap;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class AccessPointActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_point);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
}
