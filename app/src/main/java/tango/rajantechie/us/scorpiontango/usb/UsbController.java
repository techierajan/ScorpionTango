package tango.rajantechie.us.scorpiontango.usb;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import tango.rajantechie.us.scorpiontango.CommandsLobot;

/**
 * Created by rajan on 4/16/2017.
 */

public class UsbController {

private static final String TAG = UsbController.class.getSimpleName();
String ACTION_USB_PERMISSION = "tango.rajantechie.us.scorpiontango.USB_PERMISSION";
static int BAUD_RATE = 115200;
UsbManager mUsbManager;
UsbDevice mUsbDevice;
UsbSerialDevice mUsbSerialPort;
UsbDeviceConnection mUsbDeviceConnection;
UsbCallbacks mCallbacks;
boolean serialReady = false;
Context mContext;
private int mVendorID;

public void setVendorID(int id) {
    this.mVendorID = id;
}

public void log(String message) {
    Log.e(TAG, message);
}

public UsbController(Context context) {
    this.mContext = context;
    mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
}

public void setCallbackListener(UsbCallbacks callbacks) {
    this.mCallbacks = callbacks;
}

public void onStart() {
    if (serialReady) return;
    IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
    mContext.registerReceiver(mUsbBroadcast, filter);
    init();
}

public boolean isSerialReady() {
    return serialReady;
}

public void onPause() {
    if (serialReady) {
        sendSerialData(CommandsLobot.STOP.getBytes());
        closeSerialport();
    }
    try {
        mContext.unregisterReceiver(mUsbBroadcast);
    } catch (Exception e) {
    }

    serialReady = false;
}


public void init() {
    HashMap<String, UsbDevice> usbDevices = mUsbManager.getDeviceList();
    if (!usbDevices.isEmpty()) {
        for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
            mUsbDevice = entry.getValue();
            int deviceVID = mUsbDevice.getVendorId();
            if (deviceVID == mVendorID)//Arduino Vendor ID
            {
                PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, new Intent(ACTION_USB_PERMISSION), 0);
                mUsbManager.requestPermission(mUsbDevice, pi);
                log("found requesting");
                return;
            } else {
                mUsbDeviceConnection = null;
                mUsbDevice = null;
            }
        }
    }
}


public void closeSerialport() {
    if (mUsbSerialPort != null)
        mUsbSerialPort.close();
    serialReady = false;
}

public boolean sendSerialData(byte[] data) {
    if (serialReady) mUsbSerialPort.write(data);
    else return false;
    return true;
}

UsbSerialInterface.UsbReadCallback mReadCallback = new UsbSerialInterface.UsbReadCallback() {
    @Override
    public void onReceivedData(byte[] bytes) {
        mCallbacks.onSerialRecieved(bytes);
    }
};

public String serialToText(byte[] args) {
    try {
        String data = new String(args, "UTF-8");
        return data.concat("/n");
    } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
    }
    return null;
}

private final BroadcastReceiver mUsbBroadcast = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
            boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
            if (granted) {
                createSerialPort();
            } else {
                mCallbacks.onFail(UsbCallbacks.FAILURE.PERMISSION_NOT_GRANTED);
                log("no permission for serial");
            }
        } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
            mCallbacks.onDeviceDetached();
        } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
            log("here");
            init();
            mCallbacks.onSerialAttached();
        }
    }
};

private void createSerialPort() {
    mUsbDeviceConnection = mUsbManager.openDevice(mUsbDevice);
    mUsbSerialPort = UsbSerialDevice.createUsbSerialDevice(mUsbDevice, mUsbDeviceConnection);
    if (mUsbSerialPort != null) {
        if (mUsbSerialPort.open()) {
            serialReady = true;
            mUsbSerialPort.setBaudRate(BAUD_RATE);
            mUsbSerialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
            mUsbSerialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
            mUsbSerialPort.setParity(UsbSerialInterface.PARITY_NONE);
            mUsbSerialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
            mUsbSerialPort.read(mReadCallback);
            log("serial port is now open");
            return;
        } else {
            mCallbacks.onFail(UsbCallbacks.FAILURE.PORT_NOT_OPEN);
            log("serial port is not open");
        }
    } else {
        mCallbacks.onFail(UsbCallbacks.FAILURE.PORT_NULL);
        log("serial port null");
    }
    serialReady = false;
}

}
