package org.gearvrf.ui;

import org.gearvrf.GVRBitmapTexture;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Bitmap.Config;

public class ProgressBar {

    private GVRContext mGVRContext;

    public String name;
    private GVRSceneObject sceneObject;
    
    private float PROGRESS_BAR_LENGTH;

    public ProgressBar(GVRContext gvrContext, float mBrowserWidth, float mBrowserHeight, float progressBarHeight) {
        mGVRContext = gvrContext;
        
        PROGRESS_BAR_LENGTH = mBrowserWidth;

        int progressBarColor = Color.CYAN;

        Bitmap bmp = Bitmap.createBitmap(1, 1, Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(progressBarColor);

        GVRTexture texture = new GVRBitmapTexture(mGVRContext, bmp);

        sceneObject = new GVRSceneObject(mGVRContext);

        sceneObject.getTransform().setPositionX(-mBrowserWidth/2);

        GVRSceneObject progressBarQuad = new GVRSceneObject(mGVRContext,
                mGVRContext.createQuad(PROGRESS_BAR_LENGTH, progressBarHeight),
                texture);

        progressBarQuad.getTransform().setPosition(PROGRESS_BAR_LENGTH/2, 0f, 0f);

        sceneObject.addChildObject(progressBarQuad);
    }
    
    public GVRSceneObject getSceneObject() {
        return sceneObject;
    }
    
    public void updateProgress(float progress) {
        progress = Math.max(0.0f, Math.min(progress, 1.0f));
        sceneObject.getTransform().setScaleX(progress*PROGRESS_BAR_LENGTH);
    }
    
}
