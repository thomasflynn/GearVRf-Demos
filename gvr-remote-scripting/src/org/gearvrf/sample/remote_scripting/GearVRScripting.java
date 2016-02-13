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
import android.content.Context;
import android.graphics.Bitmap;
import org.gearvrf.GVRActivity;
import android.net.wifi.WifiManager;
import java.nio.ByteOrder;
import java.lang.Integer;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.oculus.VRTouchPadGestureDetector;
import com.oculus.VRTouchPadGestureDetector.OnTouchPadGestureListener;
import com.oculus.VRTouchPadGestureDetector.SwipeDirection;

public class GearVRScripting extends GVRActivity implements OnTouchPadGestureListener 
{
    private GearVRScriptingManager mScript;
    private final String TAG = "GearVRScripting";
    String ipAddress;


    private static final String BROWSER_NAME = "Navigator";
    private static final String VERSION = "0.1";

    private VRTouchPadGestureDetector mDetector = null;

    private static final int BUTTON_INTERVAL = 1000;
    private static final int TAP_INTERVAL = 300;

    private long mLatestButton = 0;
    private long mLatestTap = 0;

    private WebView mWebView;
    public final int WEBVIEW_WIDTH = 1200;
    public final int WEBVIEW_HEIGHT = 900;

    private String base = "file:///android_asset/web/";
    private String homeUrl = base+"index.html";


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        ipAddress = getWifiIpAddress(this);

        mScript = new GearVRScriptingManager();
        setScript(mScript, "gvr.xml");
    }

    /* 
     * The following getWifiIpAddress() method is taken from:
     * http://stackoverflow.com/questions/16730711/get-my-wifi-ip-address-android
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

    private void createWebView() {

        mWebView = new WebView(this);

        mWebView.setInitialScale(100);

        int width = this.WEBVIEW_WIDTH, height = this.WEBVIEW_HEIGHT;

        mWebView.measure(width, height);
        mWebView.layout(0, 0, width, height);

        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccess(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setGeolocationEnabled(true);

        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setNeedInitialFocus(false);

        String UA = settings.getUserAgentString();
        settings.setUserAgentString(UA+" VR " + BROWSER_NAME+"/"+VERSION);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                //mScript.reset();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // inject library
                String libSrc = "navi.js";

                //mActivity.injectScript(libSrc);
            }
        });

        WebView.setWebContentsDebuggingEnabled(true);

        mWebView.addJavascriptInterface(new WebAppInterface(this), "_NAVI");

    }


    WebView getWebView() {
        return mWebView;
    }

    void loadHome() {
        mWebView.loadUrl(homeUrl);
    }

    void loadUrl(String url) {
        mWebView.loadUrl(url);
    }

    void executeJS(String js) {
        if (!js.startsWith("javascript"))
            js = "javascript:"+js;
        mWebView.loadUrl(js);
    }

    void injectScript(String jsPath) {
        String script =
           "var s = document.createElement('script');" +
           "s.setAttribute('src', '"+ jsPath +"');" +
           "s.onload = function(){};" +
           "document.body.appendChild(s);";

        mWebView.loadUrl("javascript:"+script);
    }

    @Override
    public void onPause() {
        super.onPause();
        if(mWebView != null) {
            mWebView.pauseTimers();
        }
        mScript.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(mWebView != null) {
            mWebView.resumeTimers();
        }
        mScript.onResume();
    }

    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() > mLatestButton + BUTTON_INTERVAL) {
            mLatestButton = System.currentTimeMillis();

            android.util.Log.v(TAG, "onBackPressed");

            mScript.onButtonDown();
        }
    }

    @Override
    public void onLongPress(MotionEvent e) {
        android.util.Log.v(TAG, "onLongPress");
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            mLatestButton = System.currentTimeMillis();

            mScript.onLongButtonPress(keyCode);
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();

        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            mScript.onKeyDown(keyCode, event);
        } else if (event.getAction() == KeyEvent.ACTION_UP) {
            mScript.onKeyUp(keyCode, event);
        }

        return false;
    }

    @Override
    public boolean onSingleTap(MotionEvent event) {
        android.util.Log.v(TAG, "onSingleTap");
        if (System.currentTimeMillis() > mLatestTap + TAP_INTERVAL) {
            mLatestTap = System.currentTimeMillis();

            mScript.onSingleTap(event);
        }
        return false;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mDetector.onTouchEvent(event);

        mScript.onTouchEvent(event);
        return true;
    }

    @Override
    public boolean onSwipe(MotionEvent e, SwipeDirection swipeDirection,
            float velocityX, float velocityY) {
        mScript.onSwipe(e, swipeDirection, velocityX, velocityY);
        android.util.Log.v(TAG, "onSwipe");
        return false;
    }

    // WebView interface
    class WebAppInterface {
        Context mContext;

        WebAppInterface(Context c) {
            mContext = c;
        }

        /*
        // Can replace all methods with this to avoid duplication in Activity & Script?
        // need to get all arguments... reflection or varargs or JS can encode
        @JavascriptInterface
        public String call(String methodName, String params) {
            return mScript.call(methodName, params);
        }

        @JavascriptInterface
        public String getUUID() {
            return UUID.randomUUID().toString();
        }

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


        // Background 
        @JavascriptInterface
        public String getBackground() {
            return mScript.getBackground();
        }

        @JavascriptInterface
        public void setBackground(String bg) {
            mScript.setBackground(bg);
        }


        // Objects 
        @JavascriptInterface
        public void rotateObject(String name, float angle, float x, float y, float z) {
            mScript.rotateObject(name, angle, x,y,z);
        }

        public void setObjectRotation(String name, float w, float x, float y, float z) {
            mScript.setObjectRotation(name, w,x,y,z);
        }

        @JavascriptInterface
        public void setObjectPosition(String name, float x, float y, float z) {
            mScript.setObjectPosition(name, x,y,z);
        }

        @JavascriptInterface
        public void translateObject(String name, float x, float y, float z) {
            mScript.translateObject(name, x,y,z);
        }

        @JavascriptInterface
        public void setObjectScale(String name, float x, float y, float z) {
            mScript.setObjectScale(name, x,y,z);
        }

        @JavascriptInterface
        public void setObjectVisible(String name, boolean visible) {
            mScript.setObjectVisible(name, visible);
        }

        @JavascriptInterface
        public void createObject(String name, String type) {
            mScript.createNewObject(name, type);
        }

        // Scene 
        @JavascriptInterface
        public void addObjectToScene(String scene, String object) {
            mScript.addObjectToScene(scene, object);
        }

        @JavascriptInterface
        public void removeObjectFromScene(String scene, String object) {
            mScript.removeObjectFromScene(scene, object);
        }
        */
    }


}
