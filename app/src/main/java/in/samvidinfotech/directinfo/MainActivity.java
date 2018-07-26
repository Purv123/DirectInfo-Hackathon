package in.samvidinfotech.directinfo;

import android.Manifest;
import android.app.Activity;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.polidea.rxandroidble.RxBleDevice;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import in.samvidinfotech.directinfo.events.BleDeviceFoundEvent;
import in.samvidinfotech.directinfo.events.BleStartScanEvent;
import in.samvidinfotech.directinfo.events.ServiceReadyEvent;
import in.samvidinfotech.directinfo.events.StopProgressIndicatorEvent;
import in.samvidinfotech.directinfo.events.ToastMessageEvent;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_ENABLE_BT = 467;
    private static final int REQUEST_LOCATION_PERMISSION = 643;
    private ListView mDeviceList;
    private Toolbar mToolbar;
    private SwipeRefreshLayout mRefreshLayout;
    private final List<RxBleDevice> mRxBleDevices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (BluetoothAdapter.getDefaultAdapter() == null) {
            Toast.makeText(this, "Device does not seem to have bluetooth",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        if (!BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            final Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            getLocationPermission();
        } else if (BluetoothAdapter.getDefaultAdapter().isEnabled()
                &&
                ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
            initActivity();
        }
    }

    private void getLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[] { Manifest.permission.ACCESS_FINE_LOCATION },
                REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) initActivity();

            else Toast.makeText(this, "Location permission is needed in order for " +
                        "this app to work. Please restart the app.", Toast.LENGTH_LONG).show();
        }
    }

    private void initViews() {
        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setTitleTextColor(Color.WHITE);
        mToolbar.setTitle("Ble Devices");
        mRefreshLayout = findViewById(R.id.swipe_refresh);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                mRefreshLayout.setRefreshing(true);
                mRxBleDevices.clear();
                ((ArrayAdapter) mDeviceList.getAdapter()).notifyDataSetChanged();
                EventBus.getDefault().post(new BleStartScanEvent());
            }
        });
        mDeviceList = findViewById(R.id.device_list);
        mDeviceList.setAdapter(new BleListAdapter(this,
                android.R.layout.simple_list_item_1, mRxBleDevices));
        mDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RxBleDevice rxBleDevice = (RxBleDevice) parent.getAdapter().getItem(position);
                Intent intent = ContentActivity.getIntent(MainActivity.this,
                        rxBleDevice.getMacAddress());
                mRefreshLayout.setRefreshing(false);
                ActivityCompat.startActivity(MainActivity.this, intent, null);
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onBleDeviceFound(BleDeviceFoundEvent bleDeviceFoundEvent) {
        addToList(bleDeviceFoundEvent.getRxBleDevice());
    }

    @Subscribe
    public void onServiceReady(ServiceReadyEvent serviceReadyEvent) {
        EventBus.getDefault().post(new BleStartScanEvent());
        mRefreshLayout.setRefreshing(true);
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN_ORDERED)
    public void onStopProgressIndicatorEvent(StopProgressIndicatorEvent event) {
        if (event.getTAG().equals(TAG)) {
            mRefreshLayout.setRefreshing(false);
            EventBus.getDefault().removeStickyEvent(event);
        }
    }

    private void addToList(RxBleDevice bleDevice) {
        if (bleDevice != null && !mRxBleDevices.contains(bleDevice)) {
            mRxBleDevices.add(bleDevice);
            ((ArrayAdapter) mDeviceList.getAdapter()).notifyDataSetChanged();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_DENIED) {
                getLocationPermission();
            } else {
                initActivity();
            }
        }
    }

    private void initActivity() {
        initViews();
        startService(new Intent(this, BluetoothHandlerService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.stop_periodic_scan) {
            JobScheduler jobScheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
            if (jobScheduler != null) {
                jobScheduler.cancelAll();
                Log.d(TAG, "onOptionsItemSelected: Cancelled the job");
                Snackbar.make(mDeviceList, "Cancelled the job", Snackbar.LENGTH_SHORT).show();
                BluetoothHandlerService.sShouldSchedule = false;
            }
            return true;
        } else if (item.getItemId() == R.id.start_periodic_scan) {
            Log.d(TAG, "onOptionsItemSelected: Scheduled the job");
            Snackbar.make(mDeviceList, "Scheduled the job", Snackbar.LENGTH_SHORT).show();
            BluetoothHandlerService.sShouldSchedule = true;
            BluetoothHandlerService.schedulePeriodicSearchJob(this);
        }

        return super.onOptionsItemSelected(item);
    }
}
