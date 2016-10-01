package com.flynnie.flightpark;

import android.app.Activity;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import org.gearvrf.GVRActivity;
import org.gearvrf.GVRMain;

public class FlightParkActivity extends GVRActivity
{
	private FlightParkMain mMain;
	private long lastClickMillis;
	private static final long THRESHOLD_MILLIS = 500L;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mMain = new FlightParkMain();
        setMain(mMain, "gvr.xml");
    }
    
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
    	long now = SystemClock.elapsedRealtime();
    	if(now - lastClickMillis > THRESHOLD_MILLIS) {
    		lastClickMillis = now;
    		mMain.processKeyEvent(event.getKeyCode());
    	}
    	return true;
    }
    
    @Override
    public boolean dispatchGenericMotionEvent(MotionEvent event) {
        // Check that the event came from a game controller
        if ((event.getSource() & InputDevice.SOURCE_JOYSTICK) ==
                InputDevice.SOURCE_JOYSTICK &&
                event.getAction() == MotionEvent.ACTION_MOVE) {

            // Process all historical movement samples in the batch
            final int historySize = event.getHistorySize();

            // Process the movements starting from the
            // earliest historical position in the batch
            for (int i = 0; i < historySize; i++) {
                // Process the event at historical position i
                mMain.processJoystickInput(event, i);
            }

            // Process the current movement sample in the batch (position -1)
            mMain.processJoystickInput(event, -1);
            return true;
        }
        return super.dispatchGenericMotionEvent(event);
    }
}
