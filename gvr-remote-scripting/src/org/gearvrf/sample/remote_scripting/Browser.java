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

import org.gearvrf.GVRActivity;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.scene_objects.GVRViewSceneObject;
import org.gearvrf.scene_objects.GVRWebViewSceneObject;

import android.graphics.Color;
import android.webkit.WebView;
import android.widget.EditText;

public class Browser {

    private static final String TAG = "Browser";

    private static int id = 0;

    private GVRSceneObject sceneObject;
    private GVRWebViewSceneObject webViewObject;

    private WebView webView;
    private EditText editText;

    public Browser(GVRContext gvrContext, GVRActivity gvrActivity,
            float width, float height, WebView webView) {
        this(gvrContext, gvrActivity, width, height,
             gvrContext.createQuad(width, height), webView);
    }

    public Browser(GVRContext gvrContext, GVRActivity gvrActivity,
            float width, float height, GVRMesh mesh, WebView webView) {
        sceneObject = new GVRSceneObject(gvrContext);

        this.webView = webView;

        webViewObject = new GVRWebViewSceneObject(gvrContext, mesh, webView);
        //webViewObject = new NaviWebViewSceneObject(gvrContext, mesh, webView);
        webViewObject.setName("webview");//+ id++);

        sceneObject.addChildObject(webViewObject);

        float ratio = 1f / 8f;

        // text navigation bar
        EditTextSceneObject navBar = new EditTextSceneObject(gvrContext, gvrActivity,
                width, width * ratio, 1024, (int)(1024 * ratio), "");

        editText = navBar.getTextView();
        editText.setHint("Web address:");

        navBar.setBackgroundColor(Color.WHITE);
        navBar.setTextSize(20);
        navBar.setTextColor(Color.BLACK);

        navBar.getTransform().setPosition(0f, -1.3f * (height/2f), 0f);

        sceneObject.addChildObject( navBar );
    }
    
    /*private GVRViewSceneObject createWebViewObject(GVRContext gvrContext) {
        
        float aspect = (float)mWebView.getView().getWidth() / mWebView.getView().getHeight();
        float size = 2f;
        
        GVRViewSceneObject webObject = new GVRViewSceneObject(gvrContext, mWebView, size, size);
        webObject.setName("webview");
        webObject.getRenderData().getMaterial().setOpacity(1.0f);
        
        webObject.getTransform().setPosition(0.0f, 0.0f, -mScreenDistance);

        //attachDefaultEyePointee(webObject);
        
        return webObject;
    }*/

    public WebView getWebView() {
        return webView;
    }

    public EditText getEditText() {
        return editText;
    }

    public GVRWebViewSceneObject getScreenObject() {
        return webViewObject;
    }

    public GVRSceneObject getSceneObject() {
        return sceneObject;
    }

}
