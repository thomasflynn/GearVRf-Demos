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

package org.gearvrf.zoom360Photo;

import java.util.concurrent.Future;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRActivity;
import org.gearvrf.GVRPostEffect;
import org.gearvrf.GVRCameraRig;
import org.gearvrf.GVRTexture;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRAndroidResource;
import org.gearvrf.scene_objects.GVRViewSceneObject;
import org.gearvrf.scene_objects.GVRModelSceneObject;
import org.gearvrf.scene_objects.view.GVRFrameLayout;
import org.gearvrf.GVRBaseSensor;
import org.gearvrf.SensorEvent;
import org.gearvrf.ISensorEvents;

import org.gearvrf.GVRContext;
import org.gearvrf.debug.cli.LineProcessor;
import org.gearvrf.script.GVRScriptManager;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.MotionEvent.PointerCoords;
import android.view.MotionEvent.PointerProperties;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.StringWriter;
import java.lang.Runnable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class FileBrowserUtils {
    private static final String TAG = "Zoom360 FileBrowser";
    private GVRContext gvrContext;
    private boolean inflated = false;
    private GVRViewSceneObject layoutSceneObject;
    private GVRActivity activity;
    private GVRFrameLayout frameLayout;
    private int frameWidth;
    private int frameHeight;
    private Handler mainThreadHandler;
    private final static PointerProperties[] pointerProperties;
    private final static PointerCoords[] pointerCoordsArray;
    private final static PointerCoords pointerCoords;
    private static final int KEY_EVENT = 1;
    private static final int MOTION_EVENT = 2;

    private static final float QUAD_X = 2.0f;
    private static final float QUAD_Y = 1.0f;
    private static final float HALF_QUAD_X = QUAD_X / 2.0f;
    private static final float HALF_QUAD_Y = QUAD_Y / 2.0f;
    private static final float DEPTH = -1.5f;

    private String path;
    private ListView listView;
    private TextView dirView;
    private ProgressBar spinner;
    private FileBrowserListener listener;
    private boolean ignoreOnClick;

    interface FileBrowserListener {
        void onFileSelected(String filePath);
    }

    static {
        PointerProperties properties = new PointerProperties();
        properties.id = 0;
        properties.toolType = MotionEvent.TOOL_TYPE_MOUSE;
        pointerProperties = new PointerProperties[] { properties };
        pointerCoords = new PointerCoords();
        pointerCoordsArray = new PointerCoords[] { pointerCoords };
    }

    public FileBrowserUtils(GVRContext context, FileBrowserListener fileBrowserListener) {
        gvrContext = context;
        activity = (GVRActivity) context.getActivity();
        listener = fileBrowserListener;
    }

    public void inflate() {
        frameLayout = new GVRFrameLayout(activity);
        frameLayout.setDrawingCacheEnabled(false);
        View.inflate(activity, R.layout.filebrowser, frameLayout);

        listView = (ListView) frameLayout.findViewById(R.id.list);
        dirView = (TextView) frameLayout.findViewById(R.id.dirname);
        spinner = (ProgressBar) frameLayout.findViewById(R.id.progressBar);
        spinner.setVisibility(View.GONE);
        init();

        mainThreadHandler = new Handler(activity.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == MOTION_EVENT) {
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
            ignoreOnClick = true;
            return;
        }

        layoutSceneObject = new GVRViewSceneObject(gvrContext, frameLayout, gvrContext.createQuad(QUAD_X, QUAD_Y));

        layoutSceneObject.getTransform().setPosition(0.0f, 0.0f, -2.0f);
        layoutSceneObject.setName("browser");

        frameWidth = frameLayout.getWidth();
        frameHeight = frameLayout.getHeight();

        GVRBaseSensor sensor = new GVRBaseSensor(gvrContext);
        layoutSceneObject.getEventReceiver().addListener(sensorEvents);
        layoutSceneObject.setSensor(sensor);

        gvrContext.getMainScene().addSceneObject(layoutSceneObject);
    }

    public void setPosition(float x, float y, float z) {
        layoutSceneObject.getTransform().setPosition(x, y, z);
    }

    public void setRotationByAxis(float angle, float x, float y, float z) {
        layoutSceneObject.getTransform().setRotationByAxis(angle, x, y, z);
    }

    public void hide() {
        gvrContext.getMainScene().removeSceneObject(layoutSceneObject);
    }

    private void init() {
        path = "/sdcard/photospheres";
        chdir(path);
    }

    private void chdir(String filepath) {
        path = filepath;
        dirView.setText(path);
        spinner.setVisibility(View.GONE);

        List values = new ArrayList();
        File dir = new File(path);
        if(!dir.canRead()) {
            dirView.setText(dirView.getText() + " (inaccessible)");
        }

        // only allow model extensions we can read
        final File[] list = dir.listFiles(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    String filename = dir.getAbsolutePath() + File.separator + name;
                    if(new File(filename).isDirectory()) {
                        return true;
                    }
                    
                    if(
                        (name.toLowerCase().endsWith(".png")) ||
                        (name.toLowerCase().endsWith(".jpg")) ||
                        (name.toLowerCase().endsWith(".jpeg")) ||
                        (name.toLowerCase().endsWith(".bmp")) ||
                        (name.toLowerCase().endsWith(".webp"))
                        ) {
                        return true;
                    }

                    return false;
                }
        });

        // add .. so the user can go up a level
        if(list != null) {
            values.add("..");
            for(File file : list) {
                values.add(file.getName());
            }
        }

        // sort alphabetically
        Collections.sort(values);

        ArrayAdapter adapter = new ArrayAdapter(activity, android.R.layout.simple_list_item_2, android.R.id.text1, values);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if(ignoreOnClick) {
                        ignoreOnClick = false;
                        return;
                    }

                    String filename = (String) listView.getItemAtPosition(position);

                    if(filename.endsWith("..")) {
                        // strip out the /..
                        int index = path.lastIndexOf(File.separator);
                        filename = path.substring(0, index);
                    } else if(path.endsWith(File.separator)) {
                        filename = path + filename;
                    } else {
                        filename = path + File.separator + filename;
                    }

                    if(new File(filename).isDirectory()) {
                        chdir(filename);
                    } else if(!filename.isEmpty()) {
                        // strip out /sdcard
                        //filename = filename.substring(8);
                        spinner.setVisibility(View.VISIBLE);
                        // try to load the image
                        listener.onFileSelected(filename);
                        spinner.setVisibility(View.GONE);
                    }
                }
            });
    }

    private ISensorEvents sensorEvents = new ISensorEvents() {
        private static final float SCALE = 5.0f;
        private float savedMotionEventX, savedMotionEventY, savedHitPointX, savedHitPointY;
        @Override
        public void onSensorEvent(final SensorEvent event) {

            List<MotionEvent> motionEvents = event.getCursorController().getMotionEvents();

            for(MotionEvent motionEvent : motionEvents) {
                if(motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    pointerCoords.x = savedHitPointX + ((motionEvent.getX() - savedMotionEventX) * SCALE);
                    pointerCoords.y = savedHitPointY + ((motionEvent.getY() - savedMotionEventY) * SCALE);
                } else {
                    float[] hitPoint = event.getHitPoint();

                    pointerCoords.x = ((hitPoint[0] + HALF_QUAD_X) / QUAD_X) * frameWidth;
                    pointerCoords.y = (-(hitPoint[1] - HALF_QUAD_Y) / QUAD_Y) * frameHeight;

                    if(motionEvent.getAction() == KeyEvent.ACTION_DOWN) {
                        // save the coordinates on down
                        savedMotionEventX = motionEvent.getX();
                        savedMotionEventY = motionEvent.getY();

                        savedHitPointX = pointerCoords.x;
                        savedHitPointY = pointerCoords.y;
                    }
                }

                final MotionEvent clone = MotionEvent.obtain(
                        motionEvent.getDownTime(), motionEvent.getEventTime(), motionEvent.getAction(), 1, pointerProperties, pointerCoordsArray, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0);

                Message message = Message.obtain(mainThreadHandler, MOTION_EVENT, 0, 0, clone);
                mainThreadHandler.sendMessage(message);
            }

            List<KeyEvent> keyEvents = event.getCursorController().getKeyEvents();
            for(KeyEvent keyEvent : keyEvents) {
                Message message = Message.obtain(mainThreadHandler, KEY_EVENT, keyEvent.getKeyCode(), 0, null);
                mainThreadHandler.sendMessage(message);
            }
        }

    };


}

