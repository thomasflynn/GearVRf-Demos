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

import android.app.Activity;
import android.os.Bundle;
import org.gearvrf.GVRActivity;
import org.gearvrf.GVRScript;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRScene;
import org.gearvrf.scene_objects.GVRTextViewSceneObject;
import android.view.Gravity;
import java.util.Iterator;
import java.util.Queue;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.regex.Pattern;
import java.util.List;
import java.net.URL;
import java.io.InputStream;
import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRBitmapTexture;
import org.gearvrf.GVRCameraRig;
import org.gearvrf.GVRContext;
import org.gearvrf.GVREyePointeeHolder;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRMeshEyePointee;
import org.gearvrf.GVRPicker;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRScript;
import org.gearvrf.GVRTexture;
import org.gearvrf.animation.GVRAnimation;
import org.gearvrf.animation.GVRAnimationEngine;
import org.gearvrf.animation.GVROpacityAnimation;
import org.gearvrf.scene_objects.GVRCameraSceneObject;
import org.gearvrf.scene_objects.GVRCubeSceneObject;
import org.gearvrf.scene_objects.GVRCylinderSceneObject;
import org.gearvrf.scene_objects.GVRSphereSceneObject;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.SystemClock;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.Toast;

import com.oculus.VRTouchPadGestureDetector.SwipeDirection;


public class GearVRScriptingManager extends GVRScript
{
    private final String TAG = "GVRScriptingManager";
    private GVRContext mContext;
    private GearVRScripting mActivity;

    private GVRScene mScene;
    private GVRCameraRig mRig;
    private GVRSceneObject mHeadContainer;

    private List<GVRPicker.GVRPickedObject> pickedObjects;
    private float[] hitLocation = new float[3];

    private GVRSceneObject mContainer;

    private Browser browser;
    private boolean browserFocused = true;

    private EditText editText;
    private boolean activated = false;

    private Map<String, GVRSceneObject> objects = new HashMap<String, GVRSceneObject>();
    private Map<String, String> dict = new HashMap<String, String>();

    private Queue<String> taskQueue = new ArrayBlockingQueue<String>(16);
    private boolean permissionBackgroundPageSettable = true;


    @Override
    public void onInit(GVRContext gvrContext) {
        gvrContext.startDebugServer();
        mScene = gvrContext.getNextMainScene();
        mActivity = (GearVRScripting) gvrContext.getActivity();
        mContext = gvrContext;

        // get the ip address
        GearVRScripting activity = (GearVRScripting) gvrContext.getActivity();
        String ipAddress = activity.getIpAddress();
        String telnetString = "telnet " + ipAddress + " 1645";

        // create text object to tell the user where to connect
        GVRTextViewSceneObject textViewSceneObject = new GVRTextViewSceneObject(gvrContext, gvrContext.getActivity(), telnetString);
        textViewSceneObject.setGravity(Gravity.CENTER);
        textViewSceneObject.setTextSize(textViewSceneObject.getTextSize() * 1.2f);
        textViewSceneObject.getTransform().setPosition(0.0f, 0.0f, -3.0f);

        // make sure to set a name so we can reference it when we log in
        textViewSceneObject.setName("text");
        textViewSceneObject.getRenderData().setAlphaToCoverage(true);

        // add it to the scene
        mScene.addSceneObject(textViewSceneObject);

        mContainer = new GVRSceneObject(gvrContext);
        mContainer.setName("browserContainer");
        mScene.addSceneObject(mContainer);

        createBrowser();
    }

    public void refreshWebView() {
        //this.showMessage("refresh");

        browser.getWebView().reload();
    }

    public void createNewObject(String name, String type) {
        taskQueue.add(name+":"+type);
    }

    public void create(String name, String type) {

        class CreateTask implements Runnable {
            final String name;
            final String type;
            public CreateTask(String _name, String _type) {
                this.name = _name;
                this.type = _type;
            }

            @Override
            public void run() {
                GVRSceneObject obj = createObject(this.name, this.type);
            }
        }

        CreateTask ct = new CreateTask(name, type);

        mContext.runOnGlThread(ct);
    }


    public void processTaskQueue() {
        if (taskQueue.size() != 0) {
            String task = taskQueue.poll();

            String[] pieces = task.split(":");
            if (pieces.length == 2) {
                String name = pieces[0];
                String type = pieces[1];

                if (type.equals("cube")) // temp
                    create(name, "cube");
                else if (type.equals("plane"))
                    create(name, "plane");
                else if (type.equals("sphere"))
                    create(name, "sphere");
                else if (type.equals("cylinder"))
                    create(name, "cylinder");
            }
        }
    }



    /* Object */
    // make a scene object of type
    public GVRSceneObject createObject(String name, String type) {
        // TODO: implement texturing
        GVRTexture texture = mContext.loadTexture(
                new GVRAndroidResource(mContext, R.raw.earthmap1k ));

        GVRSceneObject obj;
        if (type == "cube")
            obj = new GVRCubeSceneObject(mContext);
        else if (type == "sphere")
            obj = new GVRSphereSceneObject(mContext);
        else if (type == "cylinder")
            obj = new GVRCylinderSceneObject(mContext);
        else // default : plane
            obj = new GVRSceneObject(mContext);

        //obj.setName(name);

        GVRMaterial material = new GVRMaterial(mContext);
        material.setMainTexture(texture);
        obj.getRenderData().setMaterial(material);

        objects.put(name, obj);

        return obj;
    }

    public void rotateObject(String name, float angle, float x, float y, float z) {
        GVRSceneObject obj = objects.get(name);
        if (obj == null)
            return;

        obj.getTransform().setRotationByAxis(angle, x,y,z);
    }

    public void setObjectRotation(String name, float w, float x, float y, float z) {
        GVRSceneObject obj = objects.get(name);
        if (obj == null)
            return;

        obj.getTransform().setRotation(w, x, y, z);
    }

    public void setObjectPosition(String name, float x, float y, float z) {
        GVRSceneObject obj = objects.get(name);
        if (obj == null)
            return;

        obj.getTransform().setPosition(x, y, z);
    }

    public void translateObject(String name, float x, float y, float z) {
        GVRSceneObject obj = objects.get(name);
        if (obj == null)
            return;

        obj.getTransform().translate(x, y, z);
    }

    public void setObjectScale(String name, float x, float y, float z) {
        GVRSceneObject obj = objects.get(name);
        if (obj == null)
            return;

        obj.getTransform().setScale(x, y, z);
    }

    public void setObjectVisible(String name, boolean visible) {
        GVRSceneObject obj = objects.get(name);
        if (obj == null)
            return;

        float opacity = visible ? 1f : 0f;

        obj.getRenderData().getMaterial().setOpacity(opacity);
    }

    // reset environment
    public void reset() {
        // remove all objects
        Iterator<Map.Entry<String, GVRSceneObject>> it = objects.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, GVRSceneObject> entry = it.next();

            GVRSceneObject object = entry.getValue();

            mContainer.removeChildObject(object);
        }
        objects.clear();

        // clear values
        dict.clear();

        //background.setColor(Color.DKGRAY);
    }

    /* Scene */
    public void addObjectToScene(String scene, String object) {
        final GVRSceneObject obj = objects.get(object);
        if (obj == null) {
            return;
        }

        mContainer.addChildObject(obj);
    }

    public void removeObjectFromScene(String scene, String object) {
        GVRSceneObject obj = objects.get(object);
        if (obj == null)
            return;

        mContainer.removeChildObject(obj);
    }

    /* Background */
    public String getBackground() {
        // XXX
        //return background.getValue();
        return "0x00000000";
    }

    private static final String HEX_PATTERN = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
    private Pattern pattern;

    /* Set background
     * color, gradient or image
     */
    public void setBackground(String bg) {
        android.util.Log.d(TAG, "setBackground("+bg+")");

        pattern = Pattern.compile(HEX_PATTERN);
        boolean isColor = pattern.matcher(bg).matches();

        boolean isGradient = bg.contains(","); // hacky, for now
        boolean isImage = bg.contains("http"); // absolute

        if (isColor)
            setBackgroundColor(bg);
        else if (isGradient)
            setBackgroundGradient(bg);
        else if (isImage)
            setBackgroundImage(bg);
    }

    public void setBackgroundColor(String colorStr) {
        if (!permissionBackgroundPageSettable) {
            return;
        }

        try {
            final int color = Color.parseColor(colorStr);
            // XXX background.setValue(colorStr);
            mContext.runOnGlThread(new Runnable() {
                @Override
                public void run() {
                    // XXX background.setColor(color);
                }
            });
        } catch (IllegalArgumentException e) {
            //Log.w(TAG, "Exception : " + e);
        }
    }

    public void setBackgroundGradient(String gradient) {
        if (!permissionBackgroundPageSettable) {
            return;
        }

        String[] _colors = gradient.split(",");

        final int[] colors = new int[_colors.length];

        for (int i = 0; i < _colors.length; i++) {
            colors[i] = Color.parseColor(_colors[i]);
        }

        // XXX background.setValue(gradient);

        mContext.runOnGlThread(new Runnable() {
            @Override
            public void run() {
                // XXX background.setGradient(colors);
            }
        });
    }

    public void setBackgroundImage(String imageUrl) {
        //this.showMessage("set bg image" + imageUrl);

        if (!permissionBackgroundPageSettable)
            return;

        try {
            InputStream is = new URL(imageUrl).openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            final GVRTexture texture = new GVRBitmapTexture(mContext, bitmap);

            mContext.runOnGlThread(new Runnable() {
                @Override
                public void run() {
                    // XXX background.setImage(texture);
                }
            });

        } catch (Exception e) {

        }
    }

    public String getValue(String key) {
        return dict.get(key);
    }

    public void setValue(String key, String value) {
        dict.put(key, value);
    }

    /* JS context method invocation
     */
    public String call(String methodName, String params) {
        return "return_val";
    }


    @Override
    public void onStep() {
        processTaskQueue();

        pickedObjects = GVRPicker.findObjects(mScene, 0f,0f,0f, 0f,0f,-1f);

        for (GVRPicker.GVRPickedObject pickedObject : pickedObjects) {
            GVRSceneObject obj = pickedObject.getHitObject();

            hitLocation = pickedObject.getHitLocation();

            if (obj.getName().equals("webview")) {
                browserFocused = true;
                //buttonFocused = false;

                /*String coords =
                        String.format("%.3g%n", hitLocation[0]) + "," +
                        String.format("%.3g%n", hitLocation[1]);

                editText.setText(coords);*/
            } else { // NOTE: buttons only for now

                /*
                browserFocused = false;
                buttonFocused = true;

                for (int i = 0; i < uiButtons.size(); i++) {
                    Button button = uiButtons.get(i);
                    if ( button.name.equals( obj.getName() ) ) {
                        focusedButton = button;
                        button.setFocus(true);
                    }
                }
                */
            }

            break;
        }

        /*
        // reset
        if (pickedObjects.size() == 0) {
            browserFocused = false;
            buttonFocused = false;

            if (focusedButton != null)
                focusedButton.setFocus(false);
        }
        */
    }

    private final float mBrowserWidth  = 4f;
    private final float mBrowserHeight = 3f;
    
    public void createBrowser() {
        float distance = 3.5f;

        float width  = mBrowserWidth;
        float height = mBrowserHeight;

        WebView webView = mActivity.getWebView();

        browser = new Browser(mContext, mActivity, width, height, webView);

        editText = browser.getEditText();

        GVRSceneObject screenObject = browser.getScreenObject();
        screenObject.setPickingEnabled(true);

        browser.getSceneObject().getTransform().setPosition(0f, 0f, -distance);

        mContainer.addChildObject( browser.getSceneObject() );

        // add buttons to container
        GVRSceneObject uiContainerObject = new GVRSceneObject(mContext);
        uiContainerObject.getTransform().setPosition(-1.3f*(width/2f), 0f, -distance);
        uiContainerObject.getTransform().rotateByAxis(20f, 0f, 1f, 0f);
        mContainer.addChildObject(uiContainerObject);

    }

    public void onPause() {

    }

    public void onResume() {

    }

    public void onKeyDown(int keyCode, KeyEvent event) {
        if (browserFocused) {
            WebView webView = browser.getWebView();
            webView.dispatchKeyEvent(event);
            return;
        }

        if (!activated) {
            editText.setActivated(true);
            editText.requestFocus();
            activated = true;
        }

        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            String navText = editText.getText().toString();

            if ( Patterns.WEB_URL.matcher(navText).matches() ) {
                if (!navText.toLowerCase().startsWith("http://"))
                    navText = "http://" + navText;
                browser.getWebView().loadUrl(navText);
            }

            return;
        }

        editText.dispatchKeyEvent(event);
    }

    public void onKeyUp(int keyCode, KeyEvent event) {
        if (browserFocused) {
            WebView webView = browser.getWebView();
            webView.dispatchKeyEvent(event);
            return;
        }

        if (!activated) {
            editText.setActivated(true);
            editText.requestFocus();
            activated = true;
        }

        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            return;
        }

        editText.dispatchKeyEvent(event);
    }

    public void click() {
        WebView webView = browser.getWebView();

        final long uMillis = SystemClock.uptimeMillis();

        int width = mActivity.WEBVIEW_WIDTH,
           height = mActivity.WEBVIEW_HEIGHT;

        float hitX = this.hitLocation[0];
        float hitY = this.hitLocation[1] * -1f;
        
        // normalize
        hitX += mBrowserWidth/2.0f;
        hitY += mBrowserHeight/2.0f;
        hitX /= mBrowserWidth;
        hitY /= mBrowserHeight;

        android.util.Log.v(TAG, "coords:" + hitX + "  " + hitY);
        //this.showMessage("coords: " + hitX + "  " + hitY);
        
        float x = Math.round(hitX * width);
        float y = Math.round(hitY * height);

        webView.dispatchTouchEvent(MotionEvent.obtain(uMillis, uMillis,
                MotionEvent.ACTION_DOWN, x, y, 0));
        webView.dispatchTouchEvent(MotionEvent.obtain(uMillis, uMillis,
                MotionEvent.ACTION_UP, x, y, 0));
    }

    public void scroll(int direction, float velocity) {
        WebView webView = browser.getWebView();

        int dy = direction * 10;

        webView.scrollBy(0, dy);
    }

    public void onSingleTap(MotionEvent event) {
        if (browserFocused) {
        android.util.Log.v(TAG, "calling click()");
            click();
        }     
    }

    public void onButtonDown() {

    }

    public void onLongButtonPress(int keyCode) {

    }

    public void onTouchEvent(MotionEvent event) {

    }

    public boolean onSwipe(MotionEvent e, SwipeDirection swipeDirection,
            float velocityX, float velocityY) {

        switch (swipeDirection) {
            case Up:
                scroll(-1, velocityY);
                break;
            case Down:
                scroll(1, velocityY);
                break;
            case Forward:
                break;
            case Backward:
                break;
        }
        return true;
    }

}
