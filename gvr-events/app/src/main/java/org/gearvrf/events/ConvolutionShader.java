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

package org.gearvrf.events;

import org.gearvrf.GVRContext;
import org.gearvrf.GVRShaderTemplate;
import org.gearvrf.utility.TextFile;

public class ConvolutionShader extends GVRShaderTemplate
{
    private static String fragTemplate;
    private static String vtxTemplate;

    public ConvolutionShader(GVRContext context)
    {
        super(" float texelWidth; float texelHeight; mat4 convolutionMatrix; ");
        vtxTemplate = TextFile.readTextFile(context.getContext(), R.raw.convolution_vertex);
        fragTemplate = TextFile.readTextFile(context.getContext(), R.raw.convolution_fragment);

        setSegment("VertexTemplate", vtxTemplate);
        setSegment("FragmentTemplate", fragTemplate);
    }
}
