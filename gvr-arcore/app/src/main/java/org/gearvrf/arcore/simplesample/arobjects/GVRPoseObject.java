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

package org.gearvrf.arcore.simplesample.arobjects;

import android.opengl.Matrix;

import com.google.ar.core.Pose;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRSceneObject;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Represents a ARCore pose in the scene.
 */
public abstract class GVRPoseObject extends GVRSceneObject {
    // Aux matrix to convert from AR world space to AR cam space.
    protected static float[] mModelViewMatrix = new float[16];
    // Represents a AR Pose at GVRf's world space
    protected float[] mPoseMatrix = new float[16];

    private Matrix4f mRotation = new Matrix4f();
    private Vector3f mScale = new Vector3f(1.0f, 1.0f, 1.0f);
    private Vector3f mTranslation = new Vector3f(0.0f, 0.0f, 0.0f);

    public GVRPoseObject(GVRContext gvrContext) {
        super(gvrContext);
    }

    /**
     * Rotates matrix m in place by angle a (in degrees)
     * around the axis (x, y, z).
     *
     * @param a angle to rotate in degrees.
     * @param x X axis component
     * @param y Y axis component
     * @param z Z axis component
     */
    public void rotate(float a, float x, float y, float z) {
        mRotation.rotate(a, x, y, z);
    }

    /**
     * Set rotation matrix
     *
     * @param a angle to rotate in degrees.
     * @param x X axis component
     * @param y Y axis component
     * @param z Z axis component
     */
    public void setRotation(float a, float x, float y, float z) {
        mRotation.rotation(a, x, y, z);
    }

    /**
     * Scales matrix m in place by x, y, and z.
     *
     * @param x scale factor x
     * @param y scale factor y
     * @param z scale factor z
     */
    public void scale(float x, float y, float z) {
        mScale.mul(x, y, z);
    }

    /**
     * Sets scale matrix m in place by x, y, and z.
     *
     * @param x scale factor x
     * @param y scale factor y
     * @param z scale factor z
     */
    public void setScale(float x, float y, float z) {
        mScale.set(x, y, z);
    }

    /**
     * Translates matrix m by x, y, and z.
     *
     * @param x translation factor x
     * @param y translation factor y
     * @param z translation factor z
     */
    public void translate(float x, float y, float z) {
        mTranslation.mul(x, y, z);
    }

    /**
     * Sets translation matrix m by x, y, and z.
     *
     * @param x translation factor x
     * @param y translation factor y
     * @param z translation factor z
     */
    public void setTranslation(float x, float y, float z) {
        mTranslation.set(x, y, z);
    }

    /**
     * @return Rotation yaw in degrees
     */
    public float getRotationYaw() {
        Vector3f rotationAngles = new Vector3f();
        mRotation.getEulerAnglesZYX(rotationAngles);

        return (float)Math.toDegrees(rotationAngles.y);
    }

    public float getScaleX() {
        return mScale.x;
    }

    public float getScaleY() {
        return mScale.y;
    }

    public float getScaleZ() {
        return mScale.z;
    }

    /**
     * Returns the ARCore Pose matrix in GVRf's world space
     *
     * @return The pose matrix in GVRf's world space.
     */
    public float[] getPoseMatrix() {
        return mPoseMatrix;
    }

    /**
     * Converts from ARCore world space to GVRf's world space
     *
     * @param pose AR Core Pose instance
     * @param arViewMatrix Phone's camera view matrix
     * @param vrCamMatrix GVRf Camera matrix
     * @param scale Scale from AR to GVRf world
     */
    public void update(Pose pose, float[] arViewMatrix, float[] vrCamMatrix, float scale) {
        pose.toMatrix(mPoseMatrix, 0);

        ar2gvr(arViewMatrix, vrCamMatrix, scale);
    }

    /**
     * Converts from AR world space to GVRf world space.
     */
    private void ar2gvr(float[] arViewMatrix, float[] vrCamMatrix, float scale) {
        // From AR world space to AR camera space.
        Matrix.multiplyMM(mModelViewMatrix, 0, arViewMatrix, 0, mPoseMatrix, 0);
        // From AR Camera space to GVRf world space
        Matrix.multiplyMM(mPoseMatrix, 0, vrCamMatrix, 0, mModelViewMatrix, 0);

        // Rotation
        mRotation.get(mModelViewMatrix);
        Matrix.multiplyMM(mPoseMatrix, 0, mPoseMatrix,0, mModelViewMatrix,0);

        // Real world scale
        Matrix.scaleM(mPoseMatrix, 0,
                scale * mScale.x, scale * mScale.y, scale * mScale.z);

        // Rela world translation
        mPoseMatrix[12] = (mPoseMatrix[12] + mTranslation.x) * scale;
        mPoseMatrix[13] = (mPoseMatrix[13] + mTranslation.y) * scale;
        mPoseMatrix[14] = (mPoseMatrix[14] + mTranslation.z) * scale;

        getTransform().setModelMatrix(mPoseMatrix);
    }
}
