#extension GL_OES_EGL_image_external : require
precision highp float;
uniform samplerExternalOES u_texture;

uniform mediump mat4 convolutionMatrix;

varying vec2 textureCoordinate;
varying vec2 leftTextureCoordinate;
varying vec2 rightTextureCoordinate;

varying vec2 topTextureCoordinate;
varying vec2 topLeftTextureCoordinate;
varying vec2 topRightTextureCoordinate;

varying vec2 bottomTextureCoordinate;
varying vec2 bottomLeftTextureCoordinate;
varying vec2 bottomRightTextureCoordinate;

mediump mat3 matrix = mat3(
            0.25, 0.5, 0.25,
            0.5,  1.0, 0.5,
            0.25, 0.5, 0.25
        );


void main()
{
    mediump vec4 bottomColor = texture2D(u_texture, bottomTextureCoordinate);
    mediump vec4 bottomLeftColor = texture2D(u_texture, bottomLeftTextureCoordinate);
    mediump vec4 bottomRightColor = texture2D(u_texture, bottomRightTextureCoordinate);
    mediump vec4 centerColor = texture2D(u_texture, textureCoordinate);
    mediump vec4 leftColor = texture2D(u_texture, leftTextureCoordinate);
    mediump vec4 rightColor = texture2D(u_texture, rightTextureCoordinate);
    mediump vec4 topColor = texture2D(u_texture, topTextureCoordinate);
    mediump vec4 topRightColor = texture2D(u_texture, topRightTextureCoordinate);
    mediump vec4 topLeftColor = texture2D(u_texture, topLeftTextureCoordinate);

    mediump vec4 resultColor = topLeftColor * matrix[0][0] + topColor * matrix[0][1] + topRightColor * matrix[0][2];
    resultColor += leftColor * matrix[1][0] + centerColor * matrix[1][1] + rightColor * matrix[1][2];
    resultColor += bottomLeftColor * matrix[2][0] + bottomColor * matrix[2][1] + bottomRightColor * matrix[2][2];

    gl_FragColor = resultColor;
}



