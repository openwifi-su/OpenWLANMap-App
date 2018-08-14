package su.openwifi.openwlanmap;

import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;
import java.util.List;
import org.osmdroid.api.IGeoPoint;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlay;
import org.osmdroid.views.overlay.simplefastpoint.SimpleFastPointOverlayOptions;
import org.osmdroid.views.overlay.simplefastpoint.SimplePointTheme;

public class MapActivity extends AppCompatActivity
    implements LoaderManager.LoaderCallbacks<List<IGeoPoint>> {
  private static final int LOADER_ID = 1;
  private MapView mapView;
  private ProgressBar progressView;
  private LoaderManager loaderManager;
  private ConnectivityManager connectivityManager;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //load/initialize the osmdroid configuration
    //set up before setContextView
    Context ctx = getApplicationContext();
    Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
    setContentView(R.layout.activity_map);
    progressView = findViewById(R.id.map_loading);
    //set up map view
    mapView = findViewById(R.id.osm_own_map);
    mapView.setTileSource(TileSourceFactory.MAPNIK);
    mapView.setBuiltInZoomControls(true);
    mapView.setMultiTouchControls(true);
    mapView.setMinZoomLevel(3.0);
    mapView.setVerticalMapRepetitionEnabled(false);
    loaderManager = getLoaderManager();
    connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo infor = connectivityManager.getActiveNetworkInfo();
    if (infor != null && infor.isConnected()) {
      loaderManager.initLoader(LOADER_ID, null, this);
    } else {
      // No internet
      Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
    }
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.map_refresh:
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo infor = connectivityManager.getActiveNetworkInfo();
        if (infor != null && infor.isConnected()) {
          loaderManager.restartLoader(LOADER_ID, null, this).forceLoad();
        } else {
          Toast.makeText(this, getString(R.string.no_connection), Toast.LENGTH_SHORT).show();
        }
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
  public Loader<List<IGeoPoint>> onCreateLoader(int id, Bundle args) {
    progressView.setVisibility(View.VISIBLE);
    mapView.setVisibility(View.GONE);
    return new CoordinateLoader(this);
  }

  @Override
  public void onLoadFinished(Loader<List<IGeoPoint>> loader, List<IGeoPoint> data) {
    progressView.setVisibility(View.GONE);
    if (data != null && !data.isEmpty()) {
      //TODO using map cluster overlay
      // to pretend loading map too slowly
      // wrap them in a theme
      SimplePointTheme pt = new SimplePointTheme(data, false);

      // set some visual options for the overlay
      // we use here MAXIMUM_OPTIMIZATION algorithm, which works well with >100k points
      SimpleFastPointOverlayOptions opt = SimpleFastPointOverlayOptions.getDefaultStyle()
          .setAlgorithm(SimpleFastPointOverlayOptions.RenderingAlgorithm.MAXIMUM_OPTIMIZATION)
          .setRadius(7).setIsClickable(false).setCellSize(15);

      // create the overlay with the theme
      final SimpleFastPointOverlay sfpo = new SimpleFastPointOverlay(pt, opt);
      // add overlay
      //check network infor
      mapView.getOverlays().add(sfpo);
      mapView.setVisibility(View.VISIBLE);
    } else {
      Toast.makeText(this,
          getString(R.string.map_loading_error),
          Toast.LENGTH_SHORT).show();
    }
  }

  @Override
  public void onLoaderReset(Loader<List<IGeoPoint>> loader) {
    //Do nothing
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.map_menu, menu);
    return true;
  }

}
