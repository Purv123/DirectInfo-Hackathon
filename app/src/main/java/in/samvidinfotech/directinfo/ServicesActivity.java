package in.samvidinfotech.directinfo;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import androidx.core.app.ActivityCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDeviceServices;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import in.samvidinfotech.directinfo.events.BleDeviceConnectedEvent;
import in.samvidinfotech.directinfo.events.CannotConnectToBleEvent;
import in.samvidinfotech.directinfo.events.ConnectToDeviceEvent;
import in.samvidinfotech.directinfo.events.DisconnectDeviceEvent;
import in.samvidinfotech.directinfo.events.DiscoverServicesEvent;
import in.samvidinfotech.directinfo.events.MessageCreatedEvent;
import in.samvidinfotech.directinfo.events.MultipleCharacteristicsReadEvent;
import in.samvidinfotech.directinfo.events.ServicesDiscoveredEvent;
import in.samvidinfotech.directinfo.events.ToastMessageEvent;

public class ServicesActivity extends AppCompatActivity {
    public static final String TAG = ServicesActivity.class.getSimpleName();
    public static final String BLUETOOTH_MAC = "bluetooth_mac";
    private ListView mServicesList;
    private Toolbar mToolbar;
    private SwipeRefreshLayout mRefreshLayout;
    private boolean mIsGoingForCharacteristics = false;
    private final List<BluetoothGattService> mServices = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_services);

        mToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        mToolbar.setTitle("Services");
        mToolbar.setTitleTextColor(Color.WHITE);
        mRefreshLayout = findViewById(R.id.swipe_refresh);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                if (getIntent() == null || !getIntent().hasExtra(BLUETOOTH_MAC)) {
                    Toast.makeText(ServicesActivity.this,
                            "Error discovering services, try again",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                mRefreshLayout.setRefreshing(true);
                EventBus.getDefault().post(new ConnectToDeviceEvent(
                        getIntent().getStringExtra(BLUETOOTH_MAC)
                ));
            }
        });
        mServicesList = findViewById(R.id.servicesList);
        mServicesList.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, mServices));
        mServicesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothGattService service =
                        (BluetoothGattService) parent.getAdapter().getItem(position);
                ArrayList<String> characteristics = new ArrayList<>();
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    characteristics.add(characteristic.getUuid().toString());
                }
                mIsGoingForCharacteristics = true;
                ActivityCompat.startActivity(ServicesActivity.this,
                        CharacteristicsActivity.getIntent(ServicesActivity.this,
                                                            characteristics), null);
            }
        });
        mServicesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position,
                                           long id) {
                BluetoothGattService service =
                        (BluetoothGattService) parent.getAdapter().getItem(position);
                ArrayList<String> characteristics = new ArrayList<>();
                for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                    characteristics.add(characteristic.getUuid().toString());
                }
                EventBus.getDefault().post(new MultipleCharacteristicsReadEvent(
                        BluetoothHandlerService.getRxBleConnection(), characteristics));
                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        EventBus.getDefault().post(new ConnectToDeviceEvent(
                getIntent().getStringExtra(BLUETOOTH_MAC)
        ));
        mRefreshLayout.setRefreshing(true);
        mIsGoingForCharacteristics = false;
    }

    @Subscribe
    public void onBleDeviceConnectedEvent(BleDeviceConnectedEvent bleDeviceConnectedEvent) {
        onDeviceConnected(bleDeviceConnectedEvent.getRxBleConnection());
    }

    private void onDeviceConnected(RxBleConnection rxBleConnection) {
        EventBus.getDefault().post(new DiscoverServicesEvent(rxBleConnection));
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onBleServicesDiscoveredEvent(ServicesDiscoveredEvent servicesDiscoveredEvent) {
        onServicesDiscovered(servicesDiscoveredEvent.getRxBleDeviceServices());
    }

    private void onServicesDiscovered(RxBleDeviceServices rxBleDeviceServices) {
        mRefreshLayout.setRefreshing(false);
        mServices.clear();
        mServices.addAll(rxBleDeviceServices.getBluetoothGattServices());
        ((ArrayAdapter) mServicesList.getAdapter()).notifyDataSetChanged();
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onMessageCreatedEvent(MessageCreatedEvent messageCreatedEvent) {
        onMessageCreated(messageCreatedEvent.getMessage());
    }

    private void onMessageCreated(String message) {
        /*final com.google.android.material.snackbar.Snackbar snackbar = Snackbar.make(mRefreshLayout, message, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("Dismiss", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                snackbar.dismiss();
            }
        });
        snackbar.show();*/
        EventBus.getDefault().post(new ToastMessageEvent(message));
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN_ORDERED)
    public void onCannotConnectToBleEvent(CannotConnectToBleEvent event) {
        mRefreshLayout.setRefreshing(false);
        Snackbar.make(mRefreshLayout, "Cannot connect to BLE", Snackbar.LENGTH_LONG).show();
        EventBus.getDefault().removeStickyEvent(event);
    }

    @Override
    protected void onStop() {
        if (!mIsGoingForCharacteristics) EventBus.getDefault().post(new DisconnectDeviceEvent());

        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    public static Intent getIntent(Context context, String bleMac) {
        Intent intent = new Intent(context, ServicesActivity.class);
        intent.putExtra(BLUETOOTH_MAC, bleMac);
        return intent;
    }
}
