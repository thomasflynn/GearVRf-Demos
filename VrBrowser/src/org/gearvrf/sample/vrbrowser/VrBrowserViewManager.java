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

package org.gearvrf.sample.vrbrowser;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.gearvrf.FutureWrapper;
import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRBitmapTexture;
import org.gearvrf.GVRContext;
import org.gearvrf.GVREyePointeeHolder;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRMeshEyePointee;
import org.gearvrf.GVRPicker;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRScript;
import org.gearvrf.GVRTexture;
import org.gearvrf.animation.GVRAnimation;
import org.gearvrf.animation.GVRAnimationEngine;
import org.gearvrf.animation.GVROpacityAnimation;
import org.gearvrf.keyboard.util.VRSamplesTouchPadGesturesDetector.SwipeDirection;
import org.gearvrf.scene_objects.GVRCameraSceneObject;
import org.gearvrf.scene_objects.GVRCubeSceneObject;
import org.gearvrf.scene_objects.GVRCylinderSceneObject;
import org.gearvrf.scene_objects.GVRSphereSceneObject;
import org.gearvrf.scene_objects.GVRTextViewSceneObject;
import org.gearvrf.scene_objects.GVRViewSceneObject;
import org.gearvrf.scene_objects.view.GVRTextView;
import org.gearvrf.scene_objects.view.GVRWebView;
import org.gearvrf.ui.Button;
import org.gearvrf.ui.ProgressBar;
import org.gearvrf.ui.UICursor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.hardware.Camera;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.SystemClock;
import android.util.Log;
import android.util.Patterns;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.Toast;

public class VrBrowserViewManager extends GVRScript {

    private final static String TAG = "VrBrowserViewManager";

    private GVRAnimationEngine mAnimationEngine;

    private VrBrowser mActivity;
    private GVRContext mGVRContext;
    private GVRContext mContext;
    private GVRScene mScene;

    private GVRSceneObject mHeadContainer;

    private GVRTextViewSceneObject mTitleBar = null;

    private GVRTextView mTextView;
    private GVRViewSceneObject textViewObject;

    private GVRWebView mWebView;
    private GVRSceneObject mMainSceneContainer;
    private GVRSceneObject mButtonContainer;

    private boolean mBrowserFocused = true;
    private boolean mKeyboardFocused = false;
    
    private List<GVRPicker.GVRPickedObject> pickedObjects;
    private float[] hitLocation = new float[3];

    private List<Button> mUiButtons = new ArrayList<Button>();
    private Button mFocusedButton = null;
    private boolean mButtonFocused = false;
    private GVRViewSceneObject mWebViewObject;
    private final float mBrowserWidth = 4f;
    private final float mBrowserHeight = 2.25f;
    private final float mBrowserDistance = 3f;

    private float mTitleBarHeight = mBrowserHeight / 2f + 0.25f;

    public ProgressBar mProgressBar = null;

    private Background mBackground;
    private boolean mPermissionBackgroundSettableByPage = true;

    private GVRCameraSceneObject mCameraObject;
    private Camera mCamera;
    private boolean mCameraDisplayed = false;
    private GVRAnimation mCameraAnim;

    private UICursor mUICursor;

    private GVRSceneObject mContainer; // for JS interface

    VrBrowserViewManager(VrBrowser activity) {
        mActivity = activity;
    }

    @Override
    public void onInit(GVRContext gvrContext) {
        mGVRContext = gvrContext;
        mContext = gvrContext;

        gvrContext.startDebugServer();

        String ipAddress = ((VrBrowser) mActivity).getIpAddress();
        String telnetString = "telnet " + ipAddress + " 1645";

        mScene = gvrContext.getNextMainScene();

        mAnimationEngine = gvrContext.getAnimationEngine();

        mMainSceneContainer = new GVRSceneObject(gvrContext);
        mScene.addSceneObject(mMainSceneContainer);

        mHeadContainer = new GVRSceneObject(gvrContext);
        mScene.getMainCameraRig().addChildObject(mHeadContainer);

        mContainer = new GVRSceneObject(gvrContext);
        mScene.addSceneObject(mContainer);

        mBackground = new Background(gvrContext);
        mBackground.setColor(Color.DKGRAY);
        mScene.addSceneObject(mBackground.getSceneObject());

        mWebViewObject = createWebViewObject(gvrContext);
        mMainSceneContainer.addChildObject(mWebViewObject);

        mButtonContainer = new GVRSceneObject(gvrContext);
        mMainSceneContainer.addChildObject(mButtonContainer);
        addButtons();

        CreateTitleBar();
        CreateProgressBar();

        addCursor(mHeadContainer, R.raw.gaze_cursor_dot2, mBrowserDistance);

        initCamera(mHeadContainer);

        // create text object to tell the user where to connect
        GVRTextViewSceneObject textViewSceneObject = new GVRTextViewSceneObject(gvrContext, gvrContext.getActivity(), telnetString);
        textViewSceneObject.setGravity(Gravity.CENTER);
        textViewSceneObject.setTextSize(textViewSceneObject.getTextSize() * 1f);
        textViewSceneObject.getTransform().setPosition(0.0f, -2.0f, -3.0f);

        // make sure to set a name so we can reference it when we log in
        textViewSceneObject.setName("text");
        
        // add it to the scene
        mScene.addSceneObject(textViewSceneObject);
        
        // test plane
        Future<GVRTexture> futureTexture = gvrContext
                .loadFutureTexture(new GVRAndroidResource(gvrContext, R.raw.back_button));

        GVRPlaneSceneObject planeObject = new GVRPlaneSceneObject(gvrContext, futureTexture, 4);
        GVRMesh planeMesh = planeObject.getRenderData().getMesh();

        planeObject.getTransform().setPosition(0f, 0f, -2f);
        // mMainSceneContainer.addChildObject(planeObject);
    }

    public void CreateTitleBar() {
        mTitleBar = new GVRTextViewSceneObject(mGVRContext, mGVRContext.getActivity(), 4.0f, 2.0f, 4000, 1000,
                mActivity.homeUrl);
        mTitleBar.setName("titleBar");
        mTitleBar.getTransform().setPosition(-0.0f, mTitleBarHeight, 0.0f);
        mTitleBar.getTransform().setScale(0.5f, 0.5f, 1);
        mTitleBar.setTextSize(mTitleBar.getTextSize() * 1.0f);
        mTitleBar.setGravity(Gravity.CENTER);
        // titleBar.setMaxLines(1);
        mTitleBar.getRenderData().setRenderingOrder(GVRRenderData.GVRRenderingOrder.TRANSPARENT);
        mTitleBar.setRefreshFrequency(GVRTextViewSceneObject.IntervalFrequency.LOW);
        mTitleBar.setTextColor(0xFFF0F0F0);

        // textViewObject = createTextViewObject(mGVRContext);
        // mTextView.setText("Hello World");
        // mTextView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

        mWebViewObject.addChildObject(mTitleBar);

        DisplayUrlBar(mActivity.homeUrl);
    }

    private void CreateProgressBar() {
        float progressBarHeight = 0.05f;
        mProgressBar = new ProgressBar(mGVRContext, mBrowserWidth, mBrowserHeight, progressBarHeight);
        mProgressBar.getSceneObject().getTransform().setPositionY(mBrowserHeight / 2 + progressBarHeight / 2);
        mWebViewObject.addChildObject(mProgressBar.getSceneObject());
        UpdateProgress(0);
    }

    public void UpdateProgress(float progress) {
        if (mProgressBar == null)
            return;
        mProgressBar.updateProgress(progress);
    }

    private void CreateEnvironment() {
        mScene.getMainCameraRig().getTransform().setPosition(-0f, 0.0f, 0f);

        GVRMesh spaceMesh = mGVRContext.loadMesh(new GVRAndroidResource(mGVRContext, R.drawable.skybox_esphere));
        GVRTexture spaceTexture = mGVRContext.loadTexture(new GVRAndroidResource(mGVRContext, R.drawable.skybox));

        GVRSceneObject mSpaceSceneObject = new GVRSceneObject(mGVRContext, spaceMesh, spaceTexture);
        mScene.addSceneObject(mSpaceSceneObject);
        mSpaceSceneObject.getRenderData().setRenderingOrder(0);

        GVRSceneObject floor = new GVRSceneObject(mGVRContext, mGVRContext.createQuad(120.0f, 120.0f),
                mGVRContext.loadTexture(new GVRAndroidResource(mGVRContext, R.drawable.floor)));
        floor.getTransform().setRotationByAxis(-90, 1, 0, 0);
        floor.getTransform().setPositionY(-10.0f);
        mScene.addSceneObject(floor);
        floor.getRenderData().setRenderingOrder(0);
    }

    /*
     * Initialize camera for passthrough video background Camera screen is
     * toggleable TODO: update to Camera2 API
     */
    @SuppressWarnings("deprecation")
    public void initCamera(GVRSceneObject container) {
        mCamera = Camera.open();

        mCamera.startPreview();

        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);

        Camera.Size size = parameters.getPreferredPreviewSizeForVideo();

        parameters.setPreviewSize(size.width, size.height);

        Log.v(TAG, "Camera size: " + size.width + "," + size.height);

        mCamera.setParameters(parameters);

        float ratio = 16f / 9f; // size.width / size.height;

        float H = 5.0f;
        float W = H * ratio;

        mCameraObject = new GVRCameraSceneObject(mContext, W, H, mCamera);
        mCameraObject.getTransform().setPosition(0f, 0f, -6.5f);
        mCameraObject.getRenderData().getMaterial().setOpacity(0.0f);
        // mCameraObject.getTransform().rotateByAxis( -90f, 0.0f, 1.0f, 0.0f );

        container.addChildObject(mCameraObject);
    }

    public void togglePassthroughCamera(boolean active) {
        if (mCameraDisplayed == active)
            return;

        float ANIMATION_DURATION = 0.6f; // secs

        mCameraAnim = new GVROpacityAnimation(mCameraObject, ANIMATION_DURATION, active ? 1.0f : 0.0f);

        mCameraAnim.start(mAnimationEngine);

        mCameraDisplayed = active;
    }

    private void addCursor(GVRSceneObject container, int resourceId, float distance) {
        mUICursor = new UICursor(mGVRContext, resourceId);
        mUICursor.setDistance(distance);
        container.addChildObject(mUICursor.getSceneObject());
    }

    public void DisplayUrlBar(String url) {
        if (mTitleBar != null) {
            mTitleBar.setText(url);
        }
    }

    public void DisplayTitleBar(String title) {
        // TODO: update a title bar
    }

    public void onBackPressed() {

    }

    private GVRViewSceneObject createWebViewObject(GVRContext gvrContext) {
        mWebView = (GVRWebView) mActivity.getWebView();

        GVRViewSceneObject webObject = new GVRViewSceneObject(gvrContext, mWebView, mBrowserWidth, mBrowserHeight);
        webObject.setName("webview");
        webObject.getTransform().setPosition(0.0f, 0.0f, -mBrowserDistance);

        attachDefaultEyePointee(webObject);
        return webObject;
    }

    private GVRViewSceneObject createTextViewObject(GVRContext gvrContext) {
        mTextView = (GVRTextView) mActivity.getTextView();

        GVRViewSceneObject textObject = new GVRViewSceneObject(gvrContext, mTextView, 4f, 2f);

        textObject.setName("title-bar");
        textObject.getRenderData().getMaterial().setOpacity(1.0f);

        attachDefaultEyePointee(textObject);

        return textObject;
    }

    private void addButtons() {
        // add buttons to container
        GVRSceneObject uiContainerObject = new GVRSceneObject(mGVRContext);
        uiContainerObject.getTransform().setPosition(-2.3f, 0f, -mBrowserDistance);
        uiContainerObject.getTransform().rotateByAxis(00f, 0f, 1f, 0f);
        mButtonContainer.addChildObject(uiContainerObject);

        float width = 1.6f;
        float height = 3.7f;

        // nav buttons
        // String[] buttons = { "home", "reload", "back", "forward",
        // "EnterURL"};
        String[] buttons = { "home", "back", "forward", "reload", "EnterURL" };
        int[] buttonTextures = { R.raw.home_button, R.raw.back_button, R.raw.forward_button, R.raw.refresh_button,
                R.drawable.url_background };

        int numButtons = buttons.length;
        float buttonSize = 0.3f;

        float xInitial = 0.5f;
        float xSpacing = (width - buttonSize) / (numButtons - 1);
        float yInitial = mTitleBarHeight;

        for (int i = 0; i < buttons.length; i++) {
            Button button = new Button(mGVRContext, buttons[i], buttonTextures[i], buttonSize);
            mUiButtons.add(button);

            GVRSceneObject buttonObject = button.getSceneObject();
            attachDefaultEyePointee(buttonObject);

            float x = xInitial + xSpacing * i;
            buttonObject.getTransform().setPositionX(x);
            buttonObject.getTransform().setPositionY(yInitial);

            if (buttons[i].equals("EnterURL")) {
                buttonObject.getTransform().setPositionX(x + 1.24f);
                buttonObject.getTransform().setPositionZ(-0.01f);
                buttonObject.getTransform().setScale(9.2f, 1.0f, 1.0f);
            }

            uiContainerObject.addChildObject(buttonObject);
        }
    }

    protected void attachDefaultEyePointee(GVRSceneObject sceneObject) {
        GVREyePointeeHolder eyePointeeHolder = new GVREyePointeeHolder(mGVRContext);
        GVRMesh mesh = sceneObject.getRenderData().getMesh();
        GVRMeshEyePointee eyePointee = new GVRMeshEyePointee(mGVRContext, mesh);
        eyePointeeHolder.addPointee(eyePointee);
        sceneObject.attachEyePointeeHolder(eyePointeeHolder);
    }

    public void navigateHome() {
        mWebView.loadUrl(mActivity.homeUrl);
    }

    public void EnterURL() {

    }

    public void refreshWebView() {
        mWebView.reload();
    }

    public void navigateForward() {
        if (mWebView.canGoForward())
            mWebView.goForward();
    }

    public void navigateBack() {
        if (mWebView.canGoBack())
            mWebView.goBack();
    }

    // browser click
    public void click() {
        final long uMillis = SystemClock.uptimeMillis();

        float[] coord = getCoord();
        float x = coord[0];
        float y = coord[1];

        // this.showMessage("click: " + x + "," + y);
        mWebView.dispatchTouchEvent(MotionEvent.obtain(uMillis, uMillis, MotionEvent.ACTION_DOWN, x, y, 0));
        mWebView.dispatchTouchEvent(MotionEvent.obtain(uMillis, uMillis, MotionEvent.ACTION_UP, x, y, 0));
    }

    public void hover() {
        float[] coord = getCoord();
        float x = coord[0];
        float y = coord[1];
        MotionEvent event = MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(),
                MotionEvent.ACTION_HOVER_MOVE, x, y, 0);
        event.setSource(InputDevice.SOURCE_MOUSE);

        mWebView.dispatchGenericMotionEvent(event);
    }

    public float[] getCoord() {
        int width = mActivity.WEBVIEW_WIDTH, height = mActivity.WEBVIEW_HEIGHT;

        float hitX = this.hitLocation[0];
        float hitY = this.hitLocation[1] * -1f;

        // normalize
        hitX += mBrowserWidth / 2.0f;
        hitY += mBrowserHeight / 2.0f;
        hitX /= mBrowserWidth;
        hitY /= mBrowserHeight;

        float[] coords = new float[2];
        coords[0] = Math.round(hitX * width);
        coords[1] = Math.round(hitY * height);

        return coords;
    }

    public void onPause() {
    }

    public void onTap() {
        if (mBrowserFocused) {
            click();
        } else if (mButtonFocused) {
            if (mFocusedButton == null) {
                EnterURL();
                return;
            }
            String buttonAction = mFocusedButton.name;
            if (buttonAction.equals("reload")) {
                refreshWebView();
            } else if (buttonAction.equals("forward")) {
                navigateForward();
            } else if (buttonAction.equals("back")) {
                navigateBack();
            } else if (buttonAction.equals("EnterURL")) {
                EnterURL();
            } else if (buttonAction.equals("home")) {
                navigateHome();

            }
        } else {

        }
    }

    @Override
    public void onStep() {
        processTaskQueue();

        pickedObjects = GVRPicker.findObjects(mScene, 0f, 0f, 0f, 0f, 0f, -1f);

        // reset
        mBrowserFocused = false;
        mButtonFocused = false;
        mKeyboardFocused = false;
        if (mFocusedButton != null)
            mFocusedButton.setFocus(false);

        for (GVRPicker.GVRPickedObject pickedObject : pickedObjects) {
            GVRSceneObject obj = pickedObject.getHitObject();

            hitLocation = pickedObject.getHitLocation();

            if (obj.getName().equals("keyboardItem")) {
                mKeyboardFocused = true;
            } else if (obj.getName().equals("webview")) {
                mBrowserFocused = true;
            } else { // NOTE: buttons only for now
                for (Button button : mUiButtons) {
                    if (button.name.equals(obj.getName())) {
                        mFocusedButton = button;
                        button.setFocus(true);
                        mButtonFocused = true;
                    }
                }
            }

            break;
        }
    }

    public void processTaskQueue() {
        if (taskQueue.size() == 0)
            return;

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

    public void onKeyDown(int keyCode, KeyEvent event) {
        if (mBrowserFocused) {
            mWebView.dispatchKeyEvent(event);
        }
    }

    public void onKeyUp(int keyCode, KeyEvent event) {
        if (mBrowserFocused) {
            mWebView.dispatchKeyEvent(event);
        }
    }

    @SuppressWarnings("incomplete-switch")
    public void onSwipe(SwipeDirection swipeDirection, float vX, float vY) {
        Log.d(TAG, "Swipe " + swipeDirection);

        if (mBrowserFocused) {
            scroll(swipeDirection, vX, vY);
        } else {
            switch (swipeDirection) {
                case Up:
                    togglePassthroughCamera(false);
                    break;
                case Down:
                    togglePassthroughCamera(true);
                    break;
                case Forward:
                    break;
                case Backward:
                    break;
            }
        }
    }

    public void scroll(SwipeDirection swipeDirection, float vX, float vY) {
        int n = 3; // tune sensitivity for HMD trackpad

        if (swipeDirection == SwipeDirection.Down) {
            mWebView.flingScroll(0, (int) Math.abs(vY) * -n);
        } else if (swipeDirection == SwipeDirection.Up) {
            mWebView.flingScroll(0, (int) Math.abs(vY) * n);
        } else if (swipeDirection == SwipeDirection.Backward) {
            mWebView.flingScroll((int) Math.abs(vX) * n, 0);
        } else if (swipeDirection == SwipeDirection.Forward) {
            mWebView.flingScroll((int) Math.abs(vX) * -n, 0);
        }
    }

    // debug
    public void showMessage(String message) {
        Toast bread = Toast.makeText(mActivity, message, Toast.LENGTH_SHORT);
        bread.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM, 0, 0);
        // toast.setGravity(Gravity.TOP|Gravity.LEFT, 0, 0);
        bread.setMargin(0f, 0f);
        bread.show();
    }

    //

    private Map<String, GVRSceneObject> objects = new HashMap<String, GVRSceneObject>();
    private Map<String, String> dict = new HashMap<String, String>();

    private Queue<String> taskQueue = new ArrayBlockingQueue<String>(16);

    /* Object */
    public void createNewObject(String name, String type) {
        taskQueue.add(name + ":" + type);
    }

    // make a scene object of type
    public GVRSceneObject createObject(String name, String type) {
        // TODO: implement texturing
        GVRTexture texture = mContext.loadTexture(new GVRAndroidResource(mContext, R.raw.earthmap1k));

        GVRSceneObject obj;
        if (type == "cube")
            obj = new GVRCubeSceneObject(mContext);
        else if (type == "sphere")
            obj = new GVRSphereSceneObject(mContext);
        else if (type == "cylinder")
            obj = new GVRCylinderSceneObject(mContext);
        else // default : plane
            obj = new GVRSceneObject(mContext);

        // obj.setName(name);

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

        obj.getTransform().setRotationByAxis(angle, x, y, z);
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

        // background.setColor(Color.DKGRAY);
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
        return mBackground.getValue();
    }

    private static final String HEX_PATTERN = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$";
    private Pattern pattern;

    /*
     * Set background color, gradient or image
     */
    public void setBackground(String bg) {
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
        if (!mPermissionBackgroundSettableByPage)
            return;

        try {
            final int color = Color.parseColor(colorStr);
            mBackground.setValue(colorStr);
            mContext.runOnGlThread(new Runnable() {
                @Override
                public void run() {
                    mBackground.setColor(color);
                }
            });
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Exception : " + e);
        }
    }

    public void setBackgroundGradient(String gradient) {
        if (!mPermissionBackgroundSettableByPage)
            return;

        String[] _colors = gradient.split(",");

        final int[] colors = new int[_colors.length];

        for (int i = 0; i < _colors.length; i++) {
            colors[i] = Color.parseColor(_colors[i]);
        }

        mBackground.setValue(gradient);

        mContext.runOnGlThread(new Runnable() {
            @Override
            public void run() {
                mBackground.setGradient(colors);
            }
        });
    }

    public void setBackgroundImage(String imageUrl) {
        this.showMessage("set bg image: " + imageUrl);

        if (!mPermissionBackgroundSettableByPage)
            return;

        try {
            InputStream is = new URL(imageUrl).openStream();
            Bitmap bitmap = BitmapFactory.decodeStream(is);

            final GVRTexture texture = new GVRBitmapTexture(mContext, bitmap);

            mContext.runOnGlThread(new Runnable() {
                @Override
                public void run() {
                    mBackground.setImage(texture);
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

    /*
     * JS context method invocation
     */
    public String call(String methodName, String params) {
        return "return_val";
    }

}
