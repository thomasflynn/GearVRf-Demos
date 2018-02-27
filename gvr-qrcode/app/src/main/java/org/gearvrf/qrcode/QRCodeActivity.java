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

package org.gearvrf.qrcode;

import android.os.Bundle;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;


import android.os.AsyncTask;
import android.graphics.Bitmap;
import java.util.Hashtable;
import java.io.IOException;

import org.gearvrf.GVRActivity;
import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRCameraRig;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMain;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;
import org.gearvrf.GVRTransform;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRDrawFrameListener;
import org.gearvrf.scene_objects.GVRModelSceneObject;
import org.gearvrf.scene_objects.GVRCameraSceneObject;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;


import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.Pose;
import com.google.ar.core.examples.java.computervision.utility.CameraPermissionHelper;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

public class QRCodeActivity extends GVRActivity {

    private int width;
    private int height;
    private int frameNumber = 0;
    private int frameTotal = 10;
    private Session mSession;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Camera camera = null;
        qrcodeCallback callback = new qrcodeCallback();

        try {
            camera = Camera.open();
            camera.startPreview();
            Camera.Parameters params = camera.getParameters();
            Camera.Size size = params.getPreviewSize();
            width = size.width;
            height = size.height;

            camera.setPreviewCallback(callback);
        } catch(Exception e) {
        }

        createSession();

        setMain(new QRCodeMain(camera, mSession));
    }

    private void createSession() {
        if (mSession == null) {
            Exception exception = null;
            String message = null;

            try {
                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }
                mSession = new Session(/* context= */ this);

            } catch (UnavailableArcoreNotInstalledException e) {
                message = "Please install ARCore";
                exception = e;
            } catch (UnavailableApkTooOldException e) {
                message = "Please update ARCore";
                exception = e;
            } catch (UnavailableSdkTooOldException e) {
                message = "Please update this app";
                exception = e;
            } catch (Exception e) {
                message = "This device does not support AR";
                exception = e;
            }

            if (message != null) {
                //showSnackbarMessage(message, true);
                android.util.Log.e(TAG, "Exception creating mSession", exception);
                return;
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        createSession();

        // Note that order matters - see the note in onPause(), the reverse applies here.
        mSession.resume();
    }

    @Override
    public void onPause() {
        if (mSession != null) {
            mSession.pause();
        }
    }


    private class qrcodeCallback implements Camera.PreviewCallback {
        public void onPreviewFrame(byte[] data, Camera camera) {
            frameNumber++;
            if(frameNumber == frameTotal) {
                frameNumber = 0;
                int dstLeft = 0;
                int dstTop = 0;
                int dstWidth = width;
                int dstHeight = height;

                // access to the specified range of frames of data
                final PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                        data, width, height, dstLeft, dstTop, dstWidth, dstHeight, false);

                // output a preview image of the picture taken by the camera
                //final Bitmap previewImage = source.renderCroppedGreyscaleBitmap();
                //pictureTakenView.setImageBitmap(previewImage);

                // set this one as the source to decode
                final BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                new DecodeImageTask().execute(bitmap);
            }
        }
    }


    private static Hashtable hints;
	static {
		hints = new Hashtable(1);
		hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
	}

    private class DecodeImageTask extends AsyncTask {
		@Override
		protected String doInBackground(Object... object) {
			String decodedText = null;
			final Reader reader = new QRCodeReader();
			try {
                BinaryBitmap bitmap = (BinaryBitmap)object[0];
				final Result result = reader.decode(bitmap, hints);
				decodedText = result.getText();
                android.util.Log.d("QRCode", decodedText);
				//cameraTimer.cancel();
            } catch (Exception e) {
				decodedText = e.toString();
			}
			return decodedText;
		}

            /*
		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);
			Toast.makeText(MainActivity.this, result, Toast.LENGTH_SHORT).show();
			txtScanResult.setText(result);
                });
            }
            */
	}

    private static class QRCodeMain extends GVRMain {
        private Camera mCamera;
        private GVRContext mContext;
        private String currentUrl;
        private GVRModelSceneObject mModel;
        private GVRScene mScene;
        private Session mSession;
        private GVRCameraRig mCameraRig;
        private boolean firstDetection = true;
        private Quaternionf rotationDiff;
        private Quaternionf originalCameraRot;
        private Quaternionf initialDetectionRot;
        private Quaternionf diffCamDet;
        private int cnt = 0;
        private GVRSceneObject mainSceneLocal = null;


        public QRCodeMain(Camera camera, Session session) {
            mCamera = camera;
            mSession = session;
        }

        @Override
        public void onInit(GVRContext gvrContext) {
            mContext = gvrContext;
            mScene = gvrContext.getMainScene();
            mScene.setBackgroundColor(1, 1, 1, 1);
            mCameraRig = mScene.getMainCameraRig();
            mainSceneLocal = new GVRSceneObject(gvrContext);
            mCameraRig.addChildObject(mainSceneLocal);

            GVRCameraSceneObject cameraObject = null;
            cameraObject = new GVRCameraSceneObject(gvrContext, 3.6f, 2.0f, mCamera);
            cameraObject.setUpCameraForVrMode(1); // set up 60 fps camera preview.

            if(cameraObject != null) {
                cameraObject.getTransform().setPosition(0.0f, 0.0f, -4.0f);
                mScene.addSceneObject(cameraObject);
            }


            GVRRenderData renderdata = cameraObject.getRenderData();
            GVRMaterial material = renderdata.getMaterial();
            GVRTexture texture = material.getMainTexture();
            int glTextureID = texture.getId(); // XXX how can this work?  geting a texture id in this thread?  this can't be right, right?
            mSession.setCameraTextureName(glTextureID);

            mModel = new GVRModelSceneObject(gvrContext);

            gvrContext.registerDrawFrameListener(new GVRDrawFrameListener() {

            @Override
            public void onDrawFrame(float v) {

                try {
                    // Obtain the current frame from ARSession. When the configuration is set to
                    // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
                    // camera framerate.
                    Frame frame = mSession.update();

                    // If not tracking, don't draw 3d objects.
                    if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
                        return;
                    }

                    /*
                    // Get camera matrix and draw.
                    // DON'T DELETE !!!!!!!!!!!
                    // (save for reference)
                    float[] viewmtx = new float[16];
                    frame.getViewMatrix(viewmtx, 0);
                    */

                    Pose cameraPose = frame.getCamera().getPose();
                    // Log.d("ARCORE", "frame Pose: " + frame.getPose());

                    float[] translation = new float[3];
                    cameraPose.getTranslation(translation, 0);

                    float[] rotation = new float[4];
                    cameraPose.getRotationQuaternion(rotation, 0);

                    if (firstDetection) {
                        GVRTransform cameraRigTransform = mCameraRig.getHeadTransform().getTransform();
                        originalCameraRot = new Quaternionf(cameraRigTransform.getRotationX(), cameraRigTransform.getRotationY(), cameraRigTransform.getRotationZ(), cameraRigTransform.getRotationW());
                        initialDetectionRot = new Quaternionf(rotation[0], rotation[1], rotation[2], rotation[3]);

                        // Calculate the difference between the coordinate systems of the camera and GVRF
                        diffCamDet = new Quaternionf();
                        initialDetectionRot.difference(originalCameraRot, diffCamDet);

                        firstDetection = false;
                    }

                    // get the rotation of the camera from ARCore
                    Quaternionf detectedRotation = new Quaternionf(rotation[0], rotation[1], rotation[2], rotation[3]);

                    // calculate the rotation relative to the initial position
                    Quaternionf diffDetect = new Quaternionf();
                    initialDetectionRot.difference(detectedRotation, diffDetect);

                    GVRTransform cameraRigTransform = mCameraRig.getHeadTransform().getTransform();
                    Quaternionf curCameraRot = new Quaternionf(cameraRigTransform.getRotationX(), cameraRigTransform.getRotationY(), cameraRigTransform.getRotationZ(), cameraRigTransform.getRotationW());
                    Quaternionf diffCamera = new Quaternionf();
                    originalCameraRot.difference(curCameraRot, diffCamera);
                    detectedRotation.difference(curCameraRot, diffCamDet);

                    Quaternionf toRotate = new Quaternionf();
                    detectedRotation.mul(diffCamDet, toRotate);

                    // invert the rotation because we rotate the scene objects instead of the camera
                    toRotate.invert();

                    // set the rotation
                    mainSceneLocal.getTransform().setRotation(toRotate.w(), toRotate.x(), toRotate.y(), toRotate.z());


                    // calculate and set the translation
                    Vector3f translattionV = new Vector3f(translation[0], translation[1], translation[2]);
                    translattionV.rotate(toRotate);
                    mainSceneLocal.getTransform().setPosition(-translattionV.x(), -translattionV.y(), -translattionV.z());

                    cnt++;
                } catch (Throwable t) {
                    // Avoid crashing the application due to unhandled exceptions.
                    android.util.Log.e(TAG, "Exception on the OpenGL thread", t);
                }
            }});

            }

            public void checkForModelURL(String URL) {
                final String url = URL;
                mContext.runOnTheFrameworkThread(new Runnable() {
                        public void run() {
                            loadModelFromUrl(url);
                        }
                    });

            }

            private void loadModelFromUrl(String url) {
                // TODO:
                //      show dialog with url text, 
                //      ask if user wants to load.
                //      if yes, check whether a webpage or 3d model
                //          load appropriate content viewer
                
                // check if it ends with .x3d
                // if not, exit
                if(!url.endsWith(".x3d")) {
                    return;
                }
                
                // if same as current url, return
                if(currentUrl != null && currentUrl.equals(url)) {
                    return;
                }
                
                // otherwise, 
                //      set as new url, 
                currentUrl = url;

                //      remove old model,
                mScene.removeSceneObject(mModel);

                try {
                    //  load model and add new model to scene
                    mModel = mContext.getAssetLoader().loadModel(url, mScene);
                } catch(IOException e) {
                }
            }


    }
}
