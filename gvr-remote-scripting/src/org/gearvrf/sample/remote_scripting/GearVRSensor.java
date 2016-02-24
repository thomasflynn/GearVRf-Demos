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

import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRBaseSensor;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRCursorController;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRScript;
import org.gearvrf.ISensorEvents;
import org.gearvrf.SensorEvent;
import org.gearvrf.GVRTexture;
import org.gearvrf.io.CursorControllerListener;
import org.gearvrf.io.GVRCursorType;
import org.gearvrf.io.GVRInputManager;
import org.gearvrf.scene_objects.GVRSphereSceneObject;
import org.gearvrf.scene_objects.GVRViewSceneObject;
import org.gearvrf.scene_objects.view.GVRFrameLayout;
import org.gearvrf.utility.Log;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.MotionEvent;
import android.view.InputDevice;
import java.util.concurrent.Future;
import android.os.Message;
import android.os.Handler;

public class GearVRSensor {

    private GVRBaseSensor sensor;
    private final static PointerProperties[] pointerProperties;
    private final static PointerCoords[] pointerCoordsArray;
    private final static PointerCoords pointerCoords;
    private static final float DEPTH = -1.5f;
    private GVRContext mGVRContext;
    private GVRScene mScene;
    private Handler mainThreadHandler;

    static {
        PointerProperties properties = new PointerProperties();
        properties.id = 0;
        properties.toolType = MotionEvent.TOOL_TYPE_MOUSE;
        pointerProperties = new PointerProperties[] { properties };
        pointerCoords = new PointerCoords();
        pointerCoordsArray = new PointerCoords[] { pointerCoords };
    }

    public GearVRSensor(GVRContext gvrContext, Handler handler) {
        // set up the input manager for the main scene
        mGVRContext = gvrContext;
        mScene = gvrContext.getMainScene();
        mainThreadHandler = handler;
        GVRInputManager inputManager = gvrContext.getInputManager();
        inputManager.addCursorControllerListener(listener);
        for (GVRCursorController cursor : inputManager.getCursorControllers()) {
            listener.onCursorControllerAdded(cursor);
        }
        sensor = new GVRBaseSensor(gvrContext);
        sensor.registerSensorEventListener(eventListener);
    }

    public GVRBaseSensor getSensor() {
        return sensor;
    }

    private ISensorEvents eventListener = new ISensorEvents() {
        private static final float SCALE = 10.0f;
        private float savedMotionEventX, savedMotionEventY, savedHitPointX,
                savedHitPointY;

        @Override
        public void onSensorEvent(SensorEvent event) {
            final MotionEvent motionEvent = event.getCursorController().getMotionEvent();
            if (motionEvent != null && motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                pointerCoords.x = savedHitPointX + ((motionEvent.getX() - savedMotionEventX) * SCALE);
                pointerCoords.y = savedHitPointY + ((motionEvent.getY() - savedMotionEventY) * SCALE);

                final MotionEvent clone = MotionEvent.obtain(
                        motionEvent.getDownTime(), motionEvent.getEventTime(),
                        motionEvent.getAction(), 1, pointerProperties,
                        pointerCoordsArray, 0, 0, 1f, 1f, 0, 0,
                        InputDevice.SOURCE_TOUCHSCREEN, 0);

                Message message = Message.obtain(mainThreadHandler, 0, 0, 0, clone);
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

    private CursorControllerListener listener = new CursorControllerListener() {

        @Override
        public void onCursorControllerRemoved(GVRCursorController controller) {
            if (controller.getCursorType() == GVRCursorType.GAZE) {
                controller.resetSceneObject();
                controller.setEnable(false);
            }
        }

        @Override
        public void onCursorControllerAdded(GVRCursorController controller) {
            android.util.Log.d("cursor", "onCursorControllerAdded()");

            // Only allow only gaze
            if (controller.getCursorType() == GVRCursorType.GAZE) {
            android.util.Log.d("cursor", "type gaze");
                GVRTexture blueTexture = mGVRContext.loadTexture(new GVRAndroidResource(mGVRContext, R.raw.bluedot));

                GVRSceneObject cursor = new GVRSceneObject(mGVRContext, 0.1f, 0.1f, blueTexture);

                /*
                GVRRenderData cursorRenderData = cursor.getRenderData();
                GVRMaterial material = cursorRenderData.getMaterial();
                material.setShaderType(shaderManager.getShaderId());
                material.setVec4(CustomShaderManager.COLOR_KEY, 1.0f, 0.0f, 0.0f, 0.5f);
                */

                mScene.addSceneObject(cursor);
                cursor.getRenderData().setDepthTest(false);
                cursor.getRenderData().setRenderingOrder(100000);
                controller.setSceneObject(cursor);
                controller.setPosition(0.0f, 0.0f, DEPTH);
                controller.setNearDepth(DEPTH);
                controller.setFarDepth(DEPTH);
            android.util.Log.d("cursor", "alright, it's added, where is it?");
            } else {
                // disable all other types
                controller.setEnable(false);
            }
        }
    };

}

