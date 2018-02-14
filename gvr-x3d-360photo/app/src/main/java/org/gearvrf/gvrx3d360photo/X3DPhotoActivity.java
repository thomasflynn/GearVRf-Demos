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
import org.gearvrf.GVRCursorController;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;
import org.gearvrf.debug.DebugServer;
import org.gearvrf.io.GVRControllerType;
import org.gearvrf.io.GVRInputManager;

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
            final DebugServer debug = gvrContext.startDebugServer();
            GVRScene scene = gvrContext.getMainScene();
            GVRCameraRig mainCameraRig = scene.getMainCameraRig();

            String filename = "photoviewer.x3d";
            //String url = new String(filename);
            String url = new String("http://172.28.4.157/~flynnt/models/x3d/" + filename);

            GVRModelSceneObject model = new GVRModelSceneObject(gvrContext);

            try {
                model = gvrContext.getAssetLoader().loadModel(url.toString(), scene);

                // set the scene object position
                model.getTransform().setPosition(0.0f, 0.0f, 0.0f);

                // add the scene object to the scene graph
                scene.addSceneObject(model);
            } catch (FileNotFoundException e) {
              Log.d(TAG, "ERROR: FileNotFoundException: " + filename);
            } catch (IOException e) {
              Log.d(TAG, "Error IOException = " + e);
            } catch (Exception e) {
              e.printStackTrace();
            }

            gvrContext.getInputManager().selectController();

        }
    }
}
