package tango.rajantechie.us.scorpiontango;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;

import java.util.ArrayList;

import tango.rajantechie.us.scorpiontango.usb.UsbCallbacks;
import tango.rajantechie.us.scorpiontango.usb.UsbController;

public class MainActivity extends AppCompatActivity {

private static final String TAG = MainActivity.class.getSimpleName();
private static final int WAKLOCK = 109;
public static final int VENDOR_ID = 6790;
private UsbController mUsbController;

private Tango mTango;
private TangoConfig mConfig;

protected String stateString;
private long lastStateChangeMillis;
private static final int stateChangeTimeoutMs = 500;
private int timesSeenObstacle;
TextView textView;
public static PowerManager pm;
public static PowerManager.WakeLock wl;
String lastAction = CommandsLobot.REVERSE;
BottomNavigationView bottomNav;

public void checkAcquireWakeLock() {

    if (ContextCompat.checkSelfPermission(this,
            Manifest.permission.WAKE_LOCK)
            != PackageManager.PERMISSION_GRANTED) {

        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.READ_CONTACTS},
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

public void releaseWakeLock() {
    if (wl != null && wl.isHeld())
        wl.release();
}

@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
        if (mUsbController.isSerialReady()) {
            mUsbController.onPause();
            return false;
        }
    }
    return super.onKeyDown(keyCode, event);
}

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    textView = (TextView) findViewById(R.id.textview);
    textView.setText("hellow");
    mUsbController = new UsbController(this);

    mUsbController.setCallbackListener(mCallbacks);
    mUsbController.setVendorID(VENDOR_ID);

    bottomNav = (BottomNavigationView) findViewById(R.id.navigation_bottom);

    bottomNav.setOnNavigationItemSelectedListener(
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.connect:
                            lastAction=CommandsLobot.FORWARD;
                            sendCmd(CommandsLobot.getNewActionGroup(CommandsLobot.FORWARD).getBytes());
                            break;

                        case R.id.disconnect:
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    mUsbController.onPause();
                                }
                            });
                            break;
                        case R.id.start_tracking:
                            lastAction = CommandsLobot.FORWARD;
                            sendCmd(CommandsLobot.getNewActionGroup(CommandsLobot.FORWARD).getBytes());
                            break;
                        case R.id.stop_tracking:
                            lastAction = CommandsLobot.REVERSE;
                            sendCmd(CommandsLobot.getNewActionGroup(CommandsLobot.REVERSE).getBytes());
                            break;
                        case R.id.show_track:
                            //// TODO: 4/16/2017
                    }
                    return true;
                }
            });
    FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
    fab.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Snackbar.make(view, "Disconnecting everything",
                    Snackbar.LENGTH_LONG).show();
        }
    });


    // Example of a call to a native method
    //TextView tv = (TextView) findViewById(R.id.sample_text);
    //tv.setText(stringFromJNI());
}

@Override
protected void onResume() {
    super.onResume();
    mTango = new Tango(MainActivity.this, new Runnable() {
        // Pass in a Runnable to be called from UI thread when Tango is ready,
        // this Runnable will be running on a new thread.
        // When Tango is ready, we can call Tango functions safely here only
        // when there is no UI thread changes involved.
        @Override
        public void run() {
            synchronized (MainActivity.this) {
                mConfig = setupTangoConfig(mTango);

                try {
                    setTangoListeners();
                } catch (TangoErrorException e) {
                    Log.e(TAG, "TangoErrorException1");
                } catch (SecurityException e) {
                    Log.e(TAG, "TANGO PERMISSION EXCEPTION");
                }
                try {
                    mTango.connect(mConfig);
                } catch (TangoOutOfDateException e) {
                    Log.e(TAG, "OUT OF DATE");
                } catch (TangoErrorException e) {
                    Log.e(TAG, "TANGO ERROR");
                }
            }
        }
    });
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            mUsbController.onStart();
        }
    });
}

/**
 * Sets up the tango configuration object. Make sure mTango object is initialized before
 * making this call.
 */
private TangoConfig setupTangoConfig(Tango tango) {
    // Create a new Tango Configuration and enable the Depth Sensing API.
    TangoConfig config = new TangoConfig();
    config = tango.getConfig(config.CONFIG_TYPE_DEFAULT);
    config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
    return config;
}

@Override
protected void onPause() {
    super.onPause();
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            mUsbController.onPause();
        }
    });
    synchronized (this) {
        try {
             mTango.disconnect();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
}

@Override
public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    getMenuInflater().inflate(R.menu.menu_main, menu);
    return true;
}

@Override
public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    int id = item.getItemId();

    //noinspection SimplifiableIfStatement
    if (id == R.id.action_settings) {
        return true;
    }
    return super.onOptionsItemSelected(item);
}

UsbCallbacks mCallbacks = new UsbCallbacks() {
    @Override
    public void onSerialRecieved(byte[] bytes) {
        Log.d(TAG, "onSerialRecieved: " + (bytes.length > 0 ? new String(bytes) : null));
    }

    @Override
    public void onFail(FAILURE fail) {
        Log.d(TAG, "onFail: ");
    }

    @Override
    public void onDeviceDetached() {
        Log.d(TAG, "onDeviceDetached: ");
        releaseWakeLock();
    }

    @Override
    public void onSerialAttached() {
        Log.d(TAG, "onSerialAttached: ");
        checkAcquireWakeLock();
    }
};

private void setTangoListeners() {
    {
        timesSeenObstacle = 0;
        // Lock configuration and connect to Tango.
        // Select coordinate frame pair.
        final ArrayList<TangoCoordinateFramePair> framePairs =
                new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));

        // Listen for new Tango data
        mTango.connectListener(framePairs, new Tango.OnTangoUpdateListener() {
            @Override
            public void onPoseAvailable(final TangoPoseData pose) {
                // We are not using TangoPoseData for this application.
            }

            @Override
            public void onXyzIjAvailable(final TangoXyzIjData xyzIjData) {
                updatePosition(xyzIjData);
            }

            @Override
            public void onTangoEvent(final TangoEvent event) {
                // Ignoring TangoEvents.
            }

            @Override
            public void onFrameAvailable(int cameraId) {
                // We are not using onFrameAvailable for this application.
            }
        });
    }
}

/**
 * Analyze point cloud data and decide if we should turn to avoid collision
 */
private void updatePosition(TangoXyzIjData xyzIjData) {
    //check for obstacles only if moving forward
    if (lastAction != CommandsLobot.FORWARD) {
        if((System.currentTimeMillis() - lastStateChangeMillis < (2*stateChangeTimeoutMs)))
        return;
        Log.d(TAG, "updatePosition: return1 "+lastAction);
    }

    //check for obstacles only if at least 1s passed since last state change
    if (System.currentTimeMillis() - lastStateChangeMillis < stateChangeTimeoutMs) {
        Log.d(TAG, "updatePosition: return2");
        stateString="waiting";
        return;
    }

    float X, Y, Z;
    float minZ = 100500;
    float minParaZ = 100500;
    int cntPointsTooClose = 0;

    for (int i = 0; i < xyzIjData.xyz.capacity() - 3; i = i + 3) {
        X = xyzIjData.xyz.get(i);
        Y = xyzIjData.xyz.get(i + 1);
        Z = xyzIjData.xyz.get(i + 2);
        minZ = Math.min(minZ, Z);
        if (Z > (6 * Math.pow(X, 2) + 10 * Math.pow(Y + 0.1, 2))) {
            minParaZ = Math.min(minParaZ, Z);
            if (Z < 0.5) {
                if (++cntPointsTooClose > 5) {
                    timesSeenObstacle++;
                    break;
                }
            }
        }
    }
    if (cntPointsTooClose <= 5) {
        timesSeenObstacle = 0;
    }
    byte[] cmd;

    if (timesSeenObstacle > 1 && timesSeenObstacle < 10) {
        cmd = CommandsLobot.getNewActionGroup(CommandsLobot.LEFTTURN).getBytes();
        lastAction = CommandsLobot.LEFTTURN;
        stateString = String.format("Turning left (%.3f,%.3f)", minZ, minParaZ);
    } else if (timesSeenObstacle > 9) {
        cmd = CommandsLobot.getNewActionGroup(CommandsLobot.REVERSE).getBytes();
        lastAction = CommandsLobot.REVERSE;
        stateString = "Go backward";
    } else {
        cmd = CommandsLobot.getNewActionGroup(CommandsLobot.FORWARD).getBytes();
        lastAction = CommandsLobot.FORWARD;
        stateString = "Go forward";
    }
    Log.d(TAG, "updatePosition: 3"+stateString+lastAction);
    sendCmd(cmd);
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            textView.setText(stateString);
        }
    });

}

private void sendCmd(byte[] c) {
    try {
        if (mUsbController != null && mUsbController.isSerialReady()) {
            mUsbController.sendSerialData(c);
            lastStateChangeMillis = System.currentTimeMillis();
        } else {
            mUsbController.onStart();
        }
    } catch (Exception e) {
        Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
    }
}

}
