package su.openwifi.openwlanmap;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.logging.Logger;

import static su.openwifi.openwlanmap.MainActivity.ACCESS_POINT;
import static su.openwifi.openwlanmap.Utils.SIMPLE_DATE_FORMAT;

public class AccessPointActivity extends AppCompatActivity {
    private static final Logger LOG = Logger.getLogger(AccessPointActivity.class.getName());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_access_point);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //get intent
        Intent intent = getIntent();
        AccessPoint ap = intent.getExtras().getParcelable(ACCESS_POINT);
        LOG.info("ap" + ap.getRssid());

        //get view
        TextView bssid = findViewById(R.id.ap_bssid);
        TextView ssid = findViewById(R.id.ap_ssid);
        TextView rssid = findViewById(R.id.ap_rssid);
        TextView timestamp = findViewById(R.id.ap_timestamp);
        TextView freq = findViewById(R.id.ap_frequency);
        TextView channel = findViewById(R.id.ap_channel);
        TextView capabilities = findViewById(R.id.ap_capabilities);
        TextView lat = findViewById(R.id.ap_latitude);
        TextView lon = findViewById(R.id.ap_longitude);

        //set view
        setIfAvailable(bssid, ap.getBssid());
        setIfAvailable(ssid, ap.getSsid());
        if (ap.getRssid() != 0) {
            rssid.setText(ap.getRssid() + " dB");
        } else {
            rssid.setText("?");
        }
        setIfAvailable(timestamp, SIMPLE_DATE_FORMAT.format(ap.getTimestamp()));
        if (ap.getFrequency() > 0) {
            freq.setText(ap.getFrequency() + " GHz");
        } else {
            freq.setText("?");
        }
        if (ap.getChannel() != 0) {
            channel.setText(String.valueOf(ap.getChannel()));
        } else {
            channel.setText("?");
        }
        setIfAvailable(capabilities, ap.getCapabilities());
        if (ap.getLat() > 0) {
            lat.setText(String.valueOf(ap.getLat()));
        } else {
            lat.setText("?");
        }
        if (ap.getLon() > 0) {
            lon.setText(String.valueOf(ap.getLon()));
        } else {
            lon.setText("?");
        }
    }

    private void setIfAvailable(TextView textView, String content) {
        if (!TextUtils.isEmpty(content)) {
            textView.setText(content);
        } else {
            textView.setText("?");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }
}
