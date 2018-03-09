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
import java.io.File;
import android.os.Environment;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.graphics.SurfaceTexture;

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
import org.gearvrf.GVRCamera;
import org.gearvrf.GVROrthogonalCamera;
import org.gearvrf.GVRPerspectiveCamera;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMain;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;
import org.gearvrf.GVRExternalTexture;
import org.gearvrf.GVRTransform;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRMaterial.GVRShaderType;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRRenderTexture;
import org.gearvrf.GVRRenderTarget;
import org.gearvrf.GVRResourceVolume;
import org.gearvrf.GVRDrawFrameListener;
import org.gearvrf.scene_objects.GVRModelSceneObject;
import org.gearvrf.scene_objects.GVRCameraSceneObject;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.*;
import java.net.*;

import com.google.ar.core.Frame;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.Pose;
import com.google.ar.core.examples.java.computervision.utility.CameraPermissionHelper;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

public class QRCodeActivity extends GVRActivity {

    private Session mARCoreSession;
    private boolean mARCoreIsPaused = true;
    private Camera mCamera;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private QRCodeCallback mQRCodeCallback;
    private int frameNumber = 0;
    private int frameTotal = 60;
    private QRCodeMain mQRMain;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mQRCodeCallback = new QRCodeCallback(this);

        resumeARCoreSession(); // just to create the handle
        pauseARCoreSession();

        mQRMain = new QRCodeMain(mARCoreSession, mCamera, this);
        setMain(mQRMain);
    }

    public void foundQRCode(String decodedText) {
        boolean found = mQRMain.checkForModelURL(decodedText);
        if(found) {
            resumeARCoreSession(); 
        }
    }

    private class QRCodeCallback implements Camera.PreviewCallback {
        private QRCodeActivity mActivity;
        QRCodeCallback(QRCodeActivity activity) {
            mActivity = activity;
        }

        public void onPreviewFrame(byte[] data, Camera camera) {
            frameNumber++;
            if(frameNumber == frameTotal) {
                frameNumber = 0;
                int dstLeft = 0;
                int dstTop = 0;
                int dstWidth = mPreviewWidth;
                int dstHeight = mPreviewHeight;

                // access to the specified range of frames of data
                final PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
                        data, mPreviewWidth, mPreviewHeight, dstLeft, dstTop, dstWidth, dstHeight, false);

                // output a preview image of the picture taken by the camera
                //final Bitmap previewImage = source.renderCroppedGreyscaleBitmap();
                //pictureTakenView.setImageBitmap(previewImage);

                // set this one as the source to decode
                final BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                new DecodeImageTask(mActivity).execute(bitmap);
            }
        }
    }

    private static Hashtable hints;
    static {
        hints = new Hashtable(1);
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
    }

    private class DecodeImageTask extends AsyncTask {
        private QRCodeActivity mActivity;

        DecodeImageTask(QRCodeActivity activity) {
            mActivity = activity;
        }

        @Override
        protected String doInBackground(Object... object) {
            String decodedText = null;
            final Reader reader = new QRCodeReader();
            try {
                BinaryBitmap bitmap = (BinaryBitmap)object[0];
                final Result result = reader.decode(bitmap, hints);
                decodedText = result.getText();
                android.util.Log.d("QRCode", decodedText);
                mActivity.foundQRCode(decodedText);
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


    private void pauseQRCamera() {
        android.util.Log.d("QRCode", "pausing QRCamera");
        if(mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
        }
    }

    private void resumeQRCamera() {
        android.util.Log.d("QRCode", "resuming QRCamera");
        mCamera = Camera.open();
        mCamera.startPreview();
        Camera.Parameters params = mCamera.getParameters();
        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        Camera.Size size = params.getPreviewSize();
        mPreviewWidth = size.width;
        mPreviewHeight = size.height;

        mCamera.setPreviewCallback(mQRCodeCallback);
        if(mQRMain != null) {
            mQRMain.resetPreviewTexture();
        }
    }

    private void pauseARCoreSession() {
        android.util.Log.d("QRCode", "pausing ARCore");
        mARCoreSession.pause();
        mARCoreIsPaused = true;
        if(mQRMain != null) {
            mQRMain.mIsPaused = true;
        }
        resumeQRCamera();
    }

    private boolean mUserRequestedInstall = true;
    private void resumeARCoreSession() {
        pauseQRCamera();

        android.util.Log.d("QRCode", "resuming ARCore");
        if (mARCoreSession != null) {
            mARCoreSession.resume();
            mARCoreIsPaused = false;
            if(mQRMain != null) {
                mQRMain.mIsPaused = false;
            }
        } else {
            Exception exception = null;
            String message = null;

            /*
            try {
                switch(ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall) {
                        case INSTALLED:
                        break;

                        case INSTALL_REQUESTED:
                            mUserRequestedInstall = false;
                        break;
                }
            } catch(UnavailableUserDeclinedInstallationException e) {
                return;
            }
            */

            try {
                // ARCore requires camera permissions to operate. If we did not yet obtain runtime
                // permission on Android M and above, now is a good time to ask the user for it.
                if (!CameraPermissionHelper.hasCameraPermission(this)) {
                    CameraPermissionHelper.requestCameraPermission(this);
                    return;
                }
                mARCoreSession = new Session(/* context= */ this);
                mARCoreIsPaused = false;
                if(mQRMain != null) {
                    mQRMain.mIsPaused = false;
                }

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
                android.util.Log.e(TAG, "Exception creating mARCoreSession", exception);
                return;
            }        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        /*
        resumeARCoreSession();
        */
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mARCoreSession != null && mARCoreIsPaused != true) {
            pauseARCoreSession();
        }

        pauseQRCamera();
    }


    private static class QRCodeMain extends GVRMain {
        private GVRContext mContext;
        private String currentUrl;
        private GVRModelSceneObject mModel;
        private GVRScene mScene;
        private Session mARCoreSession;
        private GVRCameraRig mCameraRig;
        private boolean firstDetection = true;
        private Quaternionf rotationDiff;
        private Quaternionf originalCameraRot;
        private Quaternionf initialDetectionRot;
        private Quaternionf diffCamDet;
        private int cnt = 0;
        private GVRSceneObject mainSceneLocal = null;
        private GVRSceneObject cameraObject;
        private GVRSceneObject cameraPreview;
        private GVRRenderTexture mRenderTexture;
        private GVRRenderTarget mRenderTarget;
        private GVROrthogonalCamera mOrthoCamera;
        private GVRScene mCameraScene;
        private int[] readbackBuffer;
        private int mIndex = 0;
        private int TEXTURE_WIDTH = 1920;
        private int TEXTURE_HEIGHT = 1080;
        private int width = 1920;
        private int height = 1080;
        private Camera mCamera;
        private SurfaceTexture mSurfaceTexture;
        public boolean mIsPaused = true;
        private QRCodeActivity mActivity;

        public QRCodeMain(Session session, Camera camera, QRCodeActivity activity) {
            mARCoreSession = session;
            mCamera = camera;
            mActivity = activity;
        }

        @Override
        public void onInit(GVRContext gvrContext) {
            mContext = gvrContext;
            mScene = gvrContext.getMainScene();
            mScene.setBackgroundColor(1, 1, 1, 1);
            mCameraRig = mScene.getMainCameraRig();
            mainSceneLocal = new GVRSceneObject(gvrContext);
            mCameraRig.addChildObject(mainSceneLocal);

            GVRMesh mesh = gvrContext.createQuad(3.6f, 2.0f);
//            cameraObject = new GVRSceneObject(gvrContext, mesh);

//            cameraObject.getTransform().setPosition(0.0f, 0.0f, -4.0f);
//            mCameraRig.addChildObject(cameraObject);

            // setup render textures and render targets
//            mCameraScene = new GVRScene(gvrContext);
//            mCameraScene.setBackgroundColor(0, 0, 1, 1);
            cameraPreview = new GVRSceneObject(gvrContext, mesh);
            GVRMaterial previewMaterial = new GVRMaterial(gvrContext, GVRShaderType.OES.ID);
            GVRTexture previewTexture = new GVRExternalTexture(gvrContext);
            previewMaterial.setMainTexture(previewTexture);
            cameraPreview.getRenderData().setMaterial(previewMaterial);
            cameraPreview.getRenderData().setRenderingOrder(GVRRenderData.GVRRenderingOrder.BACKGROUND);
            cameraPreview.getRenderData().setDepthTest(false);
//            cameraPreview.getTransform().setPosition(0.0f, 0.0f, -0.1f);
            cameraPreview.getTransform().setPosition(0.0f, 0.0f, -4.0f);
            //mCameraScene.getMainCameraRig().addChildObject(cameraPreview);
            mCameraRig.addChildObject(cameraPreview);
            //mainSceneLocal.addChildObject(cameraPreview);
            //mOrthoCamera = new GVROrthogonalCamera(gvrContext);
            //mOrthoCamera.setBackgroundColor(0.0f, 1.0f, 0.0f, 1.0f);

            //readbackBuffer = new int[TEXTURE_WIDTH * TEXTURE_HEIGHT];

            resetPreviewTexture();

                    /*
            gvrContext.runOnGlThread(new Runnable() {
                @Override
                public void run()
                {
                    mRenderTexture = new GVRRenderTexture(mContext, TEXTURE_WIDTH, TEXTURE_HEIGHT);
                    mRenderTarget = new GVRRenderTarget(mRenderTexture, mCameraScene);
                    mCameraScene.getMainCameraRig().getOwnerObject().attachComponent(mRenderTarget);
                    mRenderTarget.setCamera(mOrthoCamera);
                    mRenderTarget.setEnable(true);
                    cameraObject.getRenderData().getMaterial().setMainTexture(mRenderTexture);
                }
            });
                    */

            mModel = new GVRModelSceneObject(gvrContext);
            mainSceneLocal.addChildObject(mModel);

            gvrContext.registerDrawFrameListener(new GVRDrawFrameListener() {

            @Override
            public void onDrawFrame(float v) {
                if (firstDetection && !mIsPaused) {
                    GVRRenderData renderdata = cameraPreview.getRenderData();
                    GVRMaterial material = renderdata.getMaterial();
                    GVRTexture texture = material.getMainTexture();
                    int glTextureID = texture.getId(); 
                    mARCoreSession.setCameraTextureName(glTextureID);
                }

                if(mIsPaused) {
                    mSurfaceTexture.updateTexImage();
                    return;
                }

                try {
                    // Obtain the current frame from ARSession. When the configuration is set to
                    // UpdateMode.BLOCKING (it is by default), this will throttle the rendering to the
                    // camera framerate.
                    Frame frame = mARCoreSession.update();

//        android.util.Log.d("QRCode", "ARCore frame updated");
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
//                    android.util.Log.d("QRCode", "frame Pose: " + cameraPose);

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
//        android.util.Log.d("QRCode", "ARCore pose calculated: " + -translattionV.x() + ", " + -translattionV.y() + ", " + -translattionV.z());
                } catch (Throwable t) {
                    // Avoid crashing the application due to unhandled exceptions.
                    android.util.Log.e(TAG, "Exception on the OpenGL thread", t);
                }
            }});

        }

        public void writeToFile(int[] data) {
            FileOutputStream out = null;
            File sd = Environment.getExternalStorageDirectory();
            File dest = new File(sd, "blah.jpg");
            try {
                out = new FileOutputStream(dest);
                final Bitmap bitmap = Bitmap.createBitmap(data, TEXTURE_WIDTH, TEXTURE_HEIGHT, Bitmap.Config.ARGB_8888);

                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out);
                out.flush();
                out.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        public void resetPreviewTexture() {
            if(cameraPreview == null) {
                return;
            }
            mContext.runOnGlThread(new Runnable() {
                @Override
                public void run()
                {
                    try {
                        GVRMaterial previewMaterial = new GVRMaterial(mContext, GVRShaderType.OES.ID);
                        GVRTexture previewTexture = new GVRExternalTexture(mContext);
                        previewMaterial.setMainTexture(previewTexture);
                        cameraPreview.getRenderData().setMaterial(previewMaterial);

                        mSurfaceTexture = new SurfaceTexture(previewTexture.getId());
                        mCamera.setPreviewTexture(mSurfaceTexture); // XXX
                        android.util.Log.d("QRCode", "preview texture should be set");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });



        }

        public boolean checkForModelURL(String URL) {
            final String url = URL;

            // check if it ends with .x3d
            // if not, exit
            if(!url.endsWith(".x3d") &&
               !url.endsWith(".dae") &&
               !url.endsWith(".obj") &&
               !url.endsWith(".fbx") ) {
                android.util.Log.d("QRCode", "not a 3d model");
                return false;
            }
            
            // if same as current url, return
            if(currentUrl != null && currentUrl.equals(url)) {
                android.util.Log.d("QRCode", "same as currently loaded url");
                return false;
            }

            mContext.runOnTheFrameworkThread(new Runnable() {
                    public void run() {
                        loadModelFromUrl(url);
                    }
                });

            return true;
        }

        private void loadModelFromUrl(String url) {
            // TODO:
            //      show dialog with url text, 
            //      ask if user wants to load.
            //      if yes, check whether a webpage or 3d model
            //          load appropriate content viewer
            
           
            // otherwise, 
            //      set as new url, 
            currentUrl = url;

            //      remove old model,
            //mScene.removeSceneObject(mModel);

            //  load model and add new model to scene
            android.util.Log.d("QRCode", "going to load: " + url);
            //mModel = mContext.getAssetLoader().loadModel(url, mScene);
            GVRResourceVolume volume = new GVRResourceVolume(mContext, url);
            mContext.getAssetLoader().loadModel(mModel, volume, mScene);
            android.util.Log.d("QRCode", "loaded");
        }


    }
}
