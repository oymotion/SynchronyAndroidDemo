# SynchronySDKAndroid

## Brief
Synchrony SDK is the software development kit for developers to access OYMotion products.
## 1. Permission

Application will obtain bluetooth permission by itself.

## 2. Import SDK

```java
import com.oymotion.synchrony.*;
```

## 3. Initalize

```java
SynchronyProfile SynchronyProfile = new SynchronyProfile(SynchronyErrorCallback _errorCallback);
```

## 4. Start scan

```java
    boolean result = SynchronyProfile.startScan(new ScanCallback() {
        @Override
        public void onScanResult(BluetoothDevice bluetoothDevice, int rssi) {

        }

        @Override
        public void onScanFailed(int errorCode) {

        }
    });
```

`ScanCallback::onScanFailed(int errorCode)` defines：

```java
    /**
     * Fails to start scan as BLE scan with the same settings is already started by the app.
     */
    public static final int SCAN_FAILED_ALREADY_STARTED = 1;

    /**
     * Fails to start scan as app cannot be registered.
     */
    public static final int SCAN_FAILED_APPLICATION_REGISTRATION_FAILED = 2;

    /**
     * Fails to start scan due an internal error
     */
    public static final int SCAN_FAILED_INTERNAL_ERROR = 3;

    /**
     * Fails to start power optimized scan as this feature is not supported.
     */
    public static final int SCAN_FAILED_FEATURE_UNSUPPORTED = 4;

    /**
     * Fails to start scan as it is out of hardware resources.
     *
     * @hide
     */
    public static final int SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES = 5;

    /**
     * Fails to start scan as application tries to scan too frequently.
     * @hide
     */
    public static final int SCAN_FAILED_SCANNING_TOO_FREQUENTLY = 6;
```

## 5. Stop scan

```java
    boolean result = SynchronyProfile.stopScan();
```


## 6. Connect device


```java
    GF_RET_CODE ret_code = SynchronyProfile.connect(String macAddress, bool autoConnect);
```

Param

| Type  | Name       | Define       |
|:-----|:------------|:-----------|
| String | macAddress | device mac |
| bool | autoConnect | will auto connect |

GF_RET_CODE defines:

```java
public enum GF_RET_CODE {
    GF_SUCCESS,
    GF_ERROR,
    GF_ERROR_BAD_PARAM,
    GF_ERROR_BAD_STATE,
    GF_ERROR_NOT_SUPPORT,
    GF_ERROR_SCAN_BUSY,
    GF_ERROR_NO_RESOURCE,
    GF_ERROR_TIMEOUT,
    GF_ERROR_DEVICE_BUSY,
    GF_ERROR_NOT_READY;
}
```

## 7. Disconnect

```java
    boolean result = SynchronyProfile.disconnect();
```


## 8. Get device status

```java
    BluetoothDeviceStateEx state = SynchronyProfile.getState();
```

Please send command in 'ready' status

```java
    public enum BluetoothDeviceStateEx {disconnected, connecting, connected, ready, disconnecting}
```

## 9. DataNotify

### 9.1 get data feature map of DataNotify(optional)

    GF_RET_CODE result = SynchronyProfile.getFeatureMap(new CommandResponseCallback() {
        @Override
        public void onGetFeatureMap(int resp, int featureMap) {
            Log.i("DeviceActivity", "onGetFeatureMap: " + resp);

            if (resp == ResponseResult.RSP_CODE_SUCCESS) {
                //check featureMap bitmap, see defines below:
            } else {

            }
        }
    }, 5);

```c
#define GFD_FEAT_NONE  0x000000000 //None Optional Feature supported
#define GFD_FEAT_TEMP  0x000000001 //Temperature Feature supported
#define GFD_FEAT_SERV  0x000000002 //Services Switch Feature supported
#define GFD_FEAT_LOG   0x000000004 //SWO Log Feature supported
#define GFD_FEAT_MOTOR   0x000000008 //Motor Feature supported
#define GFD_FEAT_LED   0x000000010 //LED Feature supported
#define GFD_FEAT_TRMOD   0x000000020 //Training Model Upgrade Feature supported
#define GFD_FEAT_ACC   0x000000040 //Accelerate Feature supported
#define GFD_FEAT_GYRO  0x000000080 //Gyroscope Feature supported
#define GFD_FEAT_MAG   0x000000100 //Magnetometer Feature supported
#define GFD_FEAT_EULER   0x000000200 //Euler Angle Feature supported
#define GFD_FEAT_QUAT  0x000000400 //Quaternion Feature supported
#define GFD_FEAT_ROTA  0x000000800 //Rotation Matrix Feature supported
#define GFD_FEAT_GEST  0x000001000 //EMG Gesture Feature supported
#define GFD_FEAT_RAW   0x000002000 //EMG Raw Data Feature supported
#define GFD_FEAT_MOUSE   0x000004000 //HID-Mouse Feature supported
#define GFD_FEAT_JOYSTIC   0x000008000 //HID-Joystick Feature supported
#define GFD_FEAT_STATUS  0x000010000 //Device Status Notify Feature supported
#define GFD_FEAT_MAGANG  0x000080000 //Magnetic Angle Position supported
#define GFD_FEAT_CURRENT   0x000100000 //Motor Current Monitor supported
#define GFD_FEAT_NEUCIRSTAT  0x000200000 //Neucir Status supported
#define GFD_FEAT_EEG       0x000400000 //EEG supported
#define GFD_FEAT_ECG       0x000800000 //ECG supported
#define GFD_FEAT_IMPEDANCE   0x001000000 //Impedance measurement supported
```

### 9.2 setup data types of DataNotify

```java
    int flags = DataNotifFlags.DNF_EULERANGLE | DataNotifFlags.DNF_QUATERNION;

    GF_RET_CODE result = SynchronyProfile.setDataNotifSwitch(flags, new CommandResponseCallback() {
        @Override
        public void onSetCommandResponse(int resp) {
            Log.i("DeviceActivity", "onSetCommandResponse: " + resp);

            if (resp == ResponseResult.RSP_CODE_SUCCESS) {

            } else {

            }
        }
    }, 5);
```

Data type list：

```java
    public class DataNotifFlags {
        public static final int DNF_OFF = 0;
        public static final int DNF_ACCELERATE = 1;
        public static final int DNF_GYROSCOPE = 2;
        public static final int DNF_MAGNETOMETER = 4;
        public static final int DNF_EULERANGLE = 8;
        public static final int DNF_QUATERNION = 16;
        public static final int DNF_ROTATIONMATRIX = 32;
        public static final int DNF_EMG_GESTURE = 64;
        public static final int DNF_EMG_RAW = 128;
        public static final int DNF_HID_MOUSE = 256;
        public static final int DNF_HID_JOYSTICK = 512;
        public static final int DNF_DEVICE_STATUS = 1024;
        public static final int DNF_LOG = 2048;
        public static final int DNF_GEST_EXT = 4096;
        public static final int DNF_MAGANG = 8192;
        public static final int DNF_CURRENT = 16384;
        public static final int DNF_NEUCIR_STAT = 32768;
        public static final int DNF_EEG = 65536;     
        public static final int DNF_ECG = 131072;   
        public static final int DNF_IMPEDANCE = 262144;    
        public static final int DNF_ALL = -1;
    }
```

It's possible to setup multi data type at same time, and check return data type at callback function.

For example: setup impedance with eeg and ecg.


```java
    int flags = DataNotifFlags.DNF_IMPEDANCE | DataNotifFlags.DNF_EEG| DataNotifFlags.DNF_ECG;
```

### 9.3 Get data config
Each data type has get/set config function, for example:
EEG data has getEegDataConfig and setEegDataConfig.

```java
        SynchronyProfile.getEegDataConfig(new CommandResponseCallback() {
            @Override
            public void onGetEegDataConfig(int resp, int sampRate, int channelCount, int channelMask, int sampleCount, int resolutionBits, float microVoltConversionK) {

            }
        }, 5);
```

### 9.3 Start data transfer

For start data transfer, use `startDataNotification` to start.

```java
    DataNotificationCallback notifyCb = new DataNotificationCallback() {
        @Override
        public void onData(byte[] data) {
            // first byte is data type
            if (data[0] == SynchronyProfile.NotifDataType.DNF_IMPEDANCE) {
                Log.i("DeviceActivity","Impedance data: " + Arrays.toString(data));

            } else if (data[0] == SynchronyProfile.NotifDataType.NTF_EEG) {
                Log.i("DeviceActivity","EEG data: " + Arrays.toString(data));

            } else if (...) {
                // etc 
            }
            
            // please process data in other thread to prevent blocking data receive thread
        }
    };
    SynchronyProfile.startDataNotification(DataNotificationCallback notifyCb);

```

data type：

```java
    public class NotifDataType {
        public static final int NTF_ACC_DATA = 1;
        public static final int NTF_GYO_DATA = 2;
        public static final int NTF_MAG_DATA = 3;
        public static final int NTF_EULER_DATA = 4;
        public static final int NTF_QUAT_FLOAT_DATA = 5;
        public static final int NTF_ROTA_DATA = 6;
        public static final int NTF_EMG_GEST_DATA = 7;
        public static final int NTF_EMG_ADC_DATA = 8;
        public static final int NTF_HID_MOUSE = 9;
        public static final int NTF_HID_JOYSTICK = 10;
        public static final int NTF_DEV_STATUS = 11;
        public static final int NTF_LOG_DATA = 12;
        public static final int NTF_MAG_ANGLE_DATA = 13;
        public static final int NTF_MOT_CURRENT_DATA = 14;
        public static final int NTF_NEUCIR_STATUS = 15;
        public static final int NTF_EEG = 16;
        public static final int NTF_ECG = 17;
        public static final int NTF_IMPEDANCE = 18;
        public static final int NTF_PARTIAL_DATA = 255;
    }
```

### 9.3 Stop data transfer

```java
    SynchronyProfile.stopDataNotification();
```
