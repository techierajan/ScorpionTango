package tango.rajantechie.us.scorpiontango.activities;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.google.atap.tangoservice.Tango;

import tango.rajantechie.us.scorpiontango.usb.UsbController;

/**
 * Created by rajan on 4/20/2017.
 * handles core functions - controls usb lifecycle and other stuff
 */

public class BaseActivity extends AppCompatActivity {

public static final int WAKLOCK = 109;
public static final int stateChangeTimeoutMs = 500;
public static PowerManager.WakeLock wl;
private UsbController mUsbController;
public static final int VENDOR_ID = 6790;
private boolean isSlave=true;
public long lastStateChangeMillis;



@Override
protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mUsbController = new UsbController(this);
    mUsbController.setVendorID(VENDOR_ID);
    if(Tango.getVersion(this)>0)
    {
        isSlave=false;
    }
}

public boolean isSlave()
{
    return isSlave;
}

@Override
protected void onResume() {
    super.onResume();
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            mUsbController.onStart();
        }
    });
}

@Override
protected void onStop() {
    super.onStop();
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            mUsbController.onPause();
        }
    });
}

public void checkAcquireWakeLock() {

    if (ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.WAKE_LOCK)
            != PackageManager.PERMISSION_GRANTED) {

        ActivityCompat.requestPermissions(this,
                new String[]{android.Manifest.permission.READ_CONTACTS},
                WAKLOCK);
    } else {
        acquireWakeLock();
    }
}

public void acquireWakeLock() {
    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "My Tag");
    wl.acquire();
}

@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == WAKLOCK && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        acquireWakeLock();
    }
}

public UsbController getUsbController() {
    return mUsbController;
}

}
