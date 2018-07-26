package in.samvidinfotech.directinfo;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.TaskStackBuilder;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import android.util.Log;
import android.widget.Toast;

import com.polidea.rxandroidble.RxBleClient;
import com.polidea.rxandroidble.RxBleConnection;
import com.polidea.rxandroidble.RxBleDevice;
import com.polidea.rxandroidble.RxBleDeviceServices;
import com.polidea.rxandroidble.exceptions.BleAlreadyConnectedException;
import com.polidea.rxandroidble.exceptions.BleGattCannotStartException;
import com.polidea.rxandroidble.scan.ScanResult;
import com.polidea.rxandroidble.scan.ScanSettings;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import in.samvidinfotech.directinfo.events.BleContentDiscoveredEvent;
import in.samvidinfotech.directinfo.events.BleDeviceConnectedEvent;
import in.samvidinfotech.directinfo.events.BleDeviceFoundEvent;
import in.samvidinfotech.directinfo.events.BleStartScanEvent;
import in.samvidinfotech.directinfo.events.CannotConnectToBleEvent;
import in.samvidinfotech.directinfo.events.CharacteristicReadEvent;
import in.samvidinfotech.directinfo.events.CharacteristicsDiscoveredEvent;
import in.samvidinfotech.directinfo.events.ConnectToDeviceEvent;
import in.samvidinfotech.directinfo.events.DisconnectDeviceEvent;
import in.samvidinfotech.directinfo.events.DiscoverCharacteristicsEvent;
import in.samvidinfotech.directinfo.events.DiscoverServicesEvent;
import in.samvidinfotech.directinfo.events.MessageCreatedEvent;
import in.samvidinfotech.directinfo.events.MultipleCharacteristicsReadEvent;
import in.samvidinfotech.directinfo.events.ReadBleEvent;
import in.samvidinfotech.directinfo.events.ReadCharacteristicEvent;
import in.samvidinfotech.directinfo.events.ServiceReadyEvent;
import in.samvidinfotech.directinfo.events.ServicesDiscoveredEvent;
import in.samvidinfotech.directinfo.events.SetupNotificationEvent;
import in.samvidinfotech.directinfo.events.StartPeriodicSearchEvent;
import in.samvidinfotech.directinfo.events.StopProgressIndicatorEvent;
import in.samvidinfotech.directinfo.events.ToastMessageEvent;
import in.samvidinfotech.directinfo.events.UnreadableCharacteristicEvent;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.FuncN;

public class BluetoothHandlerService extends Service {
    private static final String TAG = "BluetoothHandlerService";
    private static final String PERIODIC_SEARCH_NOTIFICATION_CHANNEL = "periodic-search";
    private static final long PERIODIC_SEARCH_JOB_INTERVAL = 16 * 1000;
    private static final int MTU = 512;
    private RxBleClient mRxBleClient;
    private Handler mHandler;
    private List<RxBleDevice> mNotifiedDevices;
    private ExecutorService mExecutorService;
    private int notificationIdCounter = 0;
    private static RxBleConnection sRxBleConnection;
    private Subscription mScanSubscription, mDeviceSubscription, mServicesSubscription;
    private Subscription mCharacteristicsSubscription;
    public static boolean sShouldSchedule = true;
    public static boolean sIsReady = false;

    public BluetoothHandlerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mRxBleClient = RxBleClient.create(this);
        EventBus.getDefault().register(this);
        mExecutorService = Executors.newCachedThreadPool();
        mHandler = new Handler();
        mNotifiedDevices = new ArrayList<>();
        schedulePeriodicSearchJob(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    PERIODIC_SEARCH_NOTIFICATION_CHANNEL,
                    "Search channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                NotificationChannel notificationChannel = notificationManager
                        .getNotificationChannel(PERIODIC_SEARCH_NOTIFICATION_CHANNEL);
                if (notificationChannel != null) return;
                notificationManager.createNotificationChannel(channel);
            }
        }
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                EventBus.getDefault().post(new ServiceReadyEvent());
            }
        }, 2000);
        sIsReady = true;
    }

    public static boolean schedulePeriodicSearchJob(Context context) {
        if (!sShouldSchedule) return false;

        ComponentName service = new ComponentName(context, PeriodicSearchJobService.class);
        JobInfo.Builder builder = new JobInfo.Builder(0, service);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setMinimumLatency(PERIODIC_SEARCH_JOB_INTERVAL);
        } else {
            builder.setPeriodic(PERIODIC_SEARCH_JOB_INTERVAL);
        }
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(JOB_SCHEDULER_SERVICE);
        if (jobScheduler != null) {
            jobScheduler.schedule(builder.build());
            return true;
        }

        return false;
    }

    @Subscribe
    public void onBleStartScanEvent(BleStartScanEvent bleStartScanEvent) {
        scanDevices();
    }

    private void scanDevices() {
        stopScanningIfRequired();
        mScanSubscription = mRxBleClient.scanBleDevices(
                new ScanSettings.Builder().build())
                .subscribe(
                        new Action1<ScanResult>() {
                            @Override
                            public void call(ScanResult scanResult) {
                                publishDeviceSearchResults(scanResult.getBleDevice());
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                EventBus.getDefault().post(new ToastMessageEvent(
                                        throwable.toString()));
                                throwable.printStackTrace();
                            }
                        });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopScanningIfRequired();
                EventBus.getDefault().postSticky(new StopProgressIndicatorEvent(MainActivity.TAG));
            }
        }, 20000);
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onConnectToDeviceEvent(ConnectToDeviceEvent connectToDeviceEvent) {
        connectToDevice(connectToDeviceEvent.getRxBleDeviceMac());
    }

    private void connectToDevice(String rxBleDeviceMac) {
        stopScanningIfRequired();
        onDisconnectDeviceEvent(null);
        RxBleDevice device = mRxBleClient.getBleDevice(rxBleDeviceMac);
        mDeviceSubscription = device.establishConnection(false)
                .subscribe(new Action1<RxBleConnection>() {
                    @Override
                    public void call(RxBleConnection rxBleConnection) {
                        sRxBleConnection = rxBleConnection;
                        rxBleConnection.requestMtu(MTU).subscribe(new Action1<Integer>() {
                            @Override
                            public void call(Integer integer) {
                                EventBus.getDefault().post(new BleDeviceConnectedEvent(sRxBleConnection));
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                EventBus.getDefault().post(
                                        new ToastMessageEvent(throwable.getMessage()));
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (throwable instanceof BleAlreadyConnectedException) {
                            EventBus.getDefault().post(
                                    new BleDeviceConnectedEvent(sRxBleConnection)
                            );
                        } else {
                            EventBus.getDefault().postSticky(
                                    new CannotConnectToBleEvent(throwable));
                            throwable.printStackTrace();
                        }
                    }
                });
    }

    @Subscribe
    public void onDiscoverServicesEvent(DiscoverServicesEvent discoverServicesEvent) {
        discoverServices(discoverServicesEvent.getRxBleConnection());
    }

    private void discoverServices(RxBleConnection rxBleConnection) {
        mServicesSubscription = rxBleConnection.discoverServices()
                .subscribe(new Action1<RxBleDeviceServices>() {
                    @Override
                    public void call(RxBleDeviceServices rxBleDeviceServices) {
                        EventBus.getDefault()
                                .post(new ServicesDiscoveredEvent(rxBleDeviceServices));
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        EventBus.getDefault().post(new ToastMessageEvent(throwable.toString()));
                        throwable.printStackTrace();
                    }
                });
    }

    @Subscribe
    public void onDiscoverCharacteristicsEvent(DiscoverCharacteristicsEvent
                                                       discoverCharacteristicsEvent) {
        discoverCharacteristics(discoverCharacteristicsEvent.getRxBleConnection(),
                                discoverCharacteristicsEvent.getServiceUuid());
    }

    private void discoverCharacteristics(RxBleConnection rxBleConnection,
                                         final String serviceUuid) {
        mCharacteristicsSubscription = rxBleConnection.discoverServices()
                .subscribe(new Action1<RxBleDeviceServices>() {
                    @Override
                    public void call(RxBleDeviceServices rxBleDeviceServices) {
                        for (BluetoothGattService bluetoothGattService :
                                rxBleDeviceServices.getBluetoothGattServices()) {
                            if (bluetoothGattService.getUuid().toString().equals(serviceUuid)) {
                                EventBus.getDefault().post(
                                        new CharacteristicsDiscoveredEvent(
                                                bluetoothGattService.getCharacteristics()
                                        )
                                );
                                mCharacteristicsSubscription.unsubscribe();
                                mCharacteristicsSubscription = null;
                                return;
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        EventBus.getDefault().post(new ToastMessageEvent(throwable.toString()));
                        throwable.printStackTrace();
                    }
                });
    }

    @Subscribe
    public void onReadCharacteristicEvent(ReadCharacteristicEvent readCharacteristicEvent) {
        readCharacteristic(readCharacteristicEvent.getRxBleConnection(),
                            readCharacteristicEvent.getUuid());
    }

    private void readCharacteristic(RxBleConnection rxBleConnection, String uuid) {
        rxBleConnection.readCharacteristic(UUID.fromString(uuid))
                .subscribe(new Action1<byte[]>() {
                    @Override
                    public void call(byte[] bytes) {
                        EventBus.getDefault().post(new CharacteristicReadEvent(bytes));
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (throwable instanceof BleGattCannotStartException) {
                            EventBus.getDefault().post(new UnreadableCharacteristicEvent());
                        } else {
                            throwable.printStackTrace();
                        }
                    }
                });

        //TODO: create a button for notification subscription
        //setupNotifications(rxBleConnection, uuid);
    }

    @Subscribe
    public void onSetupNotificationEvent(SetupNotificationEvent setupNotificationEvent) {
        setupNotifications(setupNotificationEvent.getRxBleConnection(),
                setupNotificationEvent.getUuid());
    }

    private void setupNotifications(RxBleConnection rxBleConnection, String uuid) {
        rxBleConnection.setupNotification(UUID.fromString(uuid))
                .doOnNext(new Action1<Observable<byte[]>>() {
                    @Override
                    public void call(Observable<byte[]> observable) {
                        Log.d(TAG, "call: Notification setup successful");
                    }
                })
                .flatMap(new Func1<Observable<byte[]>, Observable<byte[]>>() {
                    @Override
                    public Observable<byte[]> call(Observable<byte[]> observable) {
                        return observable;
                    }
                }).subscribe(new Action1<byte[]>() {
                    @Override
                    public void call(byte[] o) {
                        EventBus.getDefault().post(new CharacteristicReadEvent(o));
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        EventBus.getDefault().post(new ToastMessageEvent(throwable.toString()));
                        throwable.printStackTrace();
                    }
                });
    }

    @Subscribe
    public void onReadBleEvent(ReadBleEvent event) {
        readBleEvent(event.getmBleMac(), new FuncN<Void>() {
            @Override
            public Void call(Object... args) {
                if (args[0] instanceof String) {
                    String[] data = ((String) args[0]).split("-");
                    EventBus.getDefault().post(new BleContentDiscoveredEvent(data[0], data[1]));
                }

                return null;
            }
        });
    }

    public void readBleEvent(String mac, final FuncN<Void> func) {
        RxBleDevice device = mRxBleClient.getBleDevice(mac);
        mDeviceSubscription = device.establishConnection(false)
                .subscribe(new Action1<RxBleConnection>() {
                    @Override
                    public void call(final RxBleConnection rxBleConnection) {
                        sRxBleConnection = rxBleConnection;
                        rxBleConnection.requestMtu(MTU).subscribe(new Action1<Integer>() {
                            @Override
                            public void call(Integer integer) {
                                rxBleConnection.discoverServices().subscribe(new Action1<Object>() {
                                    @Override
                                    public void call(Object o) {
                                        if (!(o instanceof RxBleDeviceServices)) return;

                                        List<BluetoothGattCharacteristic> characteristicList = ((RxBleDeviceServices) o)
                                                .getBluetoothGattServices().get(2)
                                                .getCharacteristics();

                                        List<String> characteristics = new ArrayList<>(characteristicList.size());
                                        for (BluetoothGattCharacteristic characteristic : characteristicList) {
                                            characteristics.add(characteristic.getUuid().toString());
                                        }

                                        readCharacteristics(rxBleConnection, characteristics, func);
                                    }
                                }, new Action1<Throwable>() {
                                    @Override
                                    public void call(Throwable throwable) {
                                        throwable.printStackTrace();
                                    }
                                });
                            }
                        }, new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                EventBus.getDefault().post(
                                        new ToastMessageEvent(throwable.getMessage()));
                            }
                        });
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (throwable instanceof BleAlreadyConnectedException) {
                            EventBus.getDefault().post(
                                    new BleDeviceConnectedEvent(sRxBleConnection)
                            );
                        } else {
                            EventBus.getDefault().postSticky(
                                    new CannotConnectToBleEvent(throwable));
                            throwable.printStackTrace();
                        }
                    }
                });
    }

    @Subscribe
    public void onStartPeriodicSearchEvent(StartPeriodicSearchEvent event) {
        performPeriodicSearch();
    }

    private void performPeriodicSearch() {
        RxBleClient client = RxBleClient.create(this);
        if (client.getState() != RxBleClient.State.READY) {
            client = null;
            return;
        }

        final Subscription searchSubscription = client.scanBleDevices(
                new ScanSettings.Builder().build())
                .subscribe(new Action1<ScanResult>() {
                    @Override
                    public void call(ScanResult scanResult) {
                        final RxBleDevice bleDevice = scanResult.getBleDevice();
                        if (mNotifiedDevices.contains(bleDevice)) return;

                        if (bleDevice.getName() == null || !(bleDevice.getName().startsWith("BLE")
                                || bleDevice.getName().startsWith("ble"))) return;

                        mExecutorService.execute(new Runnable() {
                            @Override
                            public void run() {
                                mNotifiedDevices.add(bleDevice);
                                final CountDownLatch latch = new CountDownLatch(1);

                                final Subscription connectionSubscription =
                                    bleDevice.establishConnection(false)
                                        .subscribe(new Action1<RxBleConnection>() {
                                            @Override
                                            public void call(final RxBleConnection rxBleConnection) {
                                                rxBleConnection.requestMtu(MTU)
                                                        .subscribe(new Action1<Integer>() {
                                                            @Override
                                                            public void call(Integer integer) {
                                                                sendNotification(rxBleConnection, latch, bleDevice.getName());
                                                            }
                                                        }, new Action1<Throwable>() {
                                                            @Override
                                                            public void call(Throwable throwable) {
                                                                Log.d(TAG, throwable.getMessage());
                                                            }
                                                        });
                                            }
                                        }, new Action1<Throwable>() {
                                            @Override
                                            public void call(Throwable throwable) {
                                                EventBus.getDefault().post(new ToastMessageEvent(
                                                        throwable.toString()
                                                ));
                                                throwable.printStackTrace();
                                            }
                                        });

                                try {
                                    latch.await();
                                } catch (InterruptedException e) {
                                    EventBus.getDefault().post(new ToastMessageEvent(
                                            e.toString()
                                    ));
                                }

                                if (connectionSubscription != null &&
                                        !connectionSubscription.isUnsubscribed()) {
                                    connectionSubscription.unsubscribe();
                                }
                            }
                        });

                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        EventBus.getDefault().post(new ToastMessageEvent(throwable.toString()));
                        throwable.printStackTrace();
                    }
                });

        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: Unsubscribed from periodic search");
                if (searchSubscription != null && !searchSubscription.isUnsubscribed()) {
                    searchSubscription.unsubscribe();
                }
                mNotifiedDevices.clear();
            }
        }, 10000);
    }

    private void sendNotification(final RxBleConnection rxBleConnection,
                                  @Nullable final CountDownLatch latch, final String name) {
        rxBleConnection.discoverServices().subscribe(new Action1<Object>() {
            @Override
            public void call(Object o) {
                if (!(o instanceof RxBleDeviceServices)) return;

                List<BluetoothGattCharacteristic> characteristicList = ((RxBleDeviceServices) o)
                                .getBluetoothGattServices().get(2)
                                .getCharacteristics();

                List<String> characteristics = new ArrayList<>(characteristicList.size());
                for (BluetoothGattCharacteristic characteristic : characteristicList) {
                    characteristics.add(characteristic.getUuid().toString());
                }

                readCharacteristics(rxBleConnection, characteristics,
                        new FuncN<Void>() {
                            @Override
                            public Void call(Object... args) {
                                if (args[0] instanceof String) {
                                    String[] data = ((String) args[0]).split("-");
                                    if (data.length < 2) {
                                        EventBus.getDefault().post(new ToastMessageEvent("Error in specified message from server"));
                                        return null;
                                    }

                                    NotificationCompat.Builder builder =
                                            new NotificationCompat.Builder(
                                            BluetoothHandlerService.this,
                                            PERIODIC_SEARCH_NOTIFICATION_CHANNEL
                                    ).setSmallIcon(R.mipmap.ic_launcher_round)
                                            .setContentTitle("New device found")
                                            .setContentText(name)
                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);
                                    Intent intent = ContentActivity.getIntent(BluetoothHandlerService.this, data[0], data[1]);
                                    TaskStackBuilder stackBuilder = TaskStackBuilder.create(BluetoothHandlerService.this);
                                    stackBuilder.addNextIntentWithParentStack(intent);
                                    PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
                                    builder.setContentIntent(pendingIntent).setAutoCancel(true);
                                    NotificationManagerCompat.from(BluetoothHandlerService.this)
                                            .notify(notificationIdCounter++, builder.build());
                                    if (latch != null) {
                                        latch.countDown();
                                    }
                                }

                                return null;
                            }
                        });
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                throwable.printStackTrace();
            }
        });
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onMultipleCharacteristicsReadEvent(MultipleCharacteristicsReadEvent event) {
        readCharacteristics(event.getRxBleConnection(), event.getCharacteristicList(),
                new FuncN<Void>() {
                    @Override
                    public Void call(Object... args) {
                        if (args[0] instanceof String) {
                            EventBus.getDefault().post(new MessageCreatedEvent((String) args[0]));
                        }
                        return null;
                    }
                });
    }

    private void readCharacteristics(RxBleConnection rxBleConnection,
                                     List<String> characteristicList,
                                     final FuncN<Void> performAction) {
        List<Observable<byte []>> observables = new ArrayList<>();
        for (String s : characteristicList) {
            observables.add(rxBleConnection.readCharacteristic(UUID.fromString(s)));
        }
        Observable.combineLatest(observables, new FuncN<String>() {
            @Override
            public String call(Object... args) {
                for (Object arg : args) {
                    if (!(arg instanceof byte[])) {
                        EventBus.getDefault().post(new ToastMessageEvent(
                                "Found something except byte[]"
                        ));
                        return "";
                    }
                }

                byte[] bytes;
                try {
                    bytes = getFlattenedBytes(args);
                } catch (IOException e) {
                    EventBus.getDefault().post(new ToastMessageEvent(
                            "Error converting bytes to string"
                    ));
                    e.printStackTrace();
                    return null;
                }
                return new String(bytes);
            }
        }).subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                if (s == null) return;

                performAction.call(s);
            }
        }, new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                EventBus.getDefault().post(new ToastMessageEvent(throwable.toString()));
                throwable.printStackTrace();
            }
        });
    }

    private static byte[] getFlattenedBytes(Object[] array) throws IOException {
        List<Byte> stringBytes = new ArrayList<>(array.length);
        for (Object o : array) {
            byte[] obj = (byte[]) o;
            for (byte anObj : obj) {
                stringBytes.add(anObj);
            }
        }

        byte[] bytes = new byte[stringBytes.size()];
        for (int i = 0; i < stringBytes.size(); i++) {
            bytes[i] = stringBytes.get(i);
        }

        return bytes;
    }

    private void publishDeviceSearchResults(RxBleDevice bleDevice) {
        EventBus.getDefault().post(new BleDeviceFoundEvent(bleDevice));
    }

    @Subscribe(threadMode = ThreadMode.MAIN_ORDERED)
    public void onToastMessageEvent(ToastMessageEvent event) {
        Toast.makeText(this, event.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Subscribe
    public void onDisconnectDeviceEvent(DisconnectDeviceEvent disconnectDeviceEvent) {
        sRxBleConnection = null;
        disconnectServices();
        disconnectFromDevice();
    }

    private void disconnectServices() {
        if (mServicesSubscription != null) {
            mServicesSubscription.unsubscribe();
            mServicesSubscription = null;
        }
    }

    private void disconnectFromDevice() {
        if (mDeviceSubscription != null) {
            mDeviceSubscription.unsubscribe();
            mDeviceSubscription = null;
        }
    }

    private void stopScanningIfRequired() {
        if (mScanSubscription != null) {
            mScanSubscription.unsubscribe();
            mScanSubscription = null;
        }
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    public static RxBleConnection getRxBleConnection() {
        return sRxBleConnection;
    }
}
