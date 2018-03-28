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

package org.gearvrf.arcore;

import android.os.Bundle;

import org.gearvrf.GVRActivity;
import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRCameraRig;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMain;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;
import org.gearvrf.GVREventListeners;
import org.gearvrf.GVRPicker;
import org.gearvrf.GVRPicker.GVRPickedObject;
import org.gearvrf.GVRMeshCollider;

import org.gearvrf.scene_objects.GVRCylinderSceneObject;

import org.gearvrf.io.GVRInputManager.ICursorControllerSelectListener;
import org.gearvrf.io.GVRCursorController;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ARCoreActivity extends GVRActivity {

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setMain(new SampleMain());
    }

    private static class SampleMain extends GVRMain {
        private GVRSceneObject mModel;

        @Override
        public void onInit(GVRContext gvrContext) {
            GVRScene scene = gvrContext.getMainScene();
            //scene.setBackgroundColor(1, 1, 1, 1);

            GVRTexture texture = gvrContext.getAssetLoader().loadTexture(new GVRAndroidResource(gvrContext, R.drawable.gearvr_logo));

            GVRTexture redtexture = gvrContext.getAssetLoader().loadTexture(new GVRAndroidResource(gvrContext, R.drawable.red));

            // create a scene object (this constructor creates a rectangular scene
            // object that uses the standard texture shader
            GVRSceneObject sceneObject = new GVRSceneObject(gvrContext, 4.0f, 2.0f, texture);

            // set the scene object position
            sceneObject.getTransform().setPosition(0.0f, -3.0f, -3.0f);
            sceneObject.getTransform().setRotationByAxis(-90.0f, 1.0f, 0.0f, 0.0f);
            sceneObject.attachComponent(new GVRMeshCollider(gvrContext, null, true));

            // add the scene object to the scene graph
            scene.addSceneObject(sceneObject);

            mModel = new GVRCylinderSceneObject(gvrContext, true, redtexture);
            scene.addSceneObject(mModel);

            gvrContext.getInputManager().selectController(new ControllerSelector());

        }

        public class TouchHandler extends GVREventListeners.TouchEvents {
            private GVRSceneObject mDragged = null;
            public void onTouchStart(GVRSceneObject sceneObj, GVRPicker.GVRPickedObject pickInfo) {
                float[] hitLocation = pickInfo.getHitLocation();
                android.util.Log.d("QRCode", "hitLocation: " + 
                        hitLocation[0] + ", " +
                        hitLocation[1] + ", " +
                        hitLocation[2]);
                Vector3f location = new Vector3f(hitLocation[0], hitLocation[1], hitLocation[2]);

                Matrix4f modelMatrix = sceneObj.getTransform().getModelMatrix4f();
                android.util.Log.d("QRCode", "modelMatrix: " + modelMatrix);
                modelMatrix.transformPosition(location);

                if(mModel != null) {
                    android.util.Log.d("QRCode", "  new location: " + 
                            location.x + ", " +
                            location.y + ", " +
                            location.z);

                    mModel.getTransform().setPosition(location.x, location.y, location.z);
                }

                /*
                if(mDragged == null) {
                    GVRPicker picker = pickInfo.picker;
                    GVRCursorController controller = picker.getController();
                    if(controller.startDrag(sceneObj)) {
                        mDragged = sceneObj;
                    }
                }
                */
            }

            public void onTouchEnd(GVRSceneObject sceneObj, GVRPicker.GVRPickedObject pickInfo) {
                /*
                if(mDragged == sceneObj) {
                    GVRPicker picker = pickInfo.picker;
                    GVRCursorController controller = picker.getController();
                    controller.stopDrag();
                    mDragged = null;
                }
                */
            }

            public void onInside(GVRSceneObject sceneObj, GVRPicker.GVRPickedObject pickInfo) {
                //
                // hitLocation x getModelMatrix = world coordinates
                //

            }
        }

        public class ControllerSelector implements ICursorControllerSelectListener {
            public void onCursorControllerSelected(GVRCursorController newController, GVRCursorController oldController) {
                newController.addPickEventListener(new TouchHandler());
            }
        }
    }

}
