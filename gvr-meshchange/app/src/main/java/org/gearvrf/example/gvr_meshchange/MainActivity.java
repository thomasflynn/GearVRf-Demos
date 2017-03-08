package org.gearvrf.example.gvr_meshchange;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;

import org.gearvrf.GVRActivity;
import org.gearvrf.GVRAndroidResource;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMain;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRTexture;

import java.io.IOException;

public class MainActivity extends GVRActivity {

    private MeshChangeMain mMain;
    private long lastDownTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMain = new MeshChangeMain();
        setMain(mMain);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            lastDownTime = event.getDownTime();
        }

        if (event.getActionMasked() == MotionEvent.ACTION_UP) {
            // check if it was a quick tap
            if (event.getEventTime() - lastDownTime < 200) {
                // pass it as a tap to the Main
                mMain.touch();
            }
        }

        return true;
    }

    private class MeshChangeMain extends GVRMain {
        private GVRScene mainScene;
        private GVRSceneObject child2;
        private GVRMesh child2Mesh1;
        private GVRMesh child2Mesh2;
        private boolean mIsTouched = false;

        public void touch() {
            mIsTouched = true;
        }

        public void onInit(GVRContext gvrContext) throws IOException {

            mainScene = gvrContext.getMainScene();

                        // Load some sample textures...
            final GVRTexture parentTexture = this.getGVRContext().loadTexture(new
                     GVRAndroidResource(this.getGVRContext(), "parentTexture.png"));
            final GVRTexture child1Texture = this.getGVRContext().loadTexture(new
                    GVRAndroidResource(this.getGVRContext(), "child1Texture.png"));
            final GVRTexture child2Texture = this.getGVRContext().loadTexture(new
                    GVRAndroidResource(this.getGVRContext(), "child2Texture.png"));

            // Create the meshes...
            final GVRMesh parentMesh = this.getGVRContext().createQuad(50.0f, 30.0f);
            final GVRMesh child1Mesh = this.getGVRContext().createQuad(30.0f, 15.0f);
            final GVRMesh child2Mesh = this.getGVRContext().createQuad(5.0f, 5.0f);
            final GVRMesh child2Mesh2 = this.getGVRContext().createQuad(140.0f, 35.0f);

            // Create the objects...
            final GVRSceneObject parent = new GVRSceneObject(this.getGVRContext(),
                    parentMesh, parentTexture);
            final GVRSceneObject child1 = new GVRSceneObject(this.getGVRContext(),
                    child1Mesh, child1Texture);
            final GVRSceneObject child2 = new GVRSceneObject(this.getGVRContext(),
                    child2Mesh,  child2Texture);

            // Set the render mask
            parent.getRenderData().setRenderMask(
                    GVRRenderData.GVRRenderMaskBit.Left | GVRRenderData.GVRRenderMaskBit.Right);
            child1.getRenderData().setRenderMask(
                    GVRRenderData.GVRRenderMaskBit.Left | GVRRenderData.GVRRenderMaskBit.Right);
            child2.getRenderData().setRenderMask(
                    GVRRenderData.GVRRenderMaskBit.Left | GVRRenderData.GVRRenderMaskBit.Right);

            // Add the children to the parent object...
            parent.addChildObject(child1);
            parent.addChildObject(child2);

            // Translate the objects...
            parent.getTransform().translate(0.0f, 0.0f, -50.0f);
            child1.getTransform().translate(0.0f, 0.0f, 0.1f);
            child2.getTransform().translate(0.0f, -30.0f, 0.1f);

            // Add the parent to the main scene.
            this.mainScene.addSceneObject(parent);

            // Setting mesh2 here, would work fine...
            //child2.getRenderData().setMesh(child2Mesh2);

            this.child2 = child2;
            this.child2Mesh2 = child2Mesh2;
            this.child2Mesh1 = child2Mesh;

        }

        public void onStep() {
            if (this.mIsTouched) {
                android.util.Log.d(TAG, "Setting mesh 2");
                this.child2.getRenderData().setMesh(this.child2Mesh2);
                this.mIsTouched = false;
            }

        }
    }
}