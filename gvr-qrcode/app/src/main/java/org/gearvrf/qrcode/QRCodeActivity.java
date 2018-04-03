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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;

import android.os.Bundle;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.view.MotionEvent;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.gearvrf.GVRActivity;
import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRCameraRig;
import org.gearvrf.GVRCamera;
import org.gearvrf.GVROrthogonalCamera;
import org.gearvrf.GVRPerspectiveCamera;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMain;
import org.gearvrf.GVRMeshCollider;
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
import org.gearvrf.GVREventListeners;
import org.gearvrf.GVRPicker;
import org.gearvrf.GVRSphereCollider;
import org.gearvrf.GVRPicker.GVRPickedObject;
import org.gearvrf.io.GVRInputManager.ICursorControllerSelectListener;
import org.gearvrf.io.GVRCursorController;
import org.gearvrf.scene_objects.GVRSphereSceneObject;
import org.gearvrf.scene_objects.GVRModelSceneObject;
import org.gearvrf.scene_objects.GVRCameraSceneObject;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.nio.*;
import java.net.*;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Config;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Point;
import com.google.ar.core.PointCloud;
import com.google.ar.core.Point.OrientationMode;
import com.google.ar.core.Pose;
import com.google.ar.core.Session;
import com.google.ar.core.Trackable;
import com.google.ar.core.TrackingState;

import com.google.ar.core.examples.java.computervision.utility.CameraPermissionHelper;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;

public class QRCodeActivity extends GVRActivity implements SpeechRecognizerManager.OnResultListener {

    private Session mARCoreSession;
    private boolean mARCoreIsPaused = true;
    private Camera mCamera;
    private int mPreviewWidth;
    private int mPreviewHeight;
    private QRCodeCallback mQRCodeCallback;
    private int frameNumber = 0;
    private int frameTotal = 60;
    private QRCodeMain mQRMain;
    private SpeechRecognizerManager mSpeechRecognizerManager;
    private Handler mHandler;
    private long lastDownTime = 0;

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mHandler = new Handler();

        mQRCodeCallback = new QRCodeCallback(this);

        resumeARCoreSession(); // just to create the handle
        pauseARCoreSession();

        mQRMain = new QRCodeMain(mARCoreSession, mCamera, this);

        /*
        mSpeechRecognizerManager = new SpeechRecognizerManager(this);
        mSpeechRecognizerManager.setOnResultListner(this);
        */

        setMain(mQRMain);
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    /*
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            lastDownTime = event.getDownTime();
        }

        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            // check if it was a quick tap
            if (event.getEventTime() - lastDownTime < 200) {
                // pass it as a tap to the Main
                float x,y;
                x = event.getX();
                y = event.getY();
                mQRMain.onTap(event, x, y);
            }
        }

        return true;
    }
    */


    @Override
    public void OnResult(ArrayList<String> commands) {
        for(String command:commands)
        {
            if (command.equals("close")){
                android.util.Log.d("QRCode", "closing scene");
                pauseARCoreSession();
                mQRMain.closeScene();
                return;
            } else {
                android.util.Log.d("QRCode", "OnResult, You said: "+command);
            }

        }
    }

    public void foundQRCode(String decodedText) {
        boolean found = mQRMain.checkForModelURL(decodedText);
        if(found) {
            resumeARCoreSession(); 
            if(mSpeechRecognizerManager != null) {
                mSpeechRecognizerManager.startListening();
            }
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
        if(mSpeechRecognizerManager != null) {
            mSpeechRecognizerManager.stopListening();
        }
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
            mQRMain.resetPreviewTexture(mCamera);
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
        } else {
            pauseQRCamera();
        }

        if(mSpeechRecognizerManager != null) {
            mSpeechRecognizerManager.destroy();
        }
    }


    private static class QRCodeMain extends GVRMain {
        private GVRContext mContext;
        private String currentUrl;
        private GVRModelSceneObject mModel;
        private GVRSceneObject mSphere;
        private GVRScene mScene;
        private Session mARCoreSession;
        private GVRCameraRig mCameraRig;
        private boolean firstDetection = true;
        private Quaternionf rotationDiff;
        private Quaternionf originalCameraRot;
        private Quaternionf initialDetectionRot;
        private Quaternionf diffCamDet;
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
        private boolean placementTap = false;
        private float placementX;
        private float placementY;
        private MotionEvent placementEvent;
        private final ArrayList<Anchor> anchors = new ArrayList<>();
        private List<GVRSceneObject> planeObjects = new ArrayList<GVRSceneObject>();
        private GVRTexture planeTexture;

        public QRCodeMain(Session session, Camera camera, QRCodeActivity activity) {
            mARCoreSession = session;
            mCamera = camera;
            mActivity = activity;
        }

        public void onTap(MotionEvent event, float x, float y) {
            placementEvent = event;
            placementX = x;
            placementY = y;
            placementTap = true;
        }


        @Override
        public void onInit(GVRContext gvrContext) {
            mContext = gvrContext;
            mScene = gvrContext.getMainScene();
            mScene.setBackgroundColor(1, 1, 1, 1);
            mCameraRig = mScene.getMainCameraRig();

            mainSceneLocal = new GVRSceneObject(gvrContext);
            mCameraRig.addChildObject(mainSceneLocal);
        
            planeTexture = gvrContext.getAssetLoader().loadTexture(new GVRAndroidResource(gvrContext, R.drawable.gearvr_logo));

            for(int i=0; i<20; i++) {
                GVRSceneObject plane = new GVRSceneObject(gvrContext, 1.0f, 1.0f, planeTexture);
                plane.getTransform().setRotationByAxis(-90.0f, 1.0f, 0.0f, 0.0f);
                plane.setEnable(false);
                plane.attachComponent(new GVRMeshCollider(gvrContext, null, true));

                mainSceneLocal.addChildObject(plane);
                planeObjects.add(plane);
            }

            gvrContext.getInputManager().selectController(new ControllerSelector());

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

            resetPreviewTexture(mCamera);

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

                if(mIsPaused && mSurfaceTexture != null) {
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

//        android.util.Log.d("QRCode", "ARCore pose calculated: " + -translattionV.x() + ", " + -translattionV.y() + ", " + -translattionV.z());

                    if(placementTap) {
android.util.Log.d("QRCode", "tap!");
                        placementTap = false;
                        float x = 2960 / 2;
                        float y = 1440 / 2;

                        //android.util.Log.d("QRCode", "x, y = " + x + ", " + y);
                        //android.util.Log.d("QRCode", "placement x, y = " + placementX + ", " + placementY);
                        //android.util.Log.d("QRCode", "placement Event = " + placementEvent);
                        for(HitResult hit : frame.hitTest(placementEvent)) {
android.util.Log.d("QRCode", "!!!!!!!!!!!!!!!");
android.util.Log.d("QRCode", "a plane was hit");
android.util.Log.d("QRCode", "!!!!!!!!!!!!!!!");
                            // check if any plane was hit
                            Trackable trackable = hit.getTrackable();

                            // creates an anchor if a plane or an oriented point was hit
                            if((trackable instanceof Plane && ((Plane) trackable).isPoseInPolygon(hit.getHitPose())) ||
                              (trackable instanceof Point &&
                              ((Point) trackable).getOrientationMode() == Point.OrientationMode.ESTIMATED_SURFACE_NORMAL)) {
                                // Hits are sorted by depth.  consider only closest hit on a plane or iented point.
                                // cpa the number of objects created.  this avoids overloading ARCore.
                                if(anchors.size() >= 1) {
                                    anchors.get(0).detach();
                                    anchors.remove(0);
                                }

                                // Adding an anchor tells ARCore that is should track this position in space.
                                // This anchor is created on the Plane to place the 3d model in the correct position relative both to the word and the plane.
                                anchors.add(hit.createAnchor());
android.util.Log.d("QRCode", "an anchor was added");
                                break;
                            }
                        }
                    }

                    // visualize planes
                    Collection<Plane> allPlanes = mARCoreSession.getAllTrackables(Plane.class);
//android.util.Log.d("QRCode", "going to iterate through planes");
                    for(Plane plane : allPlanes) {
                        int i = 0;
//android.util.Log.d("QRCode", "plane: " + i);
                        if(plane.getTrackingState() != TrackingState.TRACKING || plane.getSubsumedBy() != null) {
//android.util.Log.d("QRCode", "  not tracking");
                            continue;
                        }

                        if(plane.getType() != com.google.ar.core.Plane.Type.HORIZONTAL_UPWARD_FACING) {
//android.util.Log.d("QRCode", "  not upward facing");
                            continue;
                        }

                        Pose center = plane.getCenterPose();

                        float[] position = new float[3];
                        center.getTranslation(position, 0);

                        float scaleX = plane.getExtentX();
                        float scaleZ = plane.getExtentZ();

                        planeObjects.get(i).getTransform().setPosition(position[0], position[1], position[2]);
                        planeObjects.get(i).getTransform().setScale(scaleX, scaleZ, 1.0f);
                        planeObjects.get(i).setEnable(true);
//android.util.Log.d("QRCode", "  position: " + position[0] + ", " + position[1] + ", " + position[2]);
//android.util.Log.d("QRCode", "  scale   : " + scaleX + ", " + scaleZ);
                        i++;
                    }

                    // visualize anchors created by touch
                    for(Anchor anchor : anchors) {
                        if(anchor.getTrackingState() != TrackingState.TRACKING || mModel == null) {
                            continue;
                        }
android.util.Log.d("QRCode", "anchor found");

                        // get current pose of an anchor in world space.
                        // the anchor pose is updated during calls to mARCoreSession.update() as ARCore refines its estimate of the world.
                        Pose anchorPose = anchor.getPose();
android.util.Log.d("QRCode", "anchor pose: " + anchorPose);

                        mModel.getTransform().setPosition(anchorPose.tx(),
                                                        anchorPose.ty(),
                                                        anchorPose.tz());

                        mModel.getTransform().setRotation(anchorPose.qw(),
                                                        anchorPose.qx(),
                                                        anchorPose.qy(),
                                                        anchorPose.qz());
                    }

                } catch (Throwable t) {
                    // Avoid crashing the application due to unhandled exceptions.
                    android.util.Log.e(TAG, "Exception on the OpenGL thread", t);
                }
            }});

        }

        public class TouchHandler extends GVREventListeners.TouchEvents {
            private GVRSceneObject mDragged = null;
            private boolean mModelIsRotating = false;
            private float mYaw = 0;
            private float mHitX = 0;

            public void onTouchStart(GVRSceneObject sceneObj, GVRPicker.GVRPickedObject pickInfo) {
                if(sceneObj == mModel || sceneObj == mSphere) {
                    mModelIsRotating = true;
                    android.util.Log.d("QRCode", "onTouchBegin, mModel is now rotating");
                    mYaw = mModel.getTransform().getRotationYaw();
                    mYaw = mSphere.getTransform().getRotationYaw();
                    float[] hitLocation = pickInfo.getHitLocation();
                    mHitX = hitLocation[0];
                    return;
                }

                boolean planeHit = false;
                // otherwise it was a plane
                for(int i=0; i<20 && !planeHit; i++) {
                    if(sceneObj == planeObjects.get(i)) {
                        android.util.Log.d("QRCode", "onTouchBegin, plane " + i + " was hit");
                        planeHit = true;
                    }
                }

                if(!planeHit) {
                    return;
                }
                
                //
                // hitLocation x getModelMatrix = world coordinates
                //
                float[] hitLocation = pickInfo.getHitLocation();
                /*
                android.util.Log.d("QRCode", "hitLocation: " + 
                        hitLocation[0] + ", " +
                        hitLocation[1] + ", " +
                        hitLocation[2]);
                */
                Vector3f location = new Vector3f(hitLocation[0], hitLocation[1], hitLocation[2]);

                Matrix4f modelMatrix = sceneObj.getTransform().getModelMatrix4f();
                //android.util.Log.d("QRCode", "modelMatrix: " + modelMatrix);
                modelMatrix.transformPosition(location);

                if(mModel != null) {
                    /*
                    android.util.Log.d("QRCode", "new location: " + 
                            location.x + ", " + 
                            location.y + ", " + 
                            location.z);
                    */

                    mModel.getTransform().setPosition(location.x, location.y, location.z);
                    mSphere.getTransform().setPosition(location.x, location.y, location.z);
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
                if(sceneObj == mModel || sceneObj == mSphere) {
                    mModelIsRotating = false;
                    android.util.Log.d("QRCode", "onTouchEnd, mModel not rotating");
                }

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
                if(sceneObj != mSphere) {//XXX
                    android.util.Log.d("QRCode", "onInside, not mModel, returning");
                    return;
                } else {
                    android.util.Log.d("QRCode", "onInside, is mModel");
                }

                if(!mModelIsRotating) { 
                    android.util.Log.d("QRCode", "onInside, mModel not rotating");
                    return; 
                }

                float[] hitLocation = pickInfo.getHitLocation();
                float diffX = hitLocation[0] - mHitX;

                android.util.Log.d("QRCode", "onInside, mYaw = " + mYaw);

                float angle = mYaw + (diffX * 80);
                android.util.Log.d("QRCode", "onInside, mHitX = " + mHitX);
                android.util.Log.d("QRCode", "onInside, location[0] = " + hitLocation[0]);
                android.util.Log.d("QRCode", "onInside, diffX = " + diffX);
                android.util.Log.d("QRCode", "onInside, angle = " + angle);
                mSphere.getTransform().setRotationByAxis(angle, 0.0f, 1.0f, 0.0f);
                mModel.getTransform().setRotationByAxis(angle, 0.0f, 1.0f, 0.0f);

            }
        }

        public class ControllerSelector implements ICursorControllerSelectListener {
            public void onCursorControllerSelected(GVRCursorController newController, GVRCursorController oldController) {
                newController.addPickEventListener(new TouchHandler());
            }
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

        public void resetPreviewTexture(Camera camera) {
            if(camera == null) {
            //    return;
            }

            mCamera = camera;
            if(cameraPreview == null || mCamera == null) {
                return;
            }
            mContext.runOnGlThreadPostRender(5, new Runnable() {
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
            mScene.removeSceneObject(mModel);

            final String urlString = url;
            //  load model and add new model to scene
            android.util.Log.d("QRCode", "going to load: " + urlString);
            //GVRResourceVolume volume = new GVRResourceVolume(mContext, url);
            try {
                mModel = mContext.getAssetLoader().loadModel(urlString, new AssetHandler());
                //mainSceneLocal.addChildObject(mModel);
                mSphere = new GVRSphereSceneObject(mContext, true, planeTexture);
                mSphere.getTransform().setPosition(0.0f, 0.0f, -5.0f);
                mSphere.getTransform().setScale(0.1f, 0.1f, 0.1f);
                mainSceneLocal.addChildObject(mSphere);
                GVRSphereCollider sphere = new GVRSphereCollider(mContext);
                sphere.setRadius(1.0f);
                mSphere.attachComponent(sphere);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public class AssetHandler extends GVREventListeners.AssetEvents {
            public void onAssetLoaded(GVRContext context, GVRSceneObject model, String filePath, java.lang.String errors) { 
                GVRSphereCollider sphere = new GVRSphereCollider(mContext);
                android.util.Log.d("QRCode", "onAssetLoaded");
                android.util.Log.d("QRCode", "loaded and collider added with radius: " + mModel.getBoundingVolume().radius);
                android.util.Log.d("QRCode", "center: " + mModel.getBoundingVolume().center);
                android.util.Log.d("QRCode", "minCorner: " + mModel.getBoundingVolume().minCorner);
                android.util.Log.d("QRCode", "maxCorner: " + mModel.getBoundingVolume().maxCorner);
                //sphere.setRadius(mModel.getBoundingVolume().radius);
                sphere.setRadius(1.0f);
                mModel.attachComponent(sphere);
                //mSphere.attachComponent(sphere);
                //android.util.Log.d("QRCode", "loaded and collider added");
            }

            public void onModelLoaded(GVRContext context, GVRSceneObject model, String filePath) { 
                GVRSphereCollider sphere = new GVRSphereCollider(mContext);
                android.util.Log.d("QRCode", "onModelLoaded");
                android.util.Log.d("QRCode", "loaded and collider added with radius: " + mModel.getBoundingVolume().radius);
                android.util.Log.d("QRCode", "center: " + mModel.getBoundingVolume().center);
                android.util.Log.d("QRCode", "minCorner: " + mModel.getBoundingVolume().minCorner);
                android.util.Log.d("QRCode", "maxCorner: " + mModel.getBoundingVolume().maxCorner);
                //sphere.setRadius(mModel.getBoundingVolume().radius);
                //mModel.attachComponent(sphere);
                //android.util.Log.d("QRCode", "loaded and collider added");
            }

        }

        public void closeScene() {
            for(int i=0; i<20; i++) {
                planeObjects.get(i).setEnable(false);
            }
            if(mScene != null && mModel != null) {
                mainSceneLocal.removeChildObject(mModel);
                mModel = null;
                currentUrl = null;
            }
        }

        public boolean onBackPress() {
            if(mIsPaused) {
                return false;
            } 
            mActivity.pauseARCoreSession();
            closeScene();
            return true;
        }

    }
}
