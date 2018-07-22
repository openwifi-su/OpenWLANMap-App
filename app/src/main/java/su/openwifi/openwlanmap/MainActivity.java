package su.openwifi.openwlanmap;

import static android.support.v4.view.GravityCompat.START;
import static su.openwifi.openwlanmap.Utils.MAILING_LIST;
import static su.openwifi.openwlanmap.Utils.REQUEST_GPS;
import static su.openwifi.openwlanmap.Utils.REQUEST_WRITE;

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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import su.openwifi.openwlanmap.service.Config;
import su.openwifi.openwlanmap.service.ServiceController;

public class MainActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener {
  public static final String ACCESS_POINT = "access_point";
  public static final String R_GEO_INFO = "geo_info";
  public static final String R_NEWEST_SCAN = "newest_scan";
  public static final String R_LIST_AP = "location_info_object";
  public static final String R_UPDATE_UI = "update_ui";
  public static final String R_SPEED = "update_speed";
  public static final String R_UPDATE_ERROR = "update_error";
  public static final String R_TOTAL_LIST = "total_list";
  public static final String R_UPLOAD_ERROR = "upload_error_msg";
  public static final String R_UPLOAD_MSG = "upload_msg";
  public static final String R_UPDATE_DB = "database_status";
  public static final String R_UPDATE_RANKING = "update_ranking";
  public static final String R_RANK = "rank_position";
  public static final String R_UPLOAD_UNDER_LIMIT = "update_under_limit";
  private static final String LOG_TAG = MainActivity.class.getSimpleName();
  private static final String PREF_SORT_METHOD = "sort_method";
  private static final String SORT_BY_TIME = "sort_by_time";
  private static final String SORT_BY_RSSID = "sort_by_rssid";
  private static final String SORT_BY_FREQ = "sort_by_freq";
  private static final String S_SCANN_STATUS = "scan_status";
  private static final String PREF_TOTAL_AP = "p_total_ap";
  public static final String PREF_RANKING = "p_ranking";
  public static final String PREF_OWN_BSSID = "own_bssid";
  private TextView gps;
  private TextView scanningStatus;
  private TextView totalAp;
  private TextView newestScan;
  private TextView speed;
  private TextView rank;
  private LinearLayout gpsField;
  private LinearLayout speedField;
  private LinearLayout rankField;
  private ListView listView;
  private TextView listHeader;
  private ProgressBar loading;
  private AccessPointAdapter adapter;
  private DrawerLayout drawerLayout;
  private List<AccessPoint> listAp;
  private SharedPreferences sharedPreferences;
  private boolean scanning = false;
  private Intent intent;
  private String rankingInfo;
  private BroadcastReceiver serviceBroadcastReceiver = new ServiceBroadcastReceiver();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.i(LOG_TAG, "Creating main activity");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_with_drawer_layout);

    //get all summary view
    gps = findViewById(R.id.gps);
    scanningStatus = findViewById(R.id.scan_status);
    totalAp = findViewById(R.id.ap_total);
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
      long apInDb = sharedPreferences.getLong(PREF_TOTAL_AP, 0);
      totalAp.setText(String.valueOf(apInDb));
    }

    if (!sharedPreferences.contains(PREF_RANKING)) {
      addPreference(PREF_RANKING, "unknown");
    } else {
      rankingInfo = sharedPreferences.getString(PREF_RANKING, "unknown");
      rank.setText(rankingInfo);
    }

    if (!sharedPreferences.contains(PREF_OWN_BSSID)) {
      addPreference(PREF_OWN_BSSID, generateOwnBssid());
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
    listHeader = findViewById(R.id.ap_list_h);
    loading = findViewById(R.id.loading);
    TextView emptyView = findViewById(R.id.no_api);
    listView = findViewById(R.id.list);
    listView.setEmptyView(emptyView);
    listAp = new ArrayList<>();

    adapter = new AccessPointAdapter(this, listAp);
    listView.setAdapter(adapter);

    listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intentItem = new Intent(MainActivity.this, AccessPointActivity.class);
        intentItem.putExtra(ACCESS_POINT, listAp.get(i));
        startActivity(intentItem);
      }
    });

    intent = new Intent(this, ServiceController.class);
    if (ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
      if (savedInstanceState == null || savedInstanceState.getBoolean(S_SCANN_STATUS)) {
        scanning = true;
        startService(intent);
        scanningStatus.setText(getString(R.string.scanning));
      } else {
        scanning = false;
        Log.e(LOG_TAG, "Service currently stopped + Scan status = " + scanning);
        scanningStatus.setText(getString(R.string.stopped));
        listAp.clear();
        adapter.notifyDataSetChanged();
      }
    }
  }

  private String generateOwnBssid() {
    String ownBssid;
    do {
      SecureRandom sr = new SecureRandom();
      byte[] output = new byte[6];
      sr.nextBytes(output);
      ownBssid = String.format("%2X%2X%2X%2X%2X%2X",
          output[0], output[1], output[2], output[3], output[4], output[5]);
      ownBssid = ownBssid.replace(" ", "");
    } while (ownBssid.length() < 12);
    return ownBssid;
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
    //Upload under limit
    intentFilter.addAction(R_UPLOAD_UNDER_LIMIT);
    //Upload status
    intentFilter.addAction(R_UPLOAD_ERROR);
    //Database status
    intentFilter.addAction(R_UPDATE_DB);
    //Ranking
    intentFilter.addAction(R_UPDATE_RANKING);
    registerReceiver(serviceBroadcastReceiver, intentFilter);
  }


  @Override
  protected void onStart() {
    Log.e(LOG_TAG, "Register receiver on start. Scan status = " + scanning);
    doRegister();
    super.onStart();
    Utils.checkLocationPermission(this);
    Utils.checkGpsSignal(this);
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

  private void addPreference(String key, String info) {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putString(key, info);
    editor.commit();
  }

  private void addPreference(String key, long totalAp) {
    SharedPreferences.Editor editor = sharedPreferences.edit();
    editor.putLong(key, totalAp);
    editor.commit();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode,
                                         @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    switch (requestCode) {
      case REQUEST_GPS:
        if (grantResults.length > 0) {
          if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            doRegister();
            startService(intent);
            Toast.makeText(this, getString(R.string.gps_pop_up_ok), Toast.LENGTH_SHORT).show();
            /*new Thread() {
              @Override
              public void run() {
                try {
                  Thread.sleep(1000);
                  Utils.checkGpsSignal(MainActivity.this);
                } catch (InterruptedException e) {
                  Log.e(LOG_TAG, "Fail scaling toast time");
                  Thread.currentThread().interrupt();
                }
              }
            }.start();
            */
          } else {
            Toast.makeText(this, getString(R.string.gps_pop_up), Toast.LENGTH_SHORT).show();
            new Thread() {
              @Override
              public void run() {
                try {
                  Thread.sleep(1000);
                  Intent intentPermission = new Intent(
                      Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                      Uri.parse("package:" + getPackageName()));
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
        showToastInCenter(getString(R.string.uploading));
        listView.setVisibility(View.GONE);
        listHeader.setVisibility(View.GONE);
        loading.setVisibility(View.VISIBLE);
        Config.setMode(Config.MODE.UPLOAD_MODE);
        break;
      default:
        break;
    }
    return true;
  }

  private void showToastInCenter(String msg) {
    Toast toastCenter = Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT);
    toastCenter.setGravity(Gravity.CENTER, 0, 0);
    toastCenter.show();
  }

  private void killService() {
    stopService(intent);
    listAp.clear();
    adapter.notifyDataSetChanged();
    newestScan.setText("0");
  }

  private void sortByTimestamp() {
    Collections.sort(listAp, new Comparator<AccessPoint>() {
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
    Collections.sort(listAp, new Comparator<AccessPoint>() {
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
    Collections.sort(listAp, new Comparator<AccessPoint>() {
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
        startActivity(new Intent(
            Intent.ACTION_VIEW,
            Uri.parse("market://details?id=" + getPackageName())));
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

  private void showAlertUpload(String msg) {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

  public class ServiceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      Log.i(LOG_TAG, "Receiving smt from service");
      switch (intent.getAction()) {
        case R_UPDATE_UI:
          if (loading.getVisibility() == View.VISIBLE) {
            listHeader.setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
          } else {
            final String gpsString = intent.getStringExtra(R_GEO_INFO);
            final long newest = intent.getLongExtra(R_NEWEST_SCAN, 0);
            final float speedUpdate = intent.getFloatExtra(R_SPEED, 0f);
            List<AccessPoint> list = intent.getParcelableArrayListExtra(R_LIST_AP);
            String sortMethod = sharedPreferences.getString(PREF_SORT_METHOD, SORT_BY_TIME);
            listAp.clear();
            if (list != null && !list.isEmpty()) {
              listAp.addAll(list);
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
            if(speedUpdate > 0){
              speed.setText(speedUpdate+" m/s");
            }else{
              speed.setText("?");
            }
          }
          break;
        case R_UPDATE_DB:
          long total = intent.getLongExtra(R_TOTAL_LIST, 0L);
          totalAp.setText(String.valueOf(total));
          addPreference(PREF_TOTAL_AP, total);
          break;
        case R_UPDATE_ERROR:
          gps.setText(getString(R.string.c_gps));
          newestScan.setText("0");
          break;
        case R_UPLOAD_UNDER_LIMIT:
          showAlertUpload(getString(R.string.upload_under_limit));
          resetListVisibility();
          break;
        case R_UPLOAD_ERROR:
          String msg = intent.getStringExtra(R_UPLOAD_MSG);
          showAlertUpload(msg);
          resetListVisibility();
          break;
        case R_UPDATE_RANKING:
          QueryUtils.RankingObject r = intent.getParcelableExtra(R_RANK);
          String rp = r.uploadedRank
              + "(" + r.uploadedCount + " "
              + getString(R.string.point) + ")";
          showAlertUpload(getString(R.string.uploadOK)
              + "\n" + getString(R.string.upCount) + r.uploadedCount
              + "\n" + getString(R.string.upRank) + r.uploadedRank
              + "\n" + getString(R.string.upNewAp) + r.newAps
              + "\n" + getString(R.string.upUpdAp) + r.updAps
              + "\n" + getString(R.string.upDelAp) + r.delAps
              + "\n" + getString(R.string.upNewPoint) + r.newPoints);
          rank.setText(rp);
          addPreference(PREF_RANKING, rp);
          resetListVisibility();
          break;
        default:
          break;
      }
    }
  }

  private void resetListVisibility() {
    loading.setVisibility(View.GONE);
    listView.setVisibility(View.VISIBLE);
    listHeader.setVisibility(View.VISIBLE);
  }
}
