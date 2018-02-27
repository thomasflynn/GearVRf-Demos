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

package org.gearvrf.gvrx3d360photo;

import android.os.Bundle;
import android.util.Log;

import java.io.IOException;
import java.io.FileNotFoundException;

import java.net.URL;
import java.net.MalformedURLException;


import org.gearvrf.GVRActivity;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMain;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRResourceVolume;
import org.gearvrf.GVRAndroidResource;
import org.gearvrf.scene_objects.GVRModelSceneObject;
import org.gearvrf.GVRCameraRig;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.debug.DebugServer;
import org.gearvrf.io.GVRGearCursorController;
import org.gearvrf.io.GVRCursorController;
import org.gearvrf.io.GVRControllerType;
import org.gearvrf.io.GVRInputManager;
import org.gearvrf.io.GVRInputManager.ICursorControllerSelectListener;


public class X3DPhotoActivity extends GVRActivity {

    private static final String TAG = "X3DPhotoActivity";

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setMain(new X3DPhotoMain());
    }

    private static class X3DPhotoMain extends GVRMain {
        @Override
        public void onInit(GVRContext gvrContext) {
            GVRScene scene = gvrContext.getMainScene();
            GVRCameraRig mainCameraRig = scene.getMainCameraRig();

            String url = "photoviewer.x3d";

            GVRModelSceneObject model = new GVRModelSceneObject(gvrContext);

            try {
                model = gvrContext.getAssetLoader().loadModel(url.toString(), scene);

                // set the scene object position
                model.getTransform().setPosition(0.0f, 0.0f, 0.0f);

            } catch (FileNotFoundException e) {
              Log.d(TAG, "ERROR: FileNotFoundException: " + url);
            } catch (IOException e) {
              Log.d(TAG, "Error IOException = " + e);
            } catch (Exception e) {
              e.printStackTrace();
            }

            // put the cursor at the same depth as the UI and scale it up a bit.
            ICursorControllerSelectListener controllerListener = new ICursorControllerSelectListener() {
                public void onCursorControllerSelected(GVRCursorController newController, GVRCursorController oldController)
                {
                    newController.setCursorDepth(20.0f);
                    GVRSceneObject cursorObject = newController.getCursor();
                    cursorObject.getTransform().setScale(1.0f, 1.0f, 1.0f);
                }
            };

            gvrContext.getInputManager().selectController(controllerListener);

        }
    }
}
