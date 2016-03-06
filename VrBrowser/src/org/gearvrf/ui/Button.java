package org.gearvrf.ui;

import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;

public class Button {

    public String name;
    private GVRSceneObject sceneObject;

    private boolean focused = false;

    private float DEFAULT_OPACITY = 0.3f;
    private float FOCUSED_OPACITY = 0.9f;

    public Button(GVRContext mContext, String name, int resId, float size) {
        GVRTexture texture = mContext.loadTexture(
                new GVRAndroidResource(mContext, resId ));

        sceneObject = new GVRSceneObject(mContext, size, size, texture);
        sceneObject.getRenderData().getMaterial().setOpacity(DEFAULT_OPACITY);

        this.name = name;
        sceneObject.setName(name);

        // TODO: set texture, etc
    }

    public GVRSceneObject getSceneObject() {
        return sceneObject;
    }

    public boolean setFocus(boolean focus) {
        if (focus == focused)
            return focused;

        focused = focus;
        float opacity = focused ? FOCUSED_OPACITY : DEFAULT_OPACITY;

        sceneObject.getRenderData().getMaterial().setOpacity(opacity);

        return focused;
    }

}
