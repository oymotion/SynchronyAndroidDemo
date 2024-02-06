package com.oymotion.synchronydemo;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.oymotion.synchrony.CommandResponseCallback;
import com.oymotion.synchrony.DataNotificationCallback;
import com.oymotion.synchrony.SynchronyProfile;

import java.util.Arrays;
import java.util.Deque;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedDeque;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class DeviceActivity extends AppCompatActivity {
    @BindView(R.id.connect)
    Button btn_conncet;
    @BindView(R.id.start)
    Button btn_start;
    @BindView(R.id.get_firmware_version)
    Button btn_getFirmwareVersion;
    @BindView(R.id.set)
    Button btn_set;
    public static final String EXTRA_DEVICE_NAME = "extra_device_name";
    public static final String EXTRA_MAC_ADDRESS = "extra_mac_address";

    private static final String TAG = "Synchrony";
    private SynchronyProfile.BluetoothDeviceStateEx state = SynchronyProfile.BluetoothDeviceStateEx.disconnected;
    private String macAddress;
    private TextView textViewState;
    private TextView textViewQuaternion;
    private TextView textFirmwareVersion;
    private String textErrorMsg = "";
    private Handler handler;
    private Runnable runnable;
    private boolean notifying = false;

    static final int MAX_SAMPLE_COUNT = 256;
    static final int MAX_CHANNEL_COUNT = 8;
    static final int DATA_TYPE_EEG = 0;
    static final int DATA_TYPE_ECG = 1;
    static final int DATA_TYPE_COUNT = 2;

    static final int TIMEOUT = 50000;

    static class SynchronyData{
        public int dataType;
        public int lastPackageIndex;
        public int resolutionBits;
        public int sampleRate;
        public int channelCount;
        public int channelMask;
        public int packageSampleCount;
        public double K;
        static class Item{
            public int rawDataSampleIndex;
            public int rawData;
            public float convertData;
            public float impedance;
        }
        public Deque<Item> items[];
        public SynchronyData(){
            items = new Deque[MAX_CHANNEL_COUNT];
            for (int index = 0;index < MAX_CHANNEL_COUNT;++index){
                items[index] = new ConcurrentLinkedDeque<>();
            }
        }
    }
    private SynchronyData synchronyDatas[] = new SynchronyData[DATA_TYPE_COUNT];

    private Vector<Float> impedanceData = new Vector<Float>();
    private int notifyDataFlag = 0;
    private SynchronyProfile synchronyProfile;


    @OnClick(R.id.connect)
    public void onConnectClick() {
        Log.i("DeviceActivity", "[onConnectClick] state=" + state);

        if (state != SynchronyProfile.BluetoothDeviceStateEx.connected &&
                state != SynchronyProfile.BluetoothDeviceStateEx.connecting &&
                state != SynchronyProfile.BluetoothDeviceStateEx.ready) {

            SynchronyProfile.GF_RET_CODE ret_code = synchronyProfile.connect(macAddress, false);

            if (ret_code != SynchronyProfile.GF_RET_CODE.GF_SUCCESS) {
                Log.e("DeviceActivity", "Connect failed, ret_code: " + ret_code);
                textViewState.setText("Connect failed, ret_code: " + ret_code);
                return;
            }

            handler.removeCallbacks(runnable);
            handler.postDelayed(runnable, 1000);
        } else {
            boolean success = synchronyProfile.disconnect();

            if (success) {
                btn_getFirmwareVersion.setEnabled(false);
                btn_set.setEnabled(false);
                btn_start.setEnabled(false);

                notifying = false;

                btn_start.setText("Start Data Notification");
                textViewQuaternion.setText("W: " + "\nX: " + "\nY: " + "\nZ: ");
                textFirmwareVersion.setText("FirmwareVersion: ");
            }
        }
    }


    private int response = -1;

    @OnClick(R.id.set)
    public void onSetClick() {
        if (state != SynchronyProfile.BluetoothDeviceStateEx.ready) return;
        response = -1;
        synchronyProfile.setDataNotifSwitch(notifyDataFlag, new CommandResponseCallback() {
            @Override
            public void onSetCommandResponse(int resp) {
                if (resp == SynchronyProfile.ResponseResult.RSP_CODE_SUCCESS) {
                    Log.d(TAG, "Device State: " + "Set Data Switch succeeded");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            btn_start.setEnabled(true);
                        }
                    });

                } else {
                    Log.d(TAG,  "Device State: " + "Set Data Switch failed, resp code: " + resp);
                }
            }
        }, TIMEOUT);
    }

    private void getEEG(){
        synchronyProfile.getEegDataConfig(new CommandResponseCallback() {
            @Override
            public void onGetEegDataConfig(int resp, int sampleRate, int channelMask, int packageSampleCount, int resolutionBits, double microVoltConversionK) {
                if (resp == SynchronyProfile.ResponseResult.RSP_CODE_SUCCESS) {
                    Log.d(TAG, "Device State: " + "get  EEG Config succeeded");
                    SynchronyData data = new SynchronyData();
                    data.dataType = SynchronyProfile.NotifDataType.NTF_EEG;
                    data.sampleRate = sampleRate;
                    data.resolutionBits = resolutionBits;
                    data.channelMask = channelMask;
                    data.packageSampleCount = packageSampleCount;
                    data.K = microVoltConversionK;
                    data.lastPackageIndex = 0;
                    synchronyDatas[DATA_TYPE_EEG] = data;

                    synchronyProfile.getEegDataCap(new CommandResponseCallback() {
                        @Override
                        public void onGetEegDataCap(int resp, int[] supportedSampleRates, int maxChannelCount, int maxPackageSampleCount, int[] supportedResolutionBits) {
                            if (resp == SynchronyProfile.ResponseResult.RSP_CODE_SUCCESS){
                                Log.d(TAG, "Device State: " + "get  EEG Cap succeeded");
                                synchronyDatas[DATA_TYPE_EEG].channelCount = maxChannelCount;
                                notifyDataFlag |= (SynchronyProfile.DataNotifFlags.DNF_IMPEDANCE | SynchronyProfile.DataNotifFlags.DNF_EEG);
                            }else{
                                Log.d(TAG, "Device State: " + "get  EEG Cap failed, resp code: " + resp);
                            }
                        }
                    }, TIMEOUT);
                } else {
                    Log.d(TAG, "Device State: " + "get  EEG Config failed, resp code: " + resp);
                }
            }
        }, TIMEOUT);

    }

    private void getECG(){
        synchronyProfile.getEcgDataConfig(new CommandResponseCallback() {
            @Override
            public void onGetEcgDataConfig(int resp, int sampleRate, int channelMask, int packageSampleCount, int resolutionBits, double microVoltConversionK) {
                if (resp == SynchronyProfile.ResponseResult.RSP_CODE_SUCCESS) {
                    Log.d(TAG, "Device State: " + "get  ECG Config succeeded");
                    SynchronyData data = new SynchronyData();
                    data.dataType = SynchronyProfile.NotifDataType.NTF_ECG;
                    data.sampleRate = sampleRate;
                    data.resolutionBits = resolutionBits;
                    data.channelMask = channelMask;
                    data.packageSampleCount = packageSampleCount;
                    data.K = microVoltConversionK;
                    data.lastPackageIndex = 0;
                    synchronyDatas[DATA_TYPE_ECG] = data;

                    synchronyProfile.getEcgDataCap(new CommandResponseCallback() {
                        @Override
                        public void onGetEcgDataCap(int resp, int[] supportedSampleRates, int maxChannelCount, int maxPackageSampleCount, int[] supportedResolutionBits) {
                            if (resp == SynchronyProfile.ResponseResult.RSP_CODE_SUCCESS){
                                Log.d(TAG, "Device State: " + "get  ECG Cap succeeded");
                                synchronyDatas[DATA_TYPE_ECG].channelCount = maxChannelCount;
                                notifyDataFlag |= (SynchronyProfile.DataNotifFlags.DNF_IMPEDANCE | SynchronyProfile.DataNotifFlags.DNF_ECG);
                            }else{
                                Log.d(TAG, "Device State: " + "get  ECG Cap failed, resp code: " + resp);
                            }
                        }
                    }, TIMEOUT);
                } else {
                    Log.d(TAG, "Device State: " + "get ECG Config failed, resp code: " + resp);
                }
            }
        }, TIMEOUT);
    }


    @OnClick(R.id.start)
    public void onStartClick() {
        if (notifying) {
            btn_start.setText("Start Data Notification");

            synchronyProfile.stopDataNotification();

            notifying = false;
        } else {
            if (state != SynchronyProfile.BluetoothDeviceStateEx.ready) return;

            for (int index = 0;index < DATA_TYPE_COUNT;++index){
                if (synchronyDatas[index] == null)
                    continue;
                synchronyDatas[index].lastPackageIndex = 0;
                for (int index1 = 0;index1 < MAX_CHANNEL_COUNT;++index1){
                    synchronyDatas[index].items[index1].clear();
                    impedanceData.clear();
                }
            }
            synchronyProfile.startDataNotification(new DataNotificationCallback() {
                @Override
                public void onData(byte[] data) {
                    Log.i("DeviceActivity", "data type: " + data[0] + ", len: " + data.length);
                    if (data[0] == SynchronyProfile.NotifDataType.NTF_IMPEDANCE){
                        int offset = 1;
                        int packageIndex = ((data[offset + 1] & 0xff) << 8 | (data[offset] & 0xff));
                        offset += 2;
                        Log.d(TAG, "impedance package index: " + packageIndex);
                        impedanceData.clear();
                        int dataCount = (data.length - 3) / 4;
                        for (int index = 0;index < dataCount;++index){
                            float impedance = getFloat(data, offset);
                            offset += 4;
                            impedanceData.add(impedance);
                        }
                        Log.d(TAG, "impedance: " + impedanceData.get(7));
                    }else if (data[0] == SynchronyProfile.NotifDataType.NTF_EEG ||
                            data[0] == SynchronyProfile.NotifDataType.NTF_ECG ){
                        int dataType = data[0] - SynchronyProfile.NotifDataType.NTF_EEG;
                        SynchronyData synchronyData = synchronyDatas[dataType];
                        int offset = 1;
                        try{
                            int packageIndex = ((data[offset + 1] & 0xff) << 8 | (data[offset] & 0xff));
//                            Log.d(TAG, "package index: " + packageIndex);
                            offset += 2;
                            int newPackageIndex = packageIndex;
                            int lastPackageIndex = synchronyData.lastPackageIndex;

                            if (packageIndex < lastPackageIndex){
                                packageIndex += 65536;// package index is U16
                            }
                            int deltaPackageIndex = packageIndex - lastPackageIndex;
                            if (deltaPackageIndex > 1){
                                int lostSampleCount = synchronyData.packageSampleCount * (deltaPackageIndex - 1);
                                Log.e("DeviceActivity", "lost samples:" + lostSampleCount);
                                //add missing samples
                                readSamples(data, synchronyData, 0, lostSampleCount);
                                if (newPackageIndex == 0){
                                    synchronyData.lastPackageIndex = 65535;
                                }else{
                                    synchronyData.lastPackageIndex = newPackageIndex - 1;
                                }
                            }
                            readSamples(data, synchronyData, offset, 0);
                            synchronyData.lastPackageIndex = newPackageIndex;
                        }catch (Exception e){
                            Log.d("DeviceActivity", "error in process data" + e.getLocalizedMessage());
                        }
                    }
                }
            });

            btn_start.setText("Stop Data Notification");
            notifying = true;
        }
    }

    private static float getFloat(byte[] b, int offset) {
        int accum = 0;
        accum = accum | (b[offset + 0] & 0xff) << 0;
        accum = accum | (b[offset + 1] & 0xff) << 8;
        accum = accum | (b[offset + 2] & 0xff) << 16;
        accum = accum | (b[offset + 3] & 0xff) << 24;
        return Float.intBitsToFloat(accum);
    }

    private void readSamples(byte[] data, SynchronyData synchronyData, int offset, int lostSampleCount){
        int sampleCount = synchronyData.packageSampleCount;
        if (lostSampleCount > 0)
            sampleCount = lostSampleCount;

        double K = synchronyData.K;
        int lastSampleIndex = synchronyData.lastPackageIndex * synchronyData.packageSampleCount;

        for (int sampleIndex = 0;sampleIndex < sampleCount; ++sampleIndex, ++lastSampleIndex){
            for (int channelIndex = 0, impedanceIndex = 0; channelIndex < synchronyData.channelCount; ++channelIndex){
                if ((synchronyData.channelMask & (1 << channelIndex)) > 0){
                    if (synchronyData.dataType == SynchronyProfile.NotifDataType.NTF_ECG){
                        impedanceIndex = impedanceData.size() - 1;
                    }
                    float impedance = 0;
                    if (impedanceIndex < impedanceData.size()){
                        impedance = impedanceData.get(impedanceIndex);
                    }
                    ++impedanceIndex;
                    SynchronyData.Item dataItem = new SynchronyData.Item();
                    dataItem.rawDataSampleIndex = lastSampleIndex;
                    dataItem.impedance = impedance;
                    if (lostSampleCount > 0){
                        //add missing samples with 0
                        dataItem.rawData = 0;
                        dataItem.convertData = 0;
                    }else{
                        int rawData = 0;
                        if (synchronyData.resolutionBits == 8){
                            rawData = (0xff & data[offset]) - 128;
                            offset += 1;
                        }else if (synchronyData.resolutionBits == 16){
                            rawData = ((0xff & data[offset]) << 8 | (0xff & data[offset + 1])) - 32768;
                            offset += 2;
                        }else if (synchronyData.resolutionBits == 24) {
                            rawData = ((0xff & data[offset]) << 16 | (0xff & data[offset + 1]) << 8 | (0xff & data[offset + 2])) - 8388608;
                            offset += 3;
                        }
                        float converted = (float) (rawData * K);
                        dataItem.rawData = rawData;
                        dataItem.convertData = converted;
                    }
                    synchronyData.items[channelIndex].push(dataItem);
                    if (synchronyData.items[channelIndex].size() >= MAX_SAMPLE_COUNT){
                        synchronyData.items[channelIndex].removeFirst();
                    }
                }
            }
        }
    }

    @OnClick(R.id.get_firmware_version)
    public void onGetFirmwareVersionClick() {
        SynchronyProfile.GF_RET_CODE result = synchronyProfile.getControllerFirmwareVersion(new CommandResponseCallback() {
            @Override
            public void onGetControllerFirmwareVersion(int resp, String firmwareVersion) {
                Log.i("DeviceActivity", "\nfirmwareVersion: " + firmwareVersion);

                runOnUiThread(new Runnable() {
                    public void run() {
                        textFirmwareVersion.setText("FirmwareVersion: " + firmwareVersion);
                    }
                });
            }
        }, TIMEOUT);

        Log.i("DeviceActivity", "getControllerFirmwareVersion() result " + result);

        if (result != SynchronyProfile.GF_RET_CODE.GF_SUCCESS) {
            textFirmwareVersion.setText("FirmwareVersion: Error : " + result);
        }
        getEEG();
        getECG();
    }



    void updateState() {
        SynchronyProfile.BluetoothDeviceStateEx newState = synchronyProfile.getState();

        if (state != newState) {
            runOnUiThread(new Runnable() {
                public void run() {
                    textViewState.setText("Device State: " + newState.toString());
                }
            });

            state = newState;

            if (state == SynchronyProfile.BluetoothDeviceStateEx.disconnected) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        btn_conncet.setText("Connect");
                    }
                });
            } else if (state == SynchronyProfile.BluetoothDeviceStateEx.connected) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        btn_conncet.setText("Disconnect");
                    }
                });
            } else if (state == SynchronyProfile.BluetoothDeviceStateEx.connecting) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        btn_conncet.setText("Disconnect");
                    }
                });
            } else if (state == SynchronyProfile.BluetoothDeviceStateEx.disconnecting) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        btn_conncet.setText("Connect");
                    }
                });
            } else if (state == SynchronyProfile.BluetoothDeviceStateEx.ready) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        btn_conncet.setText("Disconnect");

                        btn_getFirmwareVersion.setEnabled(true);
                        btn_set.setEnabled(true);
                        // btn_start.setEnabled(true);
                    }
                });
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);
        ButterKnife.bind(this);
        macAddress = getIntent().getStringExtra(EXTRA_MAC_ADDRESS);
        getSupportActionBar().setSubtitle(getString(R.string.dev_name_with_mac, getIntent().getStringExtra(EXTRA_DEVICE_NAME), macAddress));

        synchronyProfile = new SynchronyProfile(new SynchronyProfile.SynchronyErrorCallback() {
            @Override
            public void onSynchronyErrorCallback(String errorMsg) {
                Toast.makeText(DeviceActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });

        textViewState = this.findViewById(R.id.text_device_state);
        textViewQuaternion = this.findViewById(R.id.text_quaternion);
        textFirmwareVersion = this.findViewById(R.id.text_firmware_version);

        btn_getFirmwareVersion.setEnabled(false);
        btn_set.setEnabled(false);
        btn_start.setEnabled(false);

        handler = new Handler();

        runnable = new Runnable() {
            public void run() {
                updateState();
                handler.postDelayed(this, 1000);
            }
        };
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
        synchronyProfile.disconnect();
    }
}
