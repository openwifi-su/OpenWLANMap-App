package su.openwifi.openwlanmap;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import su.openwifi.openwlanmap.service.ScanService;

import static android.support.v4.view.GravityCompat.START;
import static su.openwifi.openwlanmap.Utils.MAILING_LIST;
import static su.openwifi.openwlanmap.Utils.REQUEST_GPS;
import static su.openwifi.openwlanmap.Utils.REQUEST_WRITE;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    public static final String ACCESS_POINT = "access_point";
    private static final String PREF_SORT_METHOD = "sort_method";
    private static final String SORT_BY_TIME = "sort_by_time";
    private static final String SORT_BY_RSSID = "sort_by_rssid";
    private static final String SORT_BY_FREQ = "sort_by_freq";
    public static final String R_GEO_INFO = "geo_info";
    public static final String R_NEWEST_SCAN = "newest_scan";
    public static final String R_LIST_AP = "location_info_object";
    public static final String R_UPDATE_UI = "update_ui";
    public static final String R_UPDATE_ERROR = "update_error";
    public static final String R_UPDATE_LAST = "update_last";
    public static final String R_TOTAL_LIST = "total_list";
    private static final String S_SCANN_STATUS = "scan_status";
    private static final String PREF_TOTAL_AP = "p_total_ap";
    private AccessPointAdapter adapter;
    private DrawerLayout drawerLayout;
    private List<AccessPoint> listAP;
    private SharedPreferences sharedPreferences;
    TextView gps;
    TextView scanningStatus;
    TextView totalAP;
    TextView newestScan;
    TextView speed;
    TextView rank;
    LinearLayout gpsField;
    LinearLayout speedField;
    LinearLayout rankField;
    private boolean scanning = false;
    private Intent intent;
    private long apInDB;
    private BroadcastReceiver serviceBroadcastReceiver = new ServiceBroadcastReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(LOG_TAG, "Creating main activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_with_drawer_layout);

        //get all summary view
        gps = findViewById(R.id.gps);
        scanningStatus = findViewById(R.id.scan_status);
        totalAP = findViewById(R.id.ap_total);
        newestScan = findViewById(R.id.newest_scan);
        speed = findViewById(R.id.speed);
        rank = findViewById(R.id.rank);
        gpsField = findViewById(R.id.gps_field);
        speedField = findViewById(R.id.speed_field);
        rankField = findViewById(R.id.rank_field);

        //set up sharepreference
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        if (!sharedPreferences.contains(PREF_SORT_METHOD)) {
            addPreference(PREF_SORT_METHOD, SORT_BY_TIME);
        }

        if (!sharedPreferences.contains(PREF_TOTAL_AP)) {
            addPreference(PREF_TOTAL_AP, 0);
        } else {
            apInDB = sharedPreferences.getLong(PREF_TOTAL_AP, 0);
            totalAP.setText(String.valueOf(apInDB));
        }

        //set up drawer nav and toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
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
        ListView listView = findViewById(R.id.list);
        listView.setEmptyView(findViewById(R.id.no_api));
        listAP = new ArrayList<>();

        adapter = new AccessPointAdapter(this, listAP);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intentItem = new Intent(MainActivity.this, AccessPointActivity.class);
                intentItem.putExtra(ACCESS_POINT, listAP.get(i));
                startActivity(intentItem);
            }
        });

        intent = new Intent(this, ScanService.class);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (savedInstanceState == null || savedInstanceState.getBoolean(S_SCANN_STATUS)) {
                scanning = true;
                startService(intent);
                scanningStatus.setText(getString(R.string.scanning));
            } else {
                scanning = false;
                Log.e(LOG_TAG, "Service currently stopped + Scan status = " + scanning);
                scanningStatus.setText(getString(R.string.stopped));
                listAP.clear();
                adapter.notifyDataSetChanged();
            }
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.e(LOG_TAG, "Saving scanning status =" + scanning);
        outState.putBoolean(S_SCANN_STATUS, scanning);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        Log.i(LOG_TAG, "Unregister receiver on Stop");
        super.onStop();
        unregisterReceiver(serviceBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        Log.i(LOG_TAG, "on Destroy");
        super.onDestroy();
    }

    private void doRegister() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(R_UPDATE_UI);
        intentFilter.addAction(R_UPDATE_ERROR);
        //Only for demo to show save aps
        intentFilter.addAction(R_UPDATE_LAST);
        registerReceiver(serviceBroadcastReceiver, intentFilter);
    }


    @Override
    protected void onStart() {
        Log.e(LOG_TAG, "Register receiver on start. Scan status = " + scanning);
        doRegister();
        super.onStart();
        Utils.checkPermission(this);
        Utils.checkGPS(this);
        final Set<String> summarySet = sharedPreferences.getStringSet("pref_show_summary", null);
        rankField.setVisibility(View.GONE);
        gpsField.setVisibility(View.GONE);
        speedField.setVisibility(View.GONE);
        for (String item : summarySet) {
            if (item.equalsIgnoreCase(getString(R.string.rank_sum))) {
                rankField.setVisibility(View.VISIBLE);
            } else if (item.equalsIgnoreCase(getString(R.string.gps_sum))) {
                gpsField.setVisibility(View.VISIBLE);
            } else if (item.equalsIgnoreCase(getString(R.string.speed_sum))) {
                speedField.setVisibility(View.VISIBLE);
            }
        }
    }

    private void addPreference(String key, String sortMethod) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, sortMethod);
        editor.commit();
    }

    private void addPreference(String key, long totalAP) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(key, totalAP);
        editor.commit();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_GPS:
                if (grantResults.length > 0) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        doRegister();
                        startService(intent);
                        Toast.makeText(this, getString(R.string.gps_pop_up_ok), Toast.LENGTH_SHORT).show();
                        new Thread() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(1000);
                                    Utils.checkGPS(MainActivity.this);
                                } catch (InterruptedException e) {
                                    Log.e(LOG_TAG, "Fail scaling toast time");
                                    Thread.currentThread().interrupt();
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
                                    Intent intentPermission = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()));
                                    startActivity(intentPermission);
                                } catch (Exception e) {
                                    Log.e(LOG_TAG, "Fail scaling toast time");
                                    Thread.currentThread().interrupt();
                                }
                            }
                        }.start();
                    }
                }
                break;
            case REQUEST_WRITE:
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
                String toast;
                if (scanning) {
                    toast = getString(R.string.stopped);
                    killService();
                    scanning = false;
                } else {
                    toast = getString(R.string.scanning);
                    startService(intent);
                    scanning = true;
                    apInDB = sharedPreferences.getLong(PREF_TOTAL_AP,0);
                }
                scanningStatus.setText(toast);
                break;
            case R.id.gps_map:
                //go to gps map
                startActivity(new Intent(MainActivity.this, MapActivity.class));
                break;
            case R.id.sort_signal:
                addPreference(PREF_SORT_METHOD, SORT_BY_RSSID);
                sortBySignal();
                adapter.notifyDataSetChanged();
                break;
            case R.id.sort_frequency:
                addPreference(PREF_SORT_METHOD, SORT_BY_FREQ);
                sortByFreq();
                adapter.notifyDataSetChanged();
                break;
            case R.id.sort_timestamp:
                addPreference(PREF_SORT_METHOD, SORT_BY_TIME);
                sortByTimestamp();
                adapter.notifyDataSetChanged();
                break;
            case R.id.upload:
                Toast.makeText(this, "Demo uploading. Reset APs total in DB to 0", Toast.LENGTH_SHORT).show();
                addPreference(PREF_TOTAL_AP, 0);
                totalAP.setText("0");
                apInDB = 0;
                break;
            default:
                break;
        }
        return true;
    }

    private void killService() {
        stopService(intent);
        listAP.clear();
        adapter.notifyDataSetChanged();
        newestScan.setText("0");
    }

    private void sortByTimestamp() {
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
    }

    private void sortByFreq() {
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
    }

    private void sortBySignal() {
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
            showAlertExist();
        }
    }

    private void showAlertExist() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.exist));
        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                if (scanning) {
                    killService();
                }
                finish();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public class ServiceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG, "Receiving smt from service");
            switch (intent.getAction()){
                case R_UPDATE_UI:
                    String gpsString = intent.getStringExtra(R_GEO_INFO);
                    long newest = intent.getLongExtra(R_NEWEST_SCAN, 0);
                    long total = intent.getLongExtra(R_TOTAL_LIST, 0);
                    List<AccessPoint> list = intent.getParcelableArrayListExtra(R_LIST_AP);
                    String sortMethod = sharedPreferences.getString(PREF_SORT_METHOD, SORT_BY_TIME);
                    listAP.clear();
                    if (list != null && !list.isEmpty()) {
                        listAP.addAll(list);
                        switch (sortMethod) {
                            case SORT_BY_FREQ:
                                sortByFreq();
                                break;
                            case SORT_BY_RSSID:
                                sortBySignal();
                                break;
                            case SORT_BY_TIME:
                                sortByTimestamp();
                                break;
                            default:
                                break;
                        }
                    }
                    adapter.notifyDataSetChanged();
                    gps.setText(gpsString);
                    newestScan.setText(String.valueOf(newest));
                    totalAP.setText(String.valueOf(total + apInDB));
                    addPreference(PREF_TOTAL_AP, total + apInDB);
                    break;
                case R_UPDATE_ERROR:
                    gps.setText(getString(R.string.c_gps));
                    newestScan.setText("0");
                    break;
                case R_UPDATE_LAST:
                    long totalLast = intent.getLongExtra(R_TOTAL_LIST, 0);
                    addPreference(PREF_TOTAL_AP, totalLast + apInDB);
                    break;
                default:
                    break;
            }
        }
    }
}
