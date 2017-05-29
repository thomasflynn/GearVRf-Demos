attribute vec3 a_position;
attribute vec2 a_texcoord;

uniform highp float texelWidth; 
uniform highp float texelHeight; 

varying vec2 textureCoordinate;
varying vec2 leftTextureCoordinate;
varying vec2 rightTextureCoordinate;

varying vec2 topTextureCoordinate;
varying vec2 topLeftTextureCoordinate;
varying vec2 topRightTextureCoordinate;

varying vec2 bottomTextureCoordinate;
varying vec2 bottomLeftTextureCoordinate;
varying vec2 bottomRightTextureCoordinate;

void main()
{
    gl_Position = vec4(a_position, 1.0);

    vec2 widthStep = vec2(texelWidth, 0.0);
    vec2 heightStep = vec2(0.0, texelHeight);
    vec2 widthHeightStep = vec2(texelWidth, texelHeight);
    vec2 widthNegativeHeightStep = vec2(texelWidth, -texelHeight);

    textureCoordinate = a_texcoord.xy;
    leftTextureCoordinate = a_texcoord.xy - widthStep;
    rightTextureCoordinate = a_texcoord.xy + widthStep;

    topTextureCoordinate = a_texcoord.xy - heightStep;
    topLeftTextureCoordinate = a_texcoord.xy - widthHeightStep;
    topRightTextureCoordinate = a_texcoord.xy + widthNegativeHeightStep;

    bottomTextureCoordinate = a_texcoord.xy + heightStep;
    bottomLeftTextureCoordinate = a_texcoord.xy - widthNegativeHeightStep;
    bottomRightTextureCoordinate = a_texcoord.xy + widthHeightStep;
}



