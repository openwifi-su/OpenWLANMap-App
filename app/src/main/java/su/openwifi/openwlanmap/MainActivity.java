package su.openwifi.openwlanmap;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Observable;
import java.util.Observer;

import su.openwifi.openwlanmap.service.GPSLocator;

import static android.support.v4.view.GravityCompat.START;
import static su.openwifi.openwlanmap.Utils.MAILING_LIST;
import static su.openwifi.openwlanmap.Utils.REQUEST_GPS;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, Observer {
    private AccessPointAdapter adapter;
    private ListView listView;
    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private ArrayList<AccessPoint> listAP;
    private SharedPreferences sharedPreferences;
    TextView gps;
    TextView scanningStatus;
    TextView totalAP;
    TextView newestScan;
    private double testlong;
    private double testLat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_with_drawer_layout);

        //get all summary view
        gps = findViewById(R.id.gps);
        scanningStatus = findViewById(R.id.scan_status);
        totalAP = findViewById(R.id.ap_total);
        newestScan = findViewById(R.id.newest_scan);

        //set up sharepreference
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        //set up drawer nav and toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout,
                toolbar,
                R.string.open,
                R.string.close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
        NavigationView navView = findViewById(R.id.nav_layout);
        navView.setNavigationItemSelectedListener(this);


        //list of demo ap
        listView = findViewById(R.id.list);
        listAP = new ArrayList<>();
        listAP.add(new AccessPoint("A1:B2:cd:12:23:45", "foolan really logn name what now", -10, System.currentTimeMillis() + 3000, 2.4));
        listAP.add(new AccessPoint("A1:B2:cd:12:23:45", "foolan", -20, System.currentTimeMillis() + 10000, 2.5));
        listAP.add(new AccessPoint("A1:B2:cd:12:23:45", "foolan", -70, System.currentTimeMillis() + 1000, 5.0));
        listAP.add(new AccessPoint("A1:B2:cd:12:23:45", "foolan", -80, System.currentTimeMillis() + 20000, 2.9));
        listAP.add(new AccessPoint("A1:B2:cd:12:23:45", "foolan", -40, System.currentTimeMillis() + 30000, 2.6));
        listAP.add(new AccessPoint("A1:B2:cd:12:23:45", "foolan", -30, System.currentTimeMillis(), 2.3));
        listAP.add(new AccessPoint("A1:B2:cd:12:23:45", "foolan", -20, System.currentTimeMillis(), 2.2));
        listAP.add(new AccessPoint("A1:B2:cd:12:23:45", "foolan", -24, System.currentTimeMillis(), 2.1));
        listAP.add(new AccessPoint("A1:B2:cd:12:23:45", "foolan", -29, System.currentTimeMillis(), 5.0));

        adapter = new AccessPointAdapter(this, listAP);
        listView.setAdapter(adapter);


    }

    @Override
    protected void onStart() {
        super.onStart();
        Utils.checkPermission(this);
        Utils.checkGPS(this);
        //test gps
        GPSLocator SimpleGPSLocator = new SimpleGPSLocator(this);
    }

    public class SimpleGPSLocator extends GPSLocator{

        public SimpleGPSLocator(Context context) {
            super(context);
        }

        @Override
        protected void getGeo(double lat, double lon) {
            testLat=lat;
            testlong=lon;
            gps.setText(lat+"-"+lon);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_GPS:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, getString(R.string.gps_pop_up_ok), Toast.LENGTH_SHORT).show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(1000);
                                    Utils.checkGPS(MainActivity.this);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    } else {
                        Toast.makeText(this, getString(R.string.gps_pop_up), Toast.LENGTH_SHORT).show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(1000);
                                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                                    startActivity(intent);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }.start();
                    }
                }
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.scan_service:
                Toast.makeText(this, "Start scanning", Toast.LENGTH_SHORT).show();
                break;
            case R.id.gps_map:
                //go to gps map
                break;
            case R.id.sort_signal:
                Collections.sort(listAP, new Comparator<AccessPoint>() {
                    @Override
                    public int compare(AccessPoint accessPoint, AccessPoint t1) {
                        if (accessPoint.getRssid() > t1.getRssid()) {
                            return -1;
                        } else if (accessPoint.getRssid() < t1.getRssid()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
                adapter.notifyDataSetChanged();
                break;
            case R.id.sort_frequency:
                Collections.sort(listAP, new Comparator<AccessPoint>() {
                    @Override
                    public int compare(AccessPoint accessPoint, AccessPoint t1) {
                        if (accessPoint.getFrequency() > t1.getFrequency()) {
                            return -1;
                        } else if (accessPoint.getFrequency() < t1.getFrequency()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
                adapter.notifyDataSetChanged();
                break;
            case R.id.sort_timestamp:
                Collections.sort(listAP, new Comparator<AccessPoint>() {
                    @Override
                    public int compare(AccessPoint accessPoint, AccessPoint t1) {
                        if (accessPoint.getTimestamp() > t1.getTimestamp()) {
                            return -1;
                        } else if (accessPoint.getTimestamp() < t1.getTimestamp()) {
                            return 1;
                        } else {
                            return 0;
                        }
                    }
                });
                adapter.notifyDataSetChanged();
                break;
            case R.id.upload:
                Toast.makeText(this, "Uploading", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
        return true;
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                //settings page
                startActivity(new Intent(this, SettingActivity.class));
                break;
            case R.id.ranking:
                //ranking page --> fetch from backend
                startActivity(new Intent(this, RankingActivity.class));
                break;
            case R.id.map:
                startActivity(new Intent(this, OpenWifiMapActivity.class));
                break;
            case R.id.news:
                //announcement page --> fetch from backend
                startActivity(new Intent(this, NewsActivity.class));
                break;
            case R.id.contribute:
                //send feedback per email
                Intent sendEmail = new Intent(Intent.ACTION_SENDTO);
                sendEmail.setType("*/*");
                sendEmail.setData(Uri.parse("mailto:" + MAILING_LIST));
                if (sendEmail.resolveActivity(getPackageManager()) != null) {
                    startActivity(sendEmail);
                }
                break;
            case R.id.share:
                //link to app store
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName())));
                break;
            default:
                break;
        }
        drawerLayout.closeDrawer(START);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void update(Observable observable, Object o) {
        gps.setText(o.toString());
    }
}
