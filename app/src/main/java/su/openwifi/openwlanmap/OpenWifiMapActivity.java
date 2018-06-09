package su.openwifi.openwlanmap;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import static su.openwifi.openwlanmap.Utils.OPEN_WIFI_MAP_URL;

public class OpenWifiMapActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_wifi_map);
        WebView webView = findViewById(R.id.map_web_view);

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //Make it like a normal viewport as browser
        webSettings.setUseWideViewPort(true);
        webSettings.setSupportZoom(true);
        //open page inside of my Webview
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(OPEN_WIFI_MAP_URL);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        }
        return true;
    }
}
