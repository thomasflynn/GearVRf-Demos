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

// https://github.com/manask88/wiki/wiki/Speech-Recognition-Tutorial---LightBulb-Example

package org.gearvrf.qrcode;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import java.io.File;
import android.os.Environment;
import android.os.Bundle;
import java.io.IOException;
import java.util.ArrayList;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

public class SpeechRecognizerManager {


    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    /* Keyword we are looking for to activate menu */
    private static final String KEYPHRASE = "alright";
    private edu.cmu.pocketsphinx.SpeechRecognizer mPocketSphinxRecognizer;
    private static final String TAG = "QRCode";
    private Context mContext;
    protected android.speech.SpeechRecognizer mGoogleSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    private OnResultListener mOnResultListener;

    public SpeechRecognizerManager(Context context){
        this.mContext=context;

        initPocketSphinx();
        initGoogleSpeechRecognizer();

    }

    private void initPocketSphinx(){

        new AsyncTask<Void, Void, Exception>() {
            @Override
            protected Exception doInBackground(Void... params) {
                try {
                    Assets assets = new Assets(mContext);

                    //Performs the synchronization of assets in the application and external storage
                    File assetDir = assets.syncAssets();

                    //Creates a new SpeechRecognizer builder with a default configuration
                    SpeechRecognizerSetup speechRecognizerSetup = defaultSetup();

                    //Set Dictionary and Acoustic Model files
                    speechRecognizerSetup.setAcousticModel(new File(assetDir, "en-us-ptm"));
                    speechRecognizerSetup.setDictionary(new File(assetDir, "cmudict-en-us.dict"));

                    // Threshold to tune for keyphrase to balance between false positives and false negatives
                    speechRecognizerSetup.setKeywordThreshold(1e-45f);

                    //Creates a new SpeechRecognizer object based on previous set up.
                    mPocketSphinxRecognizer = speechRecognizerSetup.getRecognizer();

                    mPocketSphinxRecognizer.addListener(new PocketSphinxRecognitionListener());

                    // Create keyword-activation search.
                    mPocketSphinxRecognizer.addKeyphraseSearch(KWS_SEARCH, KEYPHRASE);
        
                    stopListening();
                } catch (IOException e) {
                    return e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    android.util.Log.d("QRCode", "Failed to init pocketSphinxRecognizer ");
                    //Toast.makeText(mContext, "Failed to init pocketSphinxRecognizer ", Toast.LENGTH_SHORT).show();
                } else {
                    restartSearch(KWS_SEARCH);
                }
            }
        }.execute();

    }

    private void initGoogleSpeechRecognizer() {

        mGoogleSpeechRecognizer = android.speech.SpeechRecognizer
                .createSpeechRecognizer(mContext);

        mGoogleSpeechRecognizer.setRecognitionListener(new GoogleRecognitionListener());

        mSpeechRecognizerIntent = new Intent( RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

        mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        mSpeechRecognizerIntent.putExtra( RecognizerIntent. EXTRA_CONFIDENCE_SCORES, true);
    }

    public void stopListening() {
        mPocketSphinxRecognizer.stop();
    }

    public void startListening() {
        mPocketSphinxRecognizer.startListening(KWS_SEARCH);

    }

    public void destroy() {
        if (mPocketSphinxRecognizer != null) {
            mPocketSphinxRecognizer.cancel();
            mPocketSphinxRecognizer.shutdown();
            mPocketSphinxRecognizer = null;
        }


        if (mGoogleSpeechRecognizer != null) {
            mGoogleSpeechRecognizer.cancel();
            ;
            mGoogleSpeechRecognizer.destroy();
            mPocketSphinxRecognizer = null;
        }

    }

    private void restartSearch(String searchName) {

        mPocketSphinxRecognizer.stop();

        mPocketSphinxRecognizer.startListening(searchName);

    }


    protected class PocketSphinxRecognitionListener implements edu.cmu.pocketsphinx.RecognitionListener {

        @Override
        public void onBeginningOfSpeech() {
            android.util.Log.d("QRCode", "beginning of speech");
        }


        /**
         * In partial result we get quick updates about current hypothesis. In
         * keyword spotting mode we can react here, in other modes we need to wait
         * for final result in onResult.
         */
        @Override
        public void onPartialResult(Hypothesis hypothesis) {
            if (hypothesis == null) {
                android.util.Log.d(TAG,"null");
                return;
            }


            String text = hypothesis.getHypstr();
            if (text.contains(KEYPHRASE)) {
                android.util.Log.d("QRCode", "text.contains("+KEYPHRASE+"): "+text);
                mGoogleSpeechRecognizer.startListening(mSpeechRecognizerIntent);
                mPocketSphinxRecognizer.cancel();
            }
        }

        @Override
        public void onResult(Hypothesis hypothesis) {
        }

        /**
         * We stop mPocketSphinxRecognizer here to get a final result
         */
        @Override
        public void onEndOfSpeech() {
            android.util.Log.d("QRCode", "end of speech");
        }

        public void onError(Exception error) {
            android.util.Log.d("QRCode", "error: " + error);
        }

        @Override
        public void onTimeout() {
            android.util.Log.d("QRCode", "timeout");
        }

    }

protected class GoogleRecognitionListener implements
            android.speech.RecognitionListener {

        private final String TAG = GoogleRecognitionListener.class.getSimpleName();

        @Override
        public void onBeginningOfSpeech() {
        }

        @Override
        public void onEndOfSpeech() {
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }

        @Override
        public void onBufferReceived(byte[] buffer) {

        }

        @Override
        public void onError(int error) {
            android.util.Log.e("QRCode", "onError:" + error);

            mPocketSphinxRecognizer.startListening(KWS_SEARCH);


        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            android.util.Log.d(TAG, "onPartialResultsheard:");

        }

        @Override
        public void onResults(Bundle results) {
            if ((results != null)
                    && results.containsKey(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)) {
                ArrayList<String> heard = 
                          results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
                float[] scores = results.getFloatArray(android.speech.SpeechRecognizer.CONFIDENCE_SCORES);

                for (int i = 0; i < heard.size(); i++) {
                    android.util.Log.d(TAG, "onResultsheard:" + heard.get(i) + " confidence:" + scores[i]);

                }


                //send list of words to activity
                if (mOnResultListener!=null){
                    mOnResultListener.OnResult(heard);
                }

            }

            mPocketSphinxRecognizer.startListening(KWS_SEARCH);


        }


        @Override
        public void onEvent(int eventType, Bundle params) {

        }

    }

    public void setOnResultListner(OnResultListener onResultListener){
        mOnResultListener=onResultListener;
    }

    public interface OnResultListener
    {
        public void OnResult(ArrayList<String> commands);
    }
}
