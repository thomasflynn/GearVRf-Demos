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

import java.io.IOException;
import android.app.Activity;
import android.os.Bundle;
import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRActivity;
import org.gearvrf.GVRScript;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRBaseSensor;
import org.gearvrf.GVRTexture;
import org.gearvrf.scene_objects.GVRTextViewSceneObject;
import org.gearvrf.scene_objects.view.GVRFrameLayout;
import org.gearvrf.scene_objects.GVRViewSceneObject;
import org.gearvrf.scene_objects.GVRSphereSceneObject;
import android.view.Gravity;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.ListView;
import android.widget.TextView;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import java.util.concurrent.Future;


public class GearVRScriptingManager extends GVRScript
{
    private GVRContext mGVRContext;
    private GearVRScripting activity;
    private final GVRFrameLayout frameLayout;
    private Handler mainThreadHandler;
    private GVRViewSceneObject layoutSceneObject;
    private GVRSphereSceneObject sphereSceneObject;

    private int frameWidth;
    private int frameHeight;

    private static final float QUAD_X = 1.0f;
    private static final float QUAD_Y = 1.0f;
    private static final float HALF_QUAD_X = QUAD_X / 2.0f;
    private static final float HALF_QUAD_Y = QUAD_Y / 2.0f;
    private static final float DEPTH = -1.5f;

    public GearVRScriptingManager(GearVRScripting scriptingActivity) {
        activity = scriptingActivity;
        frameLayout = activity.getFrameLayout();

        mainThreadHandler = new Handler(activity.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                // dispatch motion event
                MotionEvent motionEvent = (MotionEvent) msg.obj;
                frameLayout.dispatchTouchEvent(motionEvent);
                frameLayout.invalidate();
                motionEvent.recycle();
            }
        };
    }

    @Override
    public void onInit(GVRContext gvrContext) {
        gvrContext.startDebugServer();
        mGVRContext = gvrContext;
        GVRScene scene = gvrContext.getNextMainScene();

        // get the ip address
        String ipAddress = activity.getIpAddress();
        String telnetString = "telnet " + ipAddress + " 1645";

        // create text object to tell the user where to connect
        GVRTextViewSceneObject textViewSceneObject = new GVRTextViewSceneObject(gvrContext, gvrContext.getActivity(), telnetString);
        textViewSceneObject.setGravity(Gravity.CENTER);
        textViewSceneObject.setTextSize(textViewSceneObject.getTextSize() * 1.2f);
        textViewSceneObject.getTransform().setPosition(0.0f, 0.0f, -3.0f);

        // make sure to set a name so we can reference it when we log in
        textViewSceneObject.setName("text");

        // add it to the scene
        scene.addSceneObject(textViewSceneObject);

        // setup layout object
        layoutSceneObject = new GVRViewSceneObject(gvrContext, frameLayout, gvrContext.createQuad(QUAD_X, QUAD_Y));
        layoutSceneObject.getTransform().setPosition(0.0f, 0.0f, DEPTH);
        //scene.addSceneObject(layoutSceneObject);
   
        // setup sensor
        GearVRSensor gvrsensor = new GearVRSensor(gvrContext, (Handler)mainThreadHandler);
        GVRBaseSensor sensor = gvrsensor.getSensor();
        layoutSceneObject.setSensor(sensor);

        // setup background sphere
        try {
            Future<GVRTexture> background = mGVRContext.loadFutureTexture(new GVRAndroidResource(mGVRContext, "background1.jpg"));
            sphereSceneObject = new GVRSphereSceneObject(gvrContext, false, background);
            sphereSceneObject.getTransform().setScale(6.0f, 6.0f, 6.0f);
            //scene.addSceneObject(sphereSceneObject);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    public void setNewBackground(final String filename) {
        mGVRContext.runOnGlThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Future<GVRTexture> background = mGVRContext.loadFutureTexture(new GVRAndroidResource(mGVRContext, filename));
                        sphereSceneObject.getRenderData().getMaterial().setMainTexture(background);
                    } catch(IOException e) {
                        e.printStackTrace();
                    }
                }
            });
    }

    @Override
    public void onStep() {
    }
}
