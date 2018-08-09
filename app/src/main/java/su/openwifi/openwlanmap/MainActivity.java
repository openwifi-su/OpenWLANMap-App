package su.openwifi.openwlanmap;

import static android.support.v4.view.GravityCompat.START;
import static su.openwifi.openwlanmap.Utils.MAILING_LIST;
import static su.openwifi.openwlanmap.Utils.REQUEST_GPS;
import static su.openwifi.openwlanmap.Utils.REQUEST_OVERLAY;
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
import android.support.v4.content.LocalBroadcastManager;
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
import su.openwifi.openwlanmap.utils.RankingObject;

public class MainActivity extends AppCompatActivity
    implements NavigationView.OnNavigationItemSelectedListener {
  public static final String ACCESS_POINT = "access_point";
  public static final String R_GEO_INFO = "geo_info";
  public static final String R_NEWEST_SCAN = "newest_scan";
  public static final String R_LIST_AP = "location_info_object";
  public static final String ACTION_UPDATE_UI = "update_ui";
  public static final String R_SPEED = "update_speed";
  public static final String ACTION_UPDATE_ERROR = "update_error";
  public static final String R_TOTAL_LIST = "total_list";
  public static final String ACTION_UPLOAD_ERROR = "upload_error_msg";
  public static final String R_UPLOAD_MSG = "upload_msg";
  public static final String ACTION_UPDATE_DB = "database_status";
  public static final String ACTION_UPDATE_RANKING = "update_ranking";
  public static final String R_RANK = "rank_position";
  public static final String ACTION_KILL_APP = "kill_app";
  public static final String ACTION_AUTO_RANK = "auto_rank";
  public static final String PREF_RANKING = "p_ranking";
  public static final String PREF_OWN_BSSID = "own_bssid";
  public static final String ACTION_ASK_PERMISSION = "ask_for_permission";
  public static final String R_PERMISSION = "permission_code";
  private static final String LOG_TAG = MainActivity.class.getSimpleName();
  public static final String PREF_SORT_METHOD = "sort_method";
  public static final String SORT_BY_TIME = "sort_by_time";
  public static final String SORT_BY_RSSID = "sort_by_rssid";
  public static final String SORT_BY_FREQ = "sort_by_freq";
  public static final String PREF_TOTAL_AP = "p_total_ap";
  private static final long MIN_UPLOAD_ALLOWED = 20;
  private TextView gps;
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
  private Intent intent;
  private BroadcastReceiver serviceBroadcastReceiver = new ServiceBroadcastReceiver();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Log.e(LOG_TAG, "Creating main activity");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_with_drawer_layout);

    //get all summary view
    gps = findViewById(R.id.gps);
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
      Utils.addPreference(sharedPreferences, PREF_SORT_METHOD, SORT_BY_TIME);
    }

    if (!sharedPreferences.contains(PREF_TOTAL_AP)) {
      Utils.addPreferenceLong(sharedPreferences, PREF_TOTAL_AP, 0);
    } else {
      totalAp.setText(String.valueOf(sharedPreferences.getLong(PREF_TOTAL_AP, 0L)));
    }

    if (!sharedPreferences.contains(PREF_RANKING)) {
      Utils.addPreference(sharedPreferences, PREF_RANKING, "");
    } else {
      rank.setText(sharedPreferences.getString(PREF_RANKING, ""));
    }

    if (!sharedPreferences.contains(PREF_OWN_BSSID)) {
      Utils.addPreference(sharedPreferences, PREF_OWN_BSSID, generateOwnBssid());
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

    if(intent == null){
      intent = new Intent(this, ServiceController.class);
    }
    startServiceIfNotRunningYet();
  }

  @Override
  protected void onStart() {
    Log.e(LOG_TAG, "Register receiver on start. Scan status = " + ServiceController.running);
    doRegister();
    super.onStart();
    Utils.checkLocationPermission(this);
    Utils.checkGpsSignal(this);
    Utils.checkDrawOverlayPermission(this);
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
    startServiceIfNotRunningYet();
  }

  @Override
  public void onBackPressed() {
    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
      drawerLayout.closeDrawer(GravityCompat.START);
    } else {
      showAlertExist();
    }
  }

  @Override
  protected void onStop() {
    Log.i(LOG_TAG, "Unregister receiver on Stop");
    super.onStop();
    LocalBroadcastManager.getInstance(this)
        .unregisterReceiver(serviceBroadcastReceiver);
  }

  @Override
  protected void onDestroy() {
    Log.i(LOG_TAG, "on Destroy");
    super.onDestroy();
  }


  private void startServiceIfNotRunningYet() {
    Log.e(LOG_TAG, "start service. Current running?=" + ServiceController.running);
    if(intent == null){
      intent = new Intent(this, ServiceController.class);
    }
    if (!ServiceController.running) {
      if (ActivityCompat.checkSelfPermission(
          this,
          Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        Config.setMode(Config.MODE.SCAN_MODE);
        startService(intent);
      }
    } else {
      // onStop, UI receives no update on ap count and rank from autoupload
      rank.setText(ServiceController.ranking);
      totalAp.setText(String.valueOf(ServiceController.totalApsCount));
      gps.setText(ServiceController.lastLat+"-"+ServiceController.lastLon);
      newestScan.setText(String.valueOf(ServiceController.newest));
      if (ServiceController.lastSpeed > 0) {
        speed.setText(String
            .format("%.2f m/s (%.2f km/h)", ServiceController.lastSpeed,
                ServiceController.lastSpeed * 60 * 60 / 1000.0));
      } else {
        speed.setText("?");
      }
    }
  }

  private String generateOwnBssid() {
    String ownBssid;
    do {
      SecureRandom sr = new SecureRandom();
      byte[] output = new byte[6];
      sr.nextBytes(output);
      //from byte --> 2 Hexa character
      ownBssid = String.format("%2X%2X%2X%2X%2X%2X",
          output[0], output[1], output[2], output[3], output[4], output[5]);
      ownBssid = ownBssid.replace(" ", "");
    } while (ownBssid.length() != 12);
    return ownBssid;
  }


  private void doRegister() {
    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction(ACTION_UPDATE_UI);
    intentFilter.addAction(ACTION_UPDATE_ERROR);
    //Upload status
    intentFilter.addAction(ACTION_UPLOAD_ERROR);
    //Database status
    intentFilter.addAction(ACTION_UPDATE_DB);
    //Ranking
    intentFilter.addAction(ACTION_UPDATE_RANKING);
    //Kill app signal
    intentFilter.addAction(ACTION_KILL_APP);
    //Auto rank
    intentFilter.addAction(ACTION_AUTO_RANK);
    //Permission
    intentFilter.addAction(ACTION_ASK_PERMISSION);
    LocalBroadcastManager.getInstance(this)
        .registerReceiver(serviceBroadcastReceiver, intentFilter);
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
      case REQUEST_OVERLAY:
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
      case R.id.gps_map:
        //go to gps map
        //startActivity(new Intent(MainActivity.this, MapActivity.class));
        //test kill app
        Config.setMode(Config.MODE.KILL_MODE);
        break;
      case R.id.sort_signal:
        Utils.addPreference(sharedPreferences, PREF_SORT_METHOD, SORT_BY_RSSID);
        sortBySignal();
        adapter.notifyDataSetChanged();
        break;
      case R.id.sort_frequency:
        Utils.addPreference(sharedPreferences, PREF_SORT_METHOD, SORT_BY_FREQ);
        sortByFreq();
        adapter.notifyDataSetChanged();
        break;
      case R.id.sort_timestamp:
        Utils.addPreference(sharedPreferences, PREF_SORT_METHOD, SORT_BY_TIME);
        sortByTimestamp();
        adapter.notifyDataSetChanged();
        break;
      case R.id.upload:
        long apInDb = sharedPreferences.getLong(PREF_TOTAL_AP, 0L);
        if (apInDb < MIN_UPLOAD_ALLOWED) {
          showAlertUpload(getString(R.string.upload_under_limit));
        }else if (Config.getMode() == Config.MODE.UPLOAD_MODE || Config.getMode() == Config.MODE.AUTO_UPLOAD_MODE){
          showAlertUpload(getString(R.string.upload_process));
        }else if (! ((ServiceController.teamId.length() == 0) || Utils.checkBssid(ServiceController.teamId))) {
          AlertDialog.Builder builder = new AlertDialog.Builder(this);
          builder.setMessage(getString(R.string.wrong_id_msg));
          builder.setPositiveButton(R.string.upload, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              dialog.cancel();
              upload();
            }
          });
          builder.setNegativeButton(R.string.recheck, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
              if (dialog != null) {
                dialog.dismiss();
              }
            }
          });
          // Create and show the AlertDialog
          AlertDialog alertDialog = builder.create();
          alertDialog.show();
        } else {
          upload();
        }
        break;
      default:
        break;
    }
    return true;
  }

  private void upload() {
    showToastInCenter(getString(R.string.uploading));
    listView.setVisibility(View.GONE);
    listHeader.setVisibility(View.GONE);
    loading.setVisibility(View.VISIBLE);
    Config.setMode(Config.MODE.UPLOAD_MODE);
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

  private void showAlertExist() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder.setMessage(getString(R.string.exist));
    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        dialog.cancel();
        killService();
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

  private void resetListVisibility() {
    loading.setVisibility(View.GONE);
    listView.setVisibility(View.VISIBLE);
    listHeader.setVisibility(View.VISIBLE);
  }

  public class ServiceBroadcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
      Log.i(LOG_TAG, "Receiving smt from service");
      switch (intent.getAction()) {
        case ACTION_UPDATE_UI:
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
            if (speedUpdate > 0) {
              speed.setText(String
                  .format("%.2f m/s (%.2f km/h)", speedUpdate, speedUpdate * 60 * 60 / 1000.0));
            } else {
              speed.setText("?");
            }
          }
          break;
        case ACTION_UPDATE_DB:
          totalAp.setText(String.valueOf(intent.getLongExtra(R_TOTAL_LIST, 0L)));
          break;
        case ACTION_UPDATE_ERROR:
          gps.setText(getString(R.string.c_gps));
          newestScan.setText("0");
          break;
        case ACTION_UPLOAD_ERROR:
          String msg = intent.getStringExtra(R_UPLOAD_MSG);
          showAlertUpload(msg);
          resetListVisibility();
          break;
        case ACTION_UPDATE_RANKING:
          RankingObject r = intent.getParcelableExtra(R_RANK);
          showAlertUpload(getString(R.string.uploadOK)
              + "\n" + getString(R.string.upCount) + r.uploadedCount
              + "\n" + getString(R.string.upRank) + r.uploadedRank
              + "\n" + getString(R.string.upNewAp) + r.newAps
              + "\n" + getString(R.string.upUpdAp) + r.updAps
              + "\n" + getString(R.string.upDelAp) + r.delAps
              + "\n" + getString(R.string.upNewPoint) + r.newPoints);
          rank.setText(ServiceController.ranking);
          resetListVisibility();
          break;
        case ACTION_AUTO_RANK:
          rank.setText(ServiceController.ranking);
          break;
        case ACTION_KILL_APP:
          finish();
          break;
        case ACTION_ASK_PERMISSION:
          switch (intent.getIntExtra(R_PERMISSION, 0)) {
            case REQUEST_GPS:
              break;
            case REQUEST_WRITE:
              break;
            case REQUEST_OVERLAY:
              startActivityForResult(
                  new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                      Uri.parse("package:" + getPackageName())),
                  REQUEST_OVERLAY);
              break;
            default:
              break;
          }
          break;
        default:
          break;
      }
    }
  }
}
