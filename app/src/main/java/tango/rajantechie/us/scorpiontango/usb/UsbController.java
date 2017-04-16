package tango.rajantechie.us.scorpiontango.usb;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;

/**
 * Created by rajan on 4/16/2017.
 */

public class UsbController {

String ACTION_USB_PERMISSION = "rajantechie.us.scorpiontango";
static int BAUD_RATE = 115200;
UsbManager mUsbManager;
UsbDevice mUsbDevice;
UsbSerialDevice mUsbSerialPort;
UsbDeviceConnection mUsbDeviceConnection;
UsbCallbacks mCallbacks;
boolean serialReady = false;


public UsbController(Context context) {
    mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
}

public void onStart(){
    IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_USB_PERMISSION);
    filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
    filter.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
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

private final BroadcastReceiver usbBroadcast = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
            boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
            if (granted) {
                createSerialPort();
            }
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
        }
    }
}

}
