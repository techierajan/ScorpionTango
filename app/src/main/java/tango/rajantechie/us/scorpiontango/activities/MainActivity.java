package tango.rajantechie.us.scorpiontango.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.erz.joysticklibrary.JoyStick;
import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import de.hdodenhof.circleimageview.CircleImageView;
import tango.rajantechie.us.scorpiontango.CommandsLobot;
import tango.rajantechie.us.scorpiontango.R;
import tango.rajantechie.us.scorpiontango.firebase.BotConfigs;
import tango.rajantechie.us.scorpiontango.usb.UsbCallbacks;
import tango.rajantechie.us.scorpiontango.views.LogcatView;

public class MainActivity extends FirebaseActivity {

private static final String TAG = MainActivity.class.getSimpleName();

private Tango mTango;
private TangoConfig mConfig;

protected String stateString;
private int timesSeenObstacle;
TextView mNameView;
String lastAction = CommandsLobot.REVERSE;
BottomNavigationView bottomNav;
CircleImageView mCircleImageView;
JoyStick mJoyStick;
JoyStick.JoyStickListener mJoyStickListener;
private AtomicInteger mPreviousDirection = new AtomicInteger();
private boolean tangoRunning = false;
private LogcatView logcatView;


public void releaseWakeLock() {
    if (wl != null && wl.isHeld())
        wl.release();
}

public boolean isDisabled() {
    return isDisabled.get();
}

public void setDisabled(boolean isDisabled) {
    Log.d(TAG, "setDisabled: " + isDisabled);
    if (this.isDisabled == null) this.isDisabled = new AtomicBoolean();
    this.isDisabled.set(isDisabled);
}

private AtomicBoolean isDisabled;


@Override
public boolean onKeyDown(int keyCode, KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_BACK) {
        if (getUsbController().isSerialReady()) {
            getUsbController().onPause();
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
    mNameView = (TextView) findViewById(R.id.textview);
    mCircleImageView = (CircleImageView) findViewById(R.id.circular);
    logcatView = (LogcatView) findViewById(R.id.logcat);
    if (getUsbController() != null) getUsbController().setCallbackListener(mCallbacks);

    setupJoystick();

    bottomNav = (BottomNavigationView) findViewById(R.id.navigation_bottom);
    bottomNav.setOnNavigationItemSelectedListener(
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.connect:
                            if (!isSlave()) {
                                startTango();
                                lastAction = CommandsLobot.FORWARD;
                                sendCmd(CommandsLobot.FORWARD);
                            } else {
                                BotConfigs bc = new BotConfigs(CommandsLobot.getNewActionGroup(CommandsLobot.FORWARD), 100, false, lastStateChangeMillis);
                                sendCommand(bc);
                                Snackbar.make(bottomNav, getString(R.string.start), Snackbar.LENGTH_SHORT).show();
                            }
                            break;

                        case R.id.disconnect:
                            if (!isSlave()) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        stopTango();
                                        getUsbController().onPause();
                                    }
                                });
                            } else {
                                BotConfigs bc = new BotConfigs(CommandsLobot.getNewActionGroup(CommandsLobot.STOP), 100, true, lastStateChangeMillis);
                                sendCommand(bc);
                                Snackbar.make(bottomNav, getString(R.string.shutdown), Snackbar.LENGTH_SHORT).show();
                            }

                            break;
                        case R.id.start_tracking:
                            lastAction = CommandsLobot.FORWARD;
                            sendCmd(CommandsLobot.FORWARD);
                            break;
                        case R.id.stop_tracking:
                            lastAction = CommandsLobot.REVERSE;
                            sendCmd(CommandsLobot.REVERSE);
                            break;
                        case R.id.show_track:
                            //// TODO: 4/16/2017
                    }
                    return true;
                }
            });
    // Example of a call to a native method
    //TextView tv = (TextView) findViewById(R.id.sample_text);
    //tv.setText(stringFromJNI());
}

public void setupJoystick() {
    mJoyStick = (JoyStick) findViewById(R.id.joy2);

    if (!isSlave()) {
        mJoyStick.setVisibility(View.GONE);
        return;
    }
    mJoyStickListener = new JoyStick.JoyStickListener() {
        @Override
        public void onMove(JoyStick joyStick, double angle, double power, int direction) {
            if ((System.currentTimeMillis() - lastStateChangeMillis < (2000)))
                return;
            if (mPreviousDirection.get() != direction && direction != -1) {
                mPreviousDirection.set(direction);
                sendCmd(translateCommand(direction));
                Log.d(TAG, "onMove: Sending Cur=" + direction + " Prev=" + mPreviousDirection.get());
            } else {
                Log.d(TAG, "onMove: same as before");
            }
        }

        @Override
        public void onTap() {

        }

        @Override
        public void onDoubleTap() {

        }
    };
    mJoyStick.setListener(mJoyStickListener);
}

public String translateCommand(int direction) {
      /*1. DIRECTION_CENTER = -1
            2. DIRECTION_LEFT = 0
            3. DIRECTION_LEFT_UP = 1
            4. DIRECTION_UP = 2
            5. DIRECTION_UP_RIGHT = 3
            6. DIRECTION_RIGHT = 4
            7. DIRECTION_RIGHT_DOWN = 5
            8. DIRECTION_DOWN = 6
            9. DIRECTION_DOWN_LEFT = 7*/
    switch (direction) {
        case -1:
            return CommandsLobot.STOP;
        case 0:
            return CommandsLobot.LEFTTURN;
        case 1: //new action needs to be created for this
            return CommandsLobot.LEFTTURN;
        case 2:
            return CommandsLobot.FORWARD;
        case 3:
            return CommandsLobot.RIGHTTURN;
        case 4:
            return CommandsLobot.RIGHTTURN;
        case 5:
            return CommandsLobot.RIGHTTURN;
        case 6:
            return CommandsLobot.REVERSE;
        case 7:
            return CommandsLobot.LEFTTURN;
        default:
            return CommandsLobot.STOP;
    }
}

@Override
protected void onResume() {
    super.onResume();
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            if (getUsername().length() > 0) mNameView.setText(getUsername());
            if (getPhotoUrl().length() > 0) loadPhoto(getPhotoUrl(), mCircleImageView);
        }
    });
    createListeners();
}


public void startTango() {
    if (isSlave()) return;
    Toast.makeText(this, "Starting tango", Toast.LENGTH_SHORT).show();
    Runnable posReadyRunnable = new Runnable() {
        // Pass in a Runnable to be called from UI thread when Tango is ready,
        // this Runnable will be running on a new thread.
        // When Tango is ready, we can call Tango functions safely here only
        // when there is no UI thread changes involved.
        @Override
        public void run() {
            synchronized (MainActivity.this) {
                tangoRunning = true;
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
    };
    try {
        mTango = new Tango(MainActivity.this, posReadyRunnable);
    } catch (Exception e) {
        Log.d(TAG, "startTango: loading tango failed switching to slave mode");
    }
}

/**
 * Sets up the tango configuration object. Make sure mTango object is initialized before
 * making this call.
 */
private TangoConfig setupTangoConfig(Tango tango) {
    // Create a new Tango Configuration and enable the Depth Sensing API.
    TangoConfig config;
    config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
    config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);
    return config;
}

@Override
protected void onPause() {
    super.onPause();
    if (tangoRunning)
    stopTango();
}

public void stopTango() {
    if (isSlave()) return;
    Toast.makeText(this, "Stopping Tango", Toast.LENGTH_SHORT).show();
    tangoRunning = false;
    synchronized (this) {
        if (mTango == null) return;
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
    switch (item.getItemId()) {
        case R.id.sign_out_menu:
//            signOut();
            return true;
        //noinspection SimplifiableIfStatement
        case R.id.togglelogs:
            if (logcatView.getVisibility() == View.VISIBLE)
                logcatView.setVisibility(View.GONE);
            else logcatView.setVisibility(View.VISIBLE);
            return true;
        default:
            return super.onOptionsItemSelected(item);
    }
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
                new ArrayList<>();
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
@SuppressLint("DefaultLocale")
private void updatePosition(TangoXyzIjData xyzIjData) {
    //check for obstacles only if moving forward
    if (!lastAction.equals(CommandsLobot.FORWARD)) {
        if ((System.currentTimeMillis() - lastStateChangeMillis < (2 * stateChangeTimeoutMs)))
            return;
        Log.d(TAG, "updatePosition: return1 " + lastAction);
    }

    //check for obstacles only if at least 1s passed since last state change
    if (System.currentTimeMillis() - lastStateChangeMillis < stateChangeTimeoutMs) {
        Log.d(TAG, "updatePosition: return2");
        stateString = "waiting";
        return;
    }
    if (isDisabled()) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stopTango();
            }
        });
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
    String cmd;

    if (timesSeenObstacle > 1 && timesSeenObstacle < 10) {
        cmd = CommandsLobot.LEFTTURN;
        lastAction = CommandsLobot.LEFTTURN;
        stateString = String.format("Turning left (%.3f,%.3f)", minZ, minParaZ);
    } else if (timesSeenObstacle > 9) {
        cmd = CommandsLobot.REVERSE;
        lastAction = CommandsLobot.REVERSE;
        stateString = "Go backward";
    } else {
        cmd = CommandsLobot.FORWARD;
        lastAction = CommandsLobot.FORWARD;
        stateString = "Go forward";
    }
    Log.d(TAG, "updatePosition: 3" + stateString + lastAction);
    sendCmd(cmd);
}

public void sendCmd(String c) {
    super.sendCmd(c);
}

public void createListeners() {
    attachListener(
            new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    if (isSlave()) {
                        return;
                    }
                    for (DataSnapshot snap : dataSnapshot.getChildren()) {
                        if (snap.getKey().equals(LASTCOMMAND)) {
                            Log.d(TAG, "onDataChange1: " + snap.getValue());
                            sendCmd(snap.getValue().toString());
                        } else if (snap.getKey().equals(DISBALE)) {
                            try {
                                Log.d(TAG, "onDataChange2: " + snap.getValue());
                                final boolean isDisabled = Boolean.parseBoolean(snap.getValue().toString());
                                setDisabled(isDisabled);
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (tangoRunning && isDisabled){
                                            stopTango();
                                        }
                                        else if (!tangoRunning && !isDisabled) {
                                            startTango();
                                        }
                                    }
                                });
                            } catch (Exception e) {
                            }
                        }
                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
}


}
