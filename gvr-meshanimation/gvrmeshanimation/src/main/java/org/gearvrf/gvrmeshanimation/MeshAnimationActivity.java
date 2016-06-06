package org.gearvrf.gvrmeshanimation;

import org.gearvrf.GVRActivity;

import android.os.Bundle;
import com.google.vrtoolkit.cardboard.audio.CardboardAudioEngine;

public class MeshAnimationActivity extends GVRActivity {
    private CardboardAudioEngine cardboardAudioEngine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        cardboardAudioEngine = new CardboardAudioEngine(getAssets(), CardboardAudioEngine.RenderingQuality.HIGH);
        setMain(new MeshAnimationMain(this, cardboardAudioEngine), "gvr.xml");
    }

    @Override
    public void onPause() {
        cardboardAudioEngine.pause();
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        cardboardAudioEngine.resume();
    }

}
