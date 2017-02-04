package com.pulkit4tech.scroozer;

import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import android.util.Log;

import static com.pulkit4tech.scroozer.Constants.DEBUGGER;
import static com.pulkit4tech.scroozer.Constants.MY_SHARED_PREFERENCE;
import static com.pulkit4tech.scroozer.Constants.STATE_ACTIVE;
import static com.pulkit4tech.scroozer.Constants.WAKE_LOCK_TAG;

public class MainTileService extends TileService {

    private PowerManager.WakeLock wakeLock;
    private BroadcastReceiver screenOnOffReceiver;
    int i = 0;

    @Override
    public void onTileRemoved() {
        if (screenOnOffReceiver != null) {
            // Unregister BroadcastReciever
            getApplicationContext().unregisterReceiver(screenOnOffReceiver);
            screenOnOffReceiver = null;
        }
        Log.d(DEBUGGER, "Called : onTileRemoved");
    }

    @Override
    public void onTileAdded() {
        registerBroadcastReceiver();
        writeToInactiveState();
        Log.d(DEBUGGER, "Called : onTileAdded");
    }

    @Override
    public void onStartListening() {
        if (isStateActive()) {
            transitTileToActiveState();
        } else {
            transitTileToInactiveState();
        }
        Log.d(DEBUGGER, "Called : onStartListening");
    }

    private boolean isStateActive() {
        return getSharedPreferences(MY_SHARED_PREFERENCE, Context.MODE_PRIVATE).getBoolean(STATE_ACTIVE, false);
    }

    @Override
    public void onStopListening() {
        if (isStateActive()) {
            transitTileToActiveState();
        } else {
            transitTileToInactiveState();
        }
        Log.d(DEBUGGER, "Called : onStopListening");
    }

    @Override
    public void onClick() {
        transitTileToActiveState();
        Log.d(DEBUGGER, "Called : onClick");
    }

    private void transitTileToActiveState() {
        Tile tile = getQsTile();
        if (tile != null && tile.getState() == Tile.STATE_INACTIVE) {
            tile.setState(Tile.STATE_ACTIVE);
            tile.updateTile();

            //acquire lock
            acquireWakeLock();

            //write state to sharedpreference
            writeToActiveState();
            Log.d(DEBUGGER, "Transit to ACTIVE STATE");
        }
    }

    private void transitTileToInactiveState() {
        Tile tile = getQsTile();
        if (tile != null && tile.getState() == Tile.STATE_ACTIVE) {
            tile.setState(Tile.STATE_INACTIVE);
            tile.updateTile();

            //release lock
            releaseWakeLock();

            //write state to sharedpreference
            writeToInactiveState();
            Log.d(DEBUGGER, "Transit to INACTIVE STATE");
        }
    }

    private void acquireWakeLock() {
        wakeLock = ((PowerManager) getSystemService(Context.POWER_SERVICE)).newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, WAKE_LOCK_TAG);
        wakeLock.acquire();
        Log.d(DEBUGGER, "Wake Lock Acquired");
    }

    private void releaseWakeLock() {
        if (wakeLock != null) {
            wakeLock.release(PowerManager.SCREEN_BRIGHT_WAKE_LOCK);
            Log.d(DEBUGGER, "Wake Lock Released");
        }
    }

    private void registerBroadcastReceiver() {
        final IntentFilter theFilter = new IntentFilter();
        /** System Defined Broadcast */
        theFilter.addAction(Intent.ACTION_SCREEN_ON);
        theFilter.addAction(Intent.ACTION_SCREEN_OFF);

        screenOnOffReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String strAction = intent.getAction();

                if (strAction.equals(Intent.ACTION_SCREEN_OFF)) {
                    writeToInactiveState();
                    // releaseWakeLock();
                    Log.d(DEBUGGER, "Screen is Locked");
                } else if (strAction.equals(Intent.ACTION_SCREEN_ON)) {
//                    TileService.requestListeningState(getApplicationContext(),new ComponentName(getApplicationContext(),MainTileService.class));
//               releaseWakeLock();
                    // TODO : Add proper function to releasewake lock
                    Log.d(DEBUGGER, "Screen is Unlocked");
                }
            }
        };

        getApplicationContext().registerReceiver(screenOnOffReceiver, theFilter);
    }

    private void writeToActiveState() {
        SharedPreferences pref = getSharedPreferences(MY_SHARED_PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(STATE_ACTIVE, true);
        editor.apply();
    }

    private void writeToInactiveState() {
        SharedPreferences pref = getSharedPreferences(MY_SHARED_PREFERENCE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(STATE_ACTIVE, false);
        editor.apply();
    }
}
