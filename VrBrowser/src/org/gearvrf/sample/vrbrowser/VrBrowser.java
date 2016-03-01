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

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteOrder;
import java.util.UUID;

import org.gearvrf.GVRActivity;
import org.gearvrf.keyboard.util.VRSamplesTouchPadGesturesDetector;
import org.gearvrf.keyboard.util.VRSamplesTouchPadGesturesDetector.SwipeDirection;
import org.gearvrf.scene_objects.view.GVRTextView;
import org.gearvrf.scene_objects.view.GVRWebView;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebView.HitTestResult;
import android.widget.Toast;
import android.webkit.WebViewClient;
import android.webkit.ValueCallback;

public class VrBrowser extends GVRActivity implements VRSamplesTouchPadGesturesDetector.OnTouchPadGestureListener {
    private final static String TAG = "VrBrowser";

    private VrBrowserViewManager mScript;

    private GVRWebView mWebView;
    public final int WEBVIEW_WIDTH = 2560;
    public final int WEBVIEW_HEIGHT = 1440;

    private GVRTextView textView;

    private static final int BUTTON_INTERVAL = 500;
    private static final int TAP_INTERVAL = 300;
    private long mLastDownTime = 0;
    private long mLatestButton = 0;
    private VRSamplesTouchPadGesturesDetector mDetector = null;
    private long mLatestTap = 0;

    public String base = "file:///android_asset/web/";
    public String homeUrl = base + "index.html";
    // public String homeUrl = "http://news.ycombinator.com";

    private String ipAddress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ipAddress = getWifiIpAddress(this);
        
        createWebView();
        createTextView();

        mScript = new VrBrowserViewManager(this);

        setScript(mScript, "gvr.xml");
        mDetector = new VRSamplesTouchPadGesturesDetector(this, this);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void createWebView() {
        mWebView = new GVRWebView(this);
        mWebView.setInitialScale(300);

        int width = this.WEBVIEW_WIDTH;
        int height = this.WEBVIEW_HEIGHT;

        mWebView.measure(width, height);
        mWebView.layout(0, 0, width, height);

        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setGeolocationEnabled(true);

        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setNeedInitialFocus(false);

        mWebView.setVerticalScrollBarEnabled(true);

        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                if (progress >= 100)
                    VrBrowser.this.mScript.UpdateProgress(0);
                else
                    VrBrowser.this.mScript.UpdateProgress(progress * 0.0025f);
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                // TODO: update browser tab title
                VrBrowser.this.mScript.DisplayTitleBar(title);
            }

            @Override
            public void onReceivedIcon(WebView view, Bitmap icon) {
                // TODO: update browser tab icon
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                String message = consoleMessage.lineNumber() + consoleMessage.message();
                return true;
            }

            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                // TODO: create popup which user can confirm/cancel
                Log.d(VrBrowser.class.getSimpleName(), "JsAlert: " + message);
                return false;
            }

            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                // TODO: create confirm dialog
                Log.d(VrBrowser.class.getSimpleName(), "JsConfirm: " + message);
                return false;
            }

            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue,
                    JsPromptResult result) {
                // TODO: create text prompt
                Log.d(VrBrowser.class.getSimpleName(), "JsPrompt: " + message);
                return false;
            }

            @Override
            public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback,
                    WebChromeClient.FileChooserParams fileChooserParams) {
                // TODO: implement file picker
                return false;
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // request.grant();
            }

            //
            @Override
            public void onCloseWindow(WebView window) {
                Log.d(VrBrowser.class.getSimpleName(), "onCloseWindow");
                // TODO: close/remove tab
            }

            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                return false;
            }
        });

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                mScript.reset();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mScript.DisplayUrlBar(url);

                // TODO: inject library into page
                VrBrowser.this.injectLibraryScript();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                mScript.UpdateProgress(0);
                view.loadUrl(url);
                return true;
            }

            @Override
            public boolean shouldOverrideKeyEvent(WebView view, KeyEvent event) {
                return false;
            }

        });

        mWebView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                WebView.HitTestResult hitResult = ((WebView) v).getHitTestResult();

                String type = "";
                if (hitResult != null && hitResult.getType() == HitTestResult.EDIT_TEXT_TYPE) {
                    type = "TEXT";
                } else {
                    type = "NON";
                }

                // VrBrowser.this.mViewManager.showMessage("touch: " + type);

                return false;
            }
        });

        WebView.setWebContentsDebuggingEnabled(true);

        mWebView.addJavascriptInterface(new WebAppInterface(this), "_NAVI");

        mWebView.loadUrl(homeUrl);
    }

    GVRWebView getWebView() {
        return mWebView;
    }

    void createTextView() {
        textView = new GVRTextView(this);

        int width = 4000;
        int height = 1000;

        textView.measure(width, height);
        textView.layout(0, 0, width, height);
    }

    GVRTextView getTextView() {
        return textView;
    }

    private void injectLibraryScript() {
        // TODO: inject script into page
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onLongPress(MotionEvent e) {
        Log.d(VrBrowser.class.getSimpleName(), "onLongPress");
    }

    @Override
    public boolean onSwipe(MotionEvent e, SwipeDirection swipeDirection, float velocityX, float velocityY) {
        Log.d(VrBrowser.class.getSimpleName(), "onSwipe");

        mScript.onSwipe(swipeDirection, velocityX, velocityY);

        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();

        mScript.onPause();
    }

    @Override
    public void onSwiping(MotionEvent e, MotionEvent e2, float velocityX, float velocityY,
            SwipeDirection swipeDirection) {
        Log.d(VRSamplesTouchPadGesturesDetector.DEBUG_TAG, "onSwiping() : Call the stop method");
    }

    @Override
    public void onSwipeOppositeLastDirection() {
        Log.d(VRSamplesTouchPadGesturesDetector.DEBUG_TAG, "onSwipeOppositeLastDirection() : Call the stop method");
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mLatestButton = System.currentTimeMillis();
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onSingleTap(MotionEvent e) {
        Log.d(VrBrowser.class.getSimpleName(), "onSingleTap");
        if (System.currentTimeMillis() > mLatestTap + TAP_INTERVAL) {
            mLatestTap = System.currentTimeMillis();
            mScript.onTap();
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDetector.onTouchEvent(event);
        super.onTouchEvent(event);

        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            mScript.onKeyDown(keyCode, event);
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            mScript.onKeyUp(keyCode, event);
        }

        return super.onKeyDown(keyCode, event);
    }

    /*
     * The following getWifiIpAddress() method is taken from:
     * http://stackoverflow.com/questions/16730711/get-my-wifi-ip-address-
     * android
     */
    protected String getWifiIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(WIFI_SERVICE);
        int ipAddress = wifiManager.getConnectionInfo().getIpAddress();

        // Convert little-endian to big-endianif needed
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddress = Integer.reverseBytes(ipAddress);
        }

        byte[] ipByteArray = BigInteger.valueOf(ipAddress).toByteArray();

        String ipAddressString;
        try {
            ipAddressString = InetAddress.getByAddress(ipByteArray).getHostAddress();
        } catch (UnknownHostException ex) {
            android.util.Log.e(TAG, "Unable to get host address.");
            ipAddressString = null;
        }

        return ipAddressString;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    // WebView interface
    class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        // Can replace all methods with this to avoid duplication in Activity &
        // Script?
        // need to get all arguments... reflection or varargs or JS can encode
        @JavascriptInterface
        public String call(String methodName, String params) {
            return mScript.call(methodName, params);
        }

        // Launch WebView for WebVR content
        @JavascriptInterface
        public void launchWebVR(String url) {
            //
        }

        @JavascriptInterface
        public String getUUID() {
            return UUID.randomUUID().toString();
        }

        // for debugging
        @JavascriptInterface
        public void showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        }

        @JavascriptInterface
        public void refreshWebView() {
            mScript.refreshWebView();
        }

        @JavascriptInterface
        public String getValue(String key) {
            return mScript.getValue(key);
        }

        @JavascriptInterface
        public void setValue(String key, String value) {
            mScript.setValue(key, value);
        }

        /* Background */
        @JavascriptInterface
        public String getBackground() {
            return mScript.getBackground();
        }

        @JavascriptInterface
        public void setBackground(String bg) {
            mScript.setBackground(bg);
        }

        /* Objects */
        @JavascriptInterface
        public void rotateObject(String name, float angle, float x, float y, float z) {
            mScript.rotateObject(name, angle, x, y, z);
        }

        public void setObjectRotation(String name, float w, float x, float y, float z) {
            mScript.setObjectRotation(name, w, x, y, z);
        }

        @JavascriptInterface
        public void setObjectPosition(String name, float x, float y, float z) {
            mScript.setObjectPosition(name, x, y, z);
        }

        @JavascriptInterface
        public void translateObject(String name, float x, float y, float z) {
            mScript.translateObject(name, x, y, z);
        }

        @JavascriptInterface
        public void setObjectScale(String name, float x, float y, float z) {
            mScript.setObjectScale(name, x, y, z);
        }

        @JavascriptInterface
        public void setObjectVisible(String name, boolean visible) {
            mScript.setObjectVisible(name, visible);
        }

        @JavascriptInterface
        public void createObject(String name, String type) {
            mScript.createNewObject(name, type);
        }

        /* Scene */
        @JavascriptInterface
        public void addObjectToScene(String scene, String object) {
            mScript.addObjectToScene(scene, object);
        }

        @JavascriptInterface
        public void removeObjectFromScene(String scene, String object) {
            mScript.removeObjectFromScene(scene, object);
        }
    }

}
