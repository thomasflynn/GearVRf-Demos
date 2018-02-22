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

import org.gearvrf.GVRActivity;
import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRCameraRig;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMain;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;

import org.gearvrf.scene_objects.GVRCameraSceneObject;


public class QRCodeActivity extends GVRActivity {

    private int width;
    private int height;
    private int frameNumber = 0;
    private int frameTotal = 10;

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

        setMain(new QRCodeMain(camera));
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

        public QRCodeMain(Camera camera) {
            mCamera = camera;
        }

        @Override
        public void onInit(GVRContext gvrContext) {
            GVRScene scene = gvrContext.getMainScene();
            scene.setBackgroundColor(1, 1, 1, 1);

            GVRCameraSceneObject cameraObject = null;
            cameraObject = new GVRCameraSceneObject(gvrContext, 3.6f, 2.0f, mCamera);
            cameraObject.setUpCameraForVrMode(1); // set up 60 fps camera preview.

            if(cameraObject != null) {
                cameraObject.getTransform().setPosition(0.0f, 0.0f, -4.0f);
                scene.addSceneObject(cameraObject);
            }

        }
    }
}
