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
        View.inflate(activity, R.layout.main, frameLayout);

        EditText editor = (EditText) frameLayout.findViewById(R.id.editor);
        editor.requestFocus();
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
                    android.util.Log.d("Editor", "key: " + keyEvent);
                } else {
                    // dispatch motion event
                    MotionEvent motionEvent = (MotionEvent) msg.obj;
                    frameLayout.dispatchTouchEvent(motionEvent);
                    frameLayout.invalidate();
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
        layoutSceneObject.getEventReceiver().addListener(eventListener);
        layoutSceneObject.setSensor(sensor);

        gvrContext.getMainScene().addSceneObject(layoutSceneObject);
    }

    public void hide() {
        gvrContext.getMainScene().removeSceneObject(layoutSceneObject);
    }

    private ISensorEvents eventListener = new ISensorEvents() {
        private static final float SCALE = 10.0f;
        private float savedMotionEventX, savedMotionEventY, savedHitPointX,
                savedHitPointY;

        @Override
        public void onSensorEvent(SensorEvent event) {
            final MotionEvent motionEvent = event.getCursorController()
                    .getMotionEvent();
            if (motionEvent != null
                    && motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                pointerCoords.x = savedHitPointX
                        + ((motionEvent.getX() - savedMotionEventX) * SCALE);
                pointerCoords.y = savedHitPointY
                        + ((motionEvent.getY() - savedMotionEventY) * SCALE);

                final MotionEvent clone = MotionEvent.obtain(
                        motionEvent.getDownTime(), motionEvent.getEventTime(),
                        motionEvent.getAction(), 1, pointerProperties,
                        pointerCoordsArray, 0, 0, 1f, 1f, 0, 0,
                        InputDevice.SOURCE_TOUCHSCREEN, 0);

                Message message = Message.obtain(mainThreadHandler, 0, 0, 0,
                        clone);
                mainThreadHandler.sendMessage(message);

            } else {
                KeyEvent keyEvent = event.getCursorController().getKeyEvent();

                if (keyEvent == null) {
                    return;
                }

                android.util.Log.d("Editor", "got key event");
                float[] hitPoint = event.getHitPoint();

                pointerCoords.x = (hitPoint[0] + HALF_QUAD_X) * frameWidth;
                pointerCoords.y = -(hitPoint[1] - HALF_QUAD_Y) * frameHeight;

                if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                    if (motionEvent != null) {
                        // save the co ordinates on down
                        savedMotionEventX = motionEvent.getX();
                        savedMotionEventY = motionEvent.getY();
                    }
                    savedHitPointX = pointerCoords.x;
                    savedHitPointY = pointerCoords.y;
                }

                MotionEvent clone = getMotionEvent(keyEvent.getDownTime(),
                        keyEvent.getAction());

                //Message message = Message.obtain(mainThreadHandler, KEY_EVENT, keyEvent.getKeyCode(), 0, clone);
                Message message = Message.obtain(mainThreadHandler, KEY_EVENT, 0, 0, keyEvent);
                mainThreadHandler.sendMessage(message);
            }
        }

        private MotionEvent getMotionEvent(long time, int action) {
            MotionEvent event = MotionEvent.obtain(time, time, action, 1,
                    pointerProperties, pointerCoordsArray, 0, 0, 1f, 1f, 0, 0,
                    InputDevice.SOURCE_TOUCHSCREEN, 0);
            return event;
        }
    };

}

