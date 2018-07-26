package in.samvidinfotech.directinfo;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.polidea.rxandroidble.RxBleDevice;

import java.util.ArrayList;
import java.util.List;

public final class BleListAdapter extends ArrayAdapter<RxBleDevice> {
    private List<RxBleDevice> mDevices = null;
    public BleListAdapter(Context context, int resource) {
        super(context, resource);
    }

    public BleListAdapter(Context context, int resource, int textViewResourceId) {
        super(context, resource, textViewResourceId);
    }

    public BleListAdapter(Context context, int resource, RxBleDevice[] objects) {
        super(context, resource, objects);
    }

    public BleListAdapter(Context context, int resource, int textViewResourceId, RxBleDevice[] objects) {
        super(context, resource, textViewResourceId, objects);
    }

    public BleListAdapter(Context context, int resource, List<RxBleDevice> objects) {
        super(context, resource, objects);
        mDevices = new ArrayList<>();
        mDevices.addAll(objects);
    }

    public BleListAdapter(Context context, int resource, int textViewResourceId, List<RxBleDevice> objects) {
        super(context, resource, textViewResourceId, objects);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.ble_list_item,
                                parent, false);
        }
        convertView.setTransitionName("item" + position);
        TextView bleName = convertView.findViewById(R.id.ble_name);
        bleName.setText(getItem(position).getName() == null ? "No name found" :
                        getItem(position).getName());
        return convertView;
    }
}
