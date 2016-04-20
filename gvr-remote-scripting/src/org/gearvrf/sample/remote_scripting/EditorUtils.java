/* Copyright 2015 Samsung Electronics Co., LTD
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gearvrf.sample.remote_scripting;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRPostEffect;
import org.gearvrf.GVRCameraRig;
import org.gearvrf.scene_objects.GVRViewSceneObject;
import org.gearvrf.scene_objects.view.GVRFrameLayout;
import org.gearvrf.GVRBaseSensor;
import org.gearvrf.SensorEvent;
import org.gearvrf.ISensorEvents;
import java.lang.Runnable;

import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.view.View;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;

public class EditorUtils {
    private GVRContext gvrContext;
    private boolean inflated = false;
    private GVRViewSceneObject layoutSceneObject;
    private GearVRScripting activity;
    private GVRFrameLayout frameLayout;
    private int frameWidth;
    private int frameHeight;
    private Handler mainThreadHandler;
    private final static PointerProperties[] pointerProperties;
    private final static PointerCoords[] pointerCoordsArray;
    private final static PointerCoords pointerCoords;
    private static final int KEY_EVENT = 1;

    private static final float QUAD_X = 1.0f;
    private static final float QUAD_Y = 1.0f;
    private static final float HALF_QUAD_X = QUAD_X / 2.0f;
    private static final float HALF_QUAD_Y = QUAD_Y / 2.0f;
    private static final float DEPTH = -1.5f;

    private TextView updateButton;

    static {
        PointerProperties properties = new PointerProperties();
        properties.id = 0;
        properties.toolType = MotionEvent.TOOL_TYPE_MOUSE;
        pointerProperties = new PointerProperties[] { properties };
        pointerCoords = new PointerCoords();
        pointerCoordsArray = new PointerCoords[] { pointerCoords };
    }

    public EditorUtils(GVRContext context) {
        gvrContext = context;
        activity = (GearVRScripting) context.getActivity();
    }

    public void inflate() {
        frameLayout = new GVRFrameLayout(activity);
        frameLayout.setDrawingCacheEnabled(false);
        View.inflate(activity, R.layout.main, frameLayout);

        final EditText editor = (EditText) frameLayout.findViewById(R.id.editor);
        editor.requestFocus();
        editor.setDrawingCacheEnabled(false);
        editor.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    editor.invalidate();
                    frameLayout.invalidate();
                    frameLayout.requestLayout();
                }

            });


        updateButton = (TextView) frameLayout.findViewById(R.id.update);
        updateButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    android.util.Log.d("Editor", "update was clicked");
                    // get text
                    // execute script
                }
            });

        mainThreadHandler = new Handler(activity.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == KEY_EVENT) {
                    KeyEvent keyEvent = (KeyEvent) msg.obj;
                    frameLayout.dispatchKeyEvent(keyEvent);
                    frameLayout.invalidate();
                    frameLayout.requestLayout();
                    android.util.Log.d("Editor", "key: " + keyEvent);
                } else {
                    // dispatch motion event
                    MotionEvent motionEvent = (MotionEvent) msg.obj;
                    frameLayout.dispatchTouchEvent(motionEvent);
                    frameLayout.invalidate();
                    frameLayout.requestLayout();
                    motionEvent.recycle();
                }
            }
        };

        inflated = true;
    }

    public void show() {
        if(!inflated) {
            activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        inflate();
                    }
                });
        }

        while(!inflated) {
            SystemClock.sleep(500);
        }

        if(layoutSceneObject != null) {
            gvrContext.getMainScene().addSceneObject(layoutSceneObject);
            return;
        }

        layoutSceneObject = new GVRViewSceneObject(gvrContext, frameLayout, gvrContext.createQuad(QUAD_X, QUAD_Y));

        layoutSceneObject.getTransform().setPosition(0.0f, 0.0f, DEPTH);

        frameWidth = frameLayout.getWidth();
        frameHeight = frameLayout.getHeight();

        GVRBaseSensor sensor = new GVRBaseSensor(gvrContext);
        layoutSceneObject.getEventReceiver().addListener(sensorEvents);
        layoutSceneObject.setSensor(sensor);

        gvrContext.getMainScene().addSceneObject(layoutSceneObject);
    }

    public void hide() {
        gvrContext.getMainScene().removeSceneObject(layoutSceneObject);
    }

    private ISensorEvents sensorEvents = new ISensorEvents() {
        @Override
        public void onSensorEvent(final SensorEvent event) {
            final int action;
            final KeyEvent keyEvent = event.getCursorController().getKeyEvent();
            if (keyEvent == null) {
                return;
            }

            action = keyEvent.getAction();
            float[] hitPoint = event.getHitPoint();
            float x = (hitPoint[0] + HALF_QUAD_X) / QUAD_X;
            float y = -(hitPoint[1] + HALF_QUAD_Y) / QUAD_Y;
            pointerCoords.x = x * frameWidth;
            pointerCoords.y = y * frameHeight;
            long now = SystemClock.uptimeMillis();
            final MotionEvent clone = MotionEvent.obtain(now, now+1, action, 1, pointerProperties, pointerCoordsArray, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);
            Message message = Message.obtain(mainThreadHandler, 0, 0, 0, clone);
            mainThreadHandler.sendMessage(message);
        }

    };

}

