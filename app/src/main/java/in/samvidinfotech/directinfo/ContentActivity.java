package in.samvidinfotech.directinfo;

import androidx.appcompat.app.AppCompatActivity;
import in.samvidinfotech.directinfo.events.BleContentDiscoveredEvent;
import in.samvidinfotech.directinfo.events.DisconnectDeviceEvent;
import in.samvidinfotech.directinfo.events.ReadBleEvent;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class ContentActivity extends AppCompatActivity {
    private static final String BLE_MAC = "ble_mac";
    private static final String TITLE = "title";
    private static final String CONTENT = "content";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content);
        if (getIntent().hasExtra(BLE_MAC)) {
            EventBus.getDefault().post(new ReadBleEvent(getIntent().getStringExtra(BLE_MAC)));
        } else {
            onBleContentDiscoveredEvent(new BleContentDiscoveredEvent(getIntent().getStringExtra(TITLE), getIntent().getStringExtra(CONTENT)));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onBleContentDiscoveredEvent(BleContentDiscoveredEvent event) {
        TextView title = findViewById(R.id.title), content = findViewById(R.id.content);
        TransitionManager.beginDelayedTransition((ViewGroup) findViewById(R.id.root));
        title.setText(event.getmTitle());
        content.setText(event.getmContent());
        findViewById(R.id.progress).setVisibility(View.GONE);
        findViewById(R.id.root).setVisibility(View.VISIBLE);
    }

    public static Intent getIntent(Context context, String mac) {
        Intent intent = new Intent(context, ContentActivity.class);
        intent.putExtra(BLE_MAC, mac);
        return intent;
    }

    public static Intent getIntent(Context context, String title, String content) {
        Intent intent = new Intent(context, ContentActivity.class);
        intent.putExtra(TITLE, title);
        intent.putExtra(CONTENT, content);
        return intent;
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().post(new DisconnectDeviceEvent());
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
}
