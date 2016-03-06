package org.gearvrf.sample.vrbrowser;

import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRBitmapTexture;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRRenderData.GVRRenderMaskBit;
import org.gearvrf.GVRRenderData.GVRRenderingOrder;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;

public class Background {

    private static final String TAG = "Background";

    public GVRSceneObject container;
    private GVRContext mContext;

    private String value = "0x000000";

    private GVRSceneObject[] objects = new GVRSceneObject[2];

    public Background(GVRContext context) {
        mContext = context;

        container = new GVRSceneObject(mContext);

        GVRMesh sphereMesh = mContext.loadMesh(new GVRAndroidResource(mContext, R.raw.sphere_mesh));

        float s = 15.0f;

        for (int i = 0; i < 2; i++) {
            GVRSceneObject obj = new GVRSceneObject(mContext, sphereMesh);
            obj.getTransform().setScale(s, s, s);
            obj.getRenderData().setRenderMask(i == 0 ? GVRRenderMaskBit.Left : GVRRenderMaskBit.Right);

            obj.getRenderData().setRenderingOrder(GVRRenderingOrder.BACKGROUND);

            objects[i] = obj;

            container.addChildObject(obj);
        }
    }

    public GVRSceneObject getSceneObject() {
        return container;
    }

    /*
     * Return String representation of background -> if image: return average
     * color of image - TODO: implement image background => color
     */
    public String getValue() {
        return this.value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setGradient(int[] colors, float[] stops) {
        GVRTexture texture;
        if (stops != null)
            texture = new Gradient().generate(mContext, colors, stops);
        else
            texture = new Gradient().generate(mContext, colors);

        this.setTexture(texture);
    }

    public void setGradient(int[] colors) {
        setGradient(colors, null);
    }

    public void setColor(int color) {
        Bitmap bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawColor(color);

        GVRBitmapTexture texture = new GVRBitmapTexture(mContext, bitmap);

        this.setTexture(texture);
    }

    public void setImage(GVRTexture texture) {
        this.setTexture(texture);
    }

    public void setCubeMap() {

    }

    private void setTexture(GVRTexture texture) {
        GVRMaterial material = new GVRMaterial(mContext);
        material.setMainTexture(texture);

        for (int i = 0; i < 2; i++) {
            objects[i].getRenderData().setMaterial(material);
        }
    }

    /* For linear gradients */
    private class Gradient {

        private static final String TAG = "Gradient";

        private static final int WIDTH = 512;
        private static final int HEIGHT = 512;

        public Gradient() {

        }

        public GVRBitmapTexture generate(GVRContext context, int[] colors, float[] stops) {
            Bitmap bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            Shader shader = new LinearGradient(0, 0, 0, HEIGHT, colors, stops, TileMode.CLAMP);

            Paint paint = new Paint();
            paint.setShader(shader);

            canvas.drawRect(new Rect(0, 0, WIDTH, HEIGHT), paint);

            GVRBitmapTexture texture = new GVRBitmapTexture(context, bitmap);

            return texture;
        }

        public GVRBitmapTexture generate(GVRContext context, int[] colors) {
            float[] stops = new float[colors.length];

            float stop = 0.0f;
            for (int i = 0; i < colors.length; i++) {
                stops[i] = stop;
                stop += (1.0f / (colors.length - 1));
            }

            return generate(context, colors, stops);
        }

    }

}