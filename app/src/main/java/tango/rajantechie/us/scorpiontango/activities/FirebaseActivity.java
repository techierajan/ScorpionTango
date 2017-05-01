package tango.rajantechie.us.scorpiontango.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import tango.rajantechie.us.scorpiontango.CommandsLobot;
import tango.rajantechie.us.scorpiontango.firebase.BotConfigs;

/**
 * Created by rajan on 4/20/2017.
 */

public class FirebaseActivity extends BaseActivity {


public static final String BOTROOT = "botconfig";
public static final String LASTCOMMAND = "lastcommand";
public static final String DISBALE = "disable";
private static final String TAG = FirebaseActivity.class.getSimpleName();
private DatabaseReference mDatabaseReference;

//private FirebaseDatabase mFirebaseInstance;
//private GoogleApiClient mGoogleApiClient;

private FirebaseUser mFireBaseUser;
private String mUsername;
private String mPhotoUrl;


@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    FirebaseAuth mFireBaseAuth = FirebaseAuth.getInstance();
    mFireBaseUser = mFireBaseAuth.getCurrentUser();

    if (mFireBaseUser == null) {
        Log.d(TAG, "onCreate: firebase user null");
        // Not signed in, launch the Sign In activity
        startActivity(new Intent(this, SignInActivity.class));
        finish();
        return;
    } else {
        mUsername = mFireBaseUser.getDisplayName();
        if (mFireBaseUser.getPhotoUrl() != null) {
            mPhotoUrl = mFireBaseUser.getPhotoUrl().toString();
        }
    }
    mDatabaseReference = FirebaseDatabase.getInstance().getReference();
}

@Override
protected void onResume() {
    super.onResume();
}

public String getUsername() {
    return mUsername;
}

public String getPhotoUrl() {
    return mPhotoUrl;
}

public void sendCommand(BotConfigs bc) {
    mDatabaseReference.child(BOTROOT).setValue(bc);
}

public void loadPhoto(String url, ImageView imageView) {
    Glide.with(this)
            .load(url)
            .into(imageView);
}

public void attachListener(ValueEventListener listener) {
    mDatabaseReference.child(BOTROOT).addValueEventListener(listener);
}

public void sendCmd(String cmd) {
    Log.d(TAG, "sendCmd: %s" + cmd);
    lastStateChangeMillis = System.currentTimeMillis();
    if (isSlave()) {
        BotConfigs bc = new BotConfigs(CommandsLobot.getNewActionGroup(cmd), 100, false, lastStateChangeMillis);
        sendCommand(bc);
    }
    final byte[] c = CommandsLobot.getNewActionGroup(cmd).getBytes();
    runOnUiThread(new Runnable() {
        @Override
        public void run() {
            try {
                if (getUsbController() == null) return;

                if (getUsbController().isSerialReady()) {
                    getUsbController().sendSerialData(c);
                } else {
                    getUsbController().onStart();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
            }
        }
    });

}

}

