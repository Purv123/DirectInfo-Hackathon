package in.samvidinfotech.directinfo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.polidea.rxandroidble.RxBleConnection;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import in.samvidinfotech.directinfo.events.CharacteristicReadEvent;
import in.samvidinfotech.directinfo.events.ReadCharacteristicEvent;
import in.samvidinfotech.directinfo.events.UnreadableCharacteristicEvent;

public class CharacteristicsActivity extends AppCompatActivity {
    private static final String CHARACTERISTICS = "characteristics";
    private ListView mCharacteristicsList;
    private TextView mCharacteristicsValueTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_characteristics);

        mCharacteristicsList = findViewById(R.id.characteristics_list);
        mCharacteristicsValueTextView = findViewById(R.id.characteristic_value_textview);
        if (getIntent() == null) return;

        Bundle extras = getIntent().getExtras();
        if (extras == null) return;

        List<String> characteristics = (List<String>) extras.getSerializable(CHARACTERISTICS);
        if (characteristics == null) return;

        mCharacteristicsList.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                characteristics));
        mCharacteristicsList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                RxBleConnection rxBleConnection = BluetoothHandlerService.getRxBleConnection();
                if (rxBleConnection == null) {
                    Toast.makeText(CharacteristicsActivity.this,
                            "RxBleConnection is null", Toast.LENGTH_SHORT).show();
                    return;
                }
                EventBus.getDefault().post(new ReadCharacteristicEvent(
                        rxBleConnection,
                        (String) parent.getAdapter().getItem(position)
                ));
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onCharacteristicReadEvent(CharacteristicReadEvent characteristicReadEvent) {
        characteristicRead(characteristicReadEvent.getCharacteristicValue());
    }

    private void characteristicRead(byte[] characteristicValue) {
        mCharacteristicsValueTextView.setText(new String(characteristicValue));
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onUnreadableCharacteristicEvent(
            UnreadableCharacteristicEvent unreadableCharacteristicEvent) {
        onUnreadableCharacteristic();
    }

    private void onUnreadableCharacteristic() {
        Toast.makeText(this, "Unreadable Characteristic", Toast.LENGTH_SHORT).show();
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

    public static Intent getIntent(Context context, ArrayList<String> characteristics) {
        Bundle bundle = new Bundle();
        bundle.putSerializable(CHARACTERISTICS, characteristics);
        Intent intent = new Intent(context, CharacteristicsActivity.class);
        intent.putExtras(bundle);
        return intent;
    }
}
