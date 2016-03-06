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

package org.gearvrf.sample.vrbrowser;

import java.util.concurrent.Future;

import org.gearvrf.FutureWrapper;
import org.gearvrf.GVRMaterial;
import org.gearvrf.GVRSceneObject;
import org.gearvrf.GVRRenderData;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRMesh;
import org.gearvrf.GVRTexture;

public class GVRPlaneSceneObject extends GVRSceneObject {

    private static final float SIZE = 0.5f;

    // naming convention for arrays:
    // simple - single quad
    // complex - multiple quads

    private static final float[] SIMPLE_VERTICES = {
            -SIZE, -SIZE, SIZE, // 0
            SIZE, -SIZE, SIZE, // 1
            -SIZE, SIZE, SIZE, // 2
            SIZE, SIZE, SIZE, // 3
    };

    private static final float[] SIMPLE_OUTWARD_NORMALS = {
       0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f };

    private static final float[] SIMPLE_OUTWARD_TEXCOORDS = {
       0.0f, 1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f };

    private static final char[] SIMPLE_OUTWARD_INDICES = { 0, 1, 2, 2, 1, 3 };

    /**
     * Constructs a plane scene object with each side of length 1.
     * 
     * 
     * @param gvrContext
     *            current {@link GVRContext}
     */
    public GVRPlaneSceneObject(GVRContext gvrContext) {
        super(gvrContext);

        createSimplePlane(gvrContext, new GVRMaterial(gvrContext));
    }

    /**
     * Constructs a plane scene object with each side of length 1.
     * 
     * 
     * @param gvrContext
     *            current {@link GVRContext}
     * 
     * @param futureTexture
     *            the texture for face {@code Future<GVRTexture>} is used here
     *            for asynchronously loading the texture.
     */
    public GVRPlaneSceneObject(GVRContext gvrContext, Future<GVRTexture> futureTexture) {
        super(gvrContext);

        GVRMaterial material = new GVRMaterial(gvrContext);
        material.setMainTexture(futureTexture);
        createSimplePlane(gvrContext, material);
    }

    /**
     * Constructs a plane scene object with each side of length 1.
     * 
     * The material is applied to the plane.
     *
     * Use a material with the shader type
     * {@code GVRMaterial.GVRShaderType.Texture} and a {@code GVRTexture}.
     *
     * 
     * @param gvrContext
     *            current {@link GVRContext}
     * 
     * @param material
     *            the material for the face.
     */
    public GVRPlaneSceneObject(GVRContext gvrContext, GVRMaterial material) {
        super(gvrContext);

        createSimplePlane(gvrContext, material);
    }

    /**
     * Constructs a plane scene object with each side of length 1.
     * 
     * Each face is subdivided into NxN quads, where N = segmentNumber is given
     * by user.
     * 
     * 
     * @param gvrContext
     *            current {@link GVRContext}
     * 
     * @param futureTexture
     *            {@code Future<GVRTexture>} is used here for asynchronously
     *            loading the texture.
     * 
     * @param segmentNumber
     *            the segment number along each axis.
     * 
     */
    public GVRPlaneSceneObject(GVRContext gvrContext, Future<GVRTexture> futureTexture, int segmentNumber) {
        super(gvrContext);

        createComplexPlane(gvrContext, futureTexture, segmentNumber);
    }

    public GVRPlaneSceneObject(GVRContext gvrContext, Future<GVRTexture> futureTexture, int xSegments, int ySegments) {
        super(gvrContext);

        createComplexPlane(gvrContext, futureTexture, xSegments, ySegments);
    }

    private void createSimplePlane(GVRContext gvrContext, GVRMaterial material) {

        GVRMesh mesh = new GVRMesh(gvrContext);

        mesh.setVertices(SIMPLE_VERTICES);
        mesh.setNormals(SIMPLE_OUTWARD_NORMALS);
        mesh.setTexCoords(SIMPLE_OUTWARD_TEXCOORDS);
        mesh.setIndices(SIMPLE_OUTWARD_INDICES);

        GVRRenderData renderData = new GVRRenderData(gvrContext);
        renderData.setMaterial(material);
        attachRenderData(renderData);
        renderData.setMesh(mesh);
    }

    private float[] vertices;
    private float[] normals;
    private float[] texCoords;
    private char[] indices;

    private void createComplexPlane(GVRContext gvrContext, Future<GVRTexture> futureTexture, int segmentNumber) {
        createComplexPlane(gvrContext, futureTexture, segmentNumber, segmentNumber);
    }

    private void createComplexPlane(GVRContext gvrContext,
            Future<GVRTexture> futureTexture, int xSegments, int ySegments) {

        GVRSceneObject child = new GVRSceneObject(gvrContext);
        addChildObject(child);
        
        int numQuads = xSegments*ySegments;
        GVRSceneObject[] grandchildren = new GVRSceneObject[numQuads];
        GVRMesh[] subMeshes = new GVRMesh[numQuads];
        
        // 4 vertices (2 triangles) per mesh
        /*
         * TODO: 
         * -use more vertices, but not numPerFace* more as shared
         * -same for normals, etc.
         */
        vertices = new float[4*3];
        normals = new float[4*3];
        texCoords = new float[4*2];

        indices = new char[6];
        
        indices[0] = 0;
        indices[1] = 1;
        indices[2] = 2;

        indices[3] = 1;
        indices[4] = 3;
        indices[5] = 2;
        
        float segmentWidth = 2.0f * SIZE / xSegments;
        float segmentTexCoordWidth = 1.0f / xSegments;
        float segmentHeight = 2.0f * SIZE / ySegments;
        float segmentTexCoordHeight = 1.0f / ySegments;
        
        normals[0] = normals[3] = normals[6] = normals[9] = 0.0f;
        normals[1] = normals[4] = normals[7] = normals[10] = 0.0f;
        normals[2] = normals[5] = normals[8] = normals[11] = 1.0f;
        
        for (int col = 0; col<ySegments; col++) {
            for (int row = 0; row<xSegments; row++) {
                // sub-mesh (col, row)
                int index = col*xSegments+row;
                
                float x0 = -SIZE + segmentWidth * col;
                float y0 = -SIZE + segmentHeight * row;
                float x1 = x0 + segmentWidth;
                float y1 = y0 + segmentHeight;
                float z = SIZE;
                vertices[0] = x0;
                vertices[1] = y0;
                vertices[2] = z;
                vertices[3] = x1;
                vertices[4] = y0;
                vertices[5] = z;
                vertices[6] = x0;
                vertices[7] = y1;
                vertices[8] = z;
                vertices[9] = x1;
                vertices[10] = y1;
                vertices[11] = z;
                
                float s0, s1;
                s0 = col * segmentTexCoordWidth;
                s1 = (col + 1) * segmentTexCoordWidth;
                float t0 = 1.0f - (row + 1) * segmentTexCoordHeight;
                float t1 = 1.0f - row * segmentTexCoordHeight;
                texCoords[0] = s0;
                texCoords[1] = t1;
                texCoords[2] = s1;
                texCoords[3] = t1;
                texCoords[4] = s0;
                texCoords[5] = t0;
                texCoords[6] = s1;
                texCoords[7] = t0;
                
                subMeshes[index] = new GVRMesh(gvrContext);
                subMeshes[index].setVertices(vertices);
                subMeshes[index].setNormals(normals);
                subMeshes[index].setTexCoords(texCoords);
                subMeshes[index].setIndices(indices);
                grandchildren[index] = new GVRSceneObject(gvrContext,
                        new FutureWrapper<GVRMesh>(subMeshes[index]),
                        futureTexture);
                child.addChildObject(grandchildren[index]);
            }
        }

        // attached an empty renderData for parent object, so that we can set some common properties
        GVRRenderData renderData = new GVRRenderData(gvrContext);
        attachRenderData(renderData);
    }
}
