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

import java.lang.Runnable;
import java.util.concurrent.Future;
import java.io.IOException;
import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRCursorController;
import org.gearvrf.GVRTexture;
import org.gearvrf.GVRMaterial;
import org.gearvrf.io.CursorControllerListener;
import org.gearvrf.io.GVRCursorType;
import org.gearvrf.io.GVRInputManager;
import org.gearvrf.scene_objects.GVRSphereSceneObject;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRRenderData;

public class CursorUtils {
    private GVRContext gvrContext;
    private CustomShaderManager shaderManager;

    public CursorUtils(GVRContext context) {
        gvrContext = context;
    }

    public void enable() {
        gvrContext.runOnGlThread(new Runnable() {
                @Override
                public void run() {
                    // set up the input manager for the main scene
                    GVRInputManager inputManager = gvrContext.getInputManager();
                    inputManager.addCursorControllerListener(listener);
                    for (GVRCursorController cursor : inputManager.getCursorControllers()) {
                        listener.onCursorControllerAdded(cursor);
                    }
                }
            });
    }

    public void disable() {
        gvrContext.runOnGlThread(new Runnable() {
                @Override
                public void run() {
                    // set up the input manager for the main scene
                    GVRInputManager inputManager = gvrContext.getInputManager();
                    inputManager.addCursorControllerListener(listener);
                    for (GVRCursorController cursor : inputManager.getCursorControllers()) {
                        listener.onCursorControllerRemoved(cursor);
                    }
                }
            });
    }

    private CursorControllerListener listener = new CursorControllerListener() {

        @Override
        public void onCursorControllerRemoved(GVRCursorController controller) {
            if (controller.getCursorType() == GVRCursorType.GAZE) {
                controller.resetSceneObject();
                controller.setEnable(false);
            }
        }

        @Override
        public void onCursorControllerAdded(GVRCursorController controller) {
            float DEPTH = -1.5f;

            // Only allow only gaze
            if (controller.getCursorType() == GVRCursorType.GAZE) {
                if(shaderManager == null) {
                    shaderManager = new CustomShaderManager(gvrContext);
                }
                GVRSceneObject cursor = new GVRSphereSceneObject(gvrContext);
                GVRRenderData cursorRenderData = cursor.getRenderData();
                GVRMaterial material = cursorRenderData.getMaterial();
                material.setShaderType(shaderManager.getShaderId());
                material.setVec4(CustomShaderManager.COLOR_KEY, 1.0f, 0.0f, 0.0f, 0.5f);
                gvrContext.getMainScene().addSceneObject(cursor);
                cursor.getRenderData().setDepthTest(false);
                cursor.getRenderData().setRenderingOrder(100000);
                controller.setSceneObject(cursor);
                controller.setPosition(0.0f, 0.0f, DEPTH);
                controller.setNearDepth(DEPTH);
                controller.setFarDepth(DEPTH);
                cursor.getTransform().setScale(-0.015f, -0.015f, -0.015f);
            } else {
                // disable all other types
                controller.setEnable(false);
            }
        }
    };

}


