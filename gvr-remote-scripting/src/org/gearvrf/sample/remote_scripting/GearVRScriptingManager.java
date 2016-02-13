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
import java.util.ArrayList;
import java.util.List;
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

    @Override
    public void onInit(GVRContext gvrContext) {
        gvrContext.startDebugServer();
        GVRScene scene = gvrContext.getNextMainScene();

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

        // add it to the scene
        scene.addSceneObject(textViewSceneObject);
    }

    @Override
    public void onStep() {
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
