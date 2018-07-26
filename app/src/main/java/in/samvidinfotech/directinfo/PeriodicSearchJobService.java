package in.samvidinfotech.directinfo;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.util.Log;

import org.greenrobot.eventbus.EventBus;

import in.samvidinfotech.directinfo.events.StartPeriodicSearchEvent;

/**
 * Created by samvidmistry on 9/4/18.
 */

public class PeriodicSearchJobService extends JobService {
    @Override
    public boolean onStartJob(JobParameters params) {
        Log.d("PeriodicSearchJob", "onStartJob: Posted event to start search");
        EventBus.getDefault().post(new StartPeriodicSearchEvent());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            BluetoothHandlerService.schedulePeriodicSearchJob(this);
        }
        jobFinished(params, false);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return false;
    }
}
