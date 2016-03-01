package org.gearvrf.ui;

import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;

public class UICursor {

    private static final String TAG = "UICursor";

    private GVRSceneObject sceneObject;
    private float mDefaultDistance = 2.0f;

    public UICursor(GVRContext gvrContext, int resourceId) {
        GVRTexture texture = gvrContext.loadTexture(
                new GVRAndroidResource( gvrContext, resourceId) );

        sceneObject = new GVRSceneObject(gvrContext, 0.1f, 0.1f, texture);

        sceneObject.getRenderData().setDepthTest(false);
        sceneObject.getRenderData().setRenderingOrder(100000);

        this.setDistance( mDefaultDistance );
    }

    public GVRSceneObject getSceneObject() {
        return sceneObject;
    }

    public void setDistance(float distance) {
        sceneObject.getTransform().setPositionZ( -distance );
    }

}
