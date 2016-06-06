package org.gearvrf.gvrmeshanimation;

import java.io.IOException;

import org.gearvrf.GVRActivity;
import org.gearvrf.GVRContext;
import org.gearvrf.GVRCameraRig;
import org.gearvrf.GVRScene;
import org.gearvrf.GVRMain;
import org.gearvrf.animation.GVRAnimation;
import org.gearvrf.animation.GVRAnimationEngine;
import org.gearvrf.animation.GVRRepeatMode;
import org.gearvrf.animation.GVROnStart;
import org.gearvrf.scene_objects.GVRModelSceneObject;

import android.util.Log;

import com.google.vrtoolkit.cardboard.audio.CardboardAudioEngine;

public class MeshAnimationMain extends GVRMain {

    private GVRContext mGVRContext;
    private GVRModelSceneObject mCharacter;
    private static final String SOUND_FILE = "roar.mp3";
    private CardboardAudioEngine audioEngine;
    private volatile int soundId = CardboardAudioEngine.INVALID_ID;

    private final String mModelPath = "TRex_NoGround.fbx";

    private GVRActivity mActivity;

    private static final String TAG = "MeshAnimationSample";

    private float modelX, modelY, modelZ;

    private GVRAnimationEngine mAnimationEngine;
    private GVRAnimation mAssimpAnimation = null;
    private GVRCameraRig cameraRig = null;

    public MeshAnimationMain(GVRActivity activity, CardboardAudioEngine audioEngine) {
        mActivity = activity;
        this.audioEngine = audioEngine;
    }

    @Override
    public void onInit(GVRContext gvrContext) {
        mGVRContext = gvrContext;
        mAnimationEngine = gvrContext.getAnimationEngine();

        GVRScene mainScene = gvrContext.getNextMainScene(new Runnable() {
            @Override
            public void run() {
                mAssimpAnimation.start(mAnimationEngine);
            }
        });
        cameraRig = mainScene.getMainCameraRig();

        try {
            modelX = 0.0f;
            modelY = -10.0f;
            modelZ = -10.0f;

            mCharacter = gvrContext.loadModel(mModelPath);
            mCharacter.getTransform().setPosition(modelX, modelY, modelZ);
            mCharacter.getTransform().setRotationByAxis(90.0f, 1.0f, 0.0f, 0.0f);
            mCharacter.getTransform().setRotationByAxis(40.0f, 0.0f, 1.0f, 0.0f);
            mCharacter.getTransform().setScale(1.5f, 1.5f, 1.5f);

            mainScene.addSceneObject(mCharacter);

            mAssimpAnimation = mCharacter.getAnimations().get(0);
            mAssimpAnimation.setRepeatMode(GVRRepeatMode.REPEATED).setRepeatCount(-1);
            mAssimpAnimation.setOnStart(new GVROnStart() {
                    @Override
                    public void started(GVRAnimation animation) {
                        if (soundId != CardboardAudioEngine.INVALID_ID) {
                            audioEngine.playSound(soundId, false /* looped playback */);
                        }
                    }
                });
        } catch (IOException e) {
            e.printStackTrace();
            mActivity.finish();
            mActivity = null;
            Log.e(TAG, "One or more assets could not be loaded.");
        }

        // Avoid any delays during start-up due to decoding of sound files.
        new Thread(
            new Runnable() {
              public void run() {
                // Start spatial audio playback of SOUND_FILE at the model postion. The returned
                //soundId handle is stored and allows for repositioning the sound object whenever
                // the cube position changes.
                audioEngine.preloadSoundFile(SOUND_FILE);
                soundId = audioEngine.createSoundObject(SOUND_FILE);
                audioEngine.setSoundObjectPosition(soundId, modelX, modelY, modelZ);
              }
            })
        .start();

    }

    private void updateModelPosition() {
        // Update the sound location to match it with the new cube position.
        if (soundId != CardboardAudioEngine.INVALID_ID) {
            audioEngine.setSoundObjectPosition(soundId, modelX, modelY, modelZ);
        }
    }


    @Override
    public void onStep() {
        // Update the 3d audio engine with the most recent head rotation.
        float headX = cameraRig.getHeadTransform().getRotationX();
        float headY = cameraRig.getHeadTransform().getRotationY();
        float headZ = cameraRig.getHeadTransform().getRotationZ();
        float headW = cameraRig.getHeadTransform().getRotationW();
        audioEngine.setHeadRotation(headX, headY, headZ, headW);

        // update audio position if need be
        updateModelPosition();
    }
}
