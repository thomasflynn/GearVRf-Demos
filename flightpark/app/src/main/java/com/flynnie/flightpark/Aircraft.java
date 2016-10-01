package com.flynnie.flightpark;

class Aircraft {

    private float[] pos = new float[3];
    private float[] euler_angles = new float[3];
    private int throttle = 0;
    private static final int MAX_THROTTLE = 200;
    private float aileron = 0;
    private float elev = 0;
    private float fuel_capacity = 18000;
    private float fuel_current  = 18000;
    private static final String TAG = "Aircraft";
    

    void incThrust() {
        throttle += 1;
        if(throttle > MAX_THROTTLE)
        {
            throttle = MAX_THROTTLE;
        }
    }

    void decThrust() {
        throttle -= 1;
        if(throttle < 0)
        {
            throttle = 0;
        }
    }

    void rollLeft(float value) {
        aileron = (value + 00000) * (60.0f / 1.0f);
        android.util.Log.d(TAG, "rollLeft("+value+"), aileron="+aileron);
	}

    void rollRight(float value) {
        aileron = (value - 00000) * (60.0f / 1.0f);
        android.util.Log.d(TAG, "rollRight("+value+"), aileron="+aileron);
	}

    void pitchUp(float value) {
        elev = (value - 00000) * (60.0f / 1.0f);
	}

    void pitchDown(float value) {
        elev = (value - 00000) * (60.0f / 1.0f);
	}

    void zeroAilerons() {
        aileron = 0;
	}

    void zeroElevators() {
        elev = 0;
	}

    float[] getPos() {
        return pos;
    }

    float[] getHpr() {
        return euler_angles;
    }

    float getSpeed() {
        return throttle;
    }

    float getPower() {
        return (throttle / MAX_THROTTLE) * 100;
    }

    public void step(float dt) {
        float pitch;
        float alt;
        
        if(dt == 0)
        {
            return;
        }

        fuel_current -= dt;

        alt = pos[2]; // z
        float speed = throttle;

        pitch = euler_angles[1];

        //fprintf(stderr, "elev = %f, pitch = %f\n", elev, pitch);
        // define pitch rate
        float pitch_rate = 5 * dt; // 30 degrees per second

        // find direction of pitch
        if(elev > -0.5 && elev < 0.5)
        {
            if(pitch > -0.1 && pitch < 0.1)
            {
                pitch = 0; 
            }
            else if(pitch > 0.1)
            {
                pitch_rate = pitch * dt * 2;
                // 30 degrees per second
                //fprintf(stderr, "> 0.1, pitch_rate = %f\n", pitch_rate);
                pitch -= pitch_rate;
            }
            else if(pitch < -0.1)
            {
                pitch_rate = pitch * dt * 2;
                //fprintf(stderr, "< -0.1, pitch_rate = %f\n", pitch_rate);
                pitch -= pitch_rate;
            }
        }
        else if(elev - pitch < -0.1)
        {
            // add pitch rate to pitch angle 
            //fprintf(stderr, "< -0.1\n");
            pitch_rate = (30 + pitch) * dt; 
            if((elev < -5 && pitch > 5) ||
                elev > 5 && pitch < -5)
            {
                //fprintf(stderr, "< -5 && > 5\n");
                pitch_rate *= 2;
            }
            //fprintf(stderr, "pitch_rate = %f\n", pitch_rate);
            pitch -= pitch_rate;
        }
        else if(elev - pitch > 0.1)
        {
            // add pitch rate to pitch angle 
            //fprintf(stderr, "> -0.1\n");
            pitch_rate = (30 - pitch) * dt; 
            if((elev < -5 && pitch > 5) ||
                elev > 5 && pitch < -5)
            {
                //fprintf(stderr, "< -5 && > 5\n");
                pitch_rate *= 2;
            }
            //fprintf(stderr, "pitch_rate = %f\n", pitch_rate);
            pitch += pitch_rate; 
        }

        if(pitch > 60)
        {
            pitch = 60;
        }
        else if(pitch < -60)
        {
            pitch = -60;
        }

        euler_angles[1] = pitch;
        pitch = pitch * ((float)Math.PI / 180.0f);

        // should convert speed from kts to feet per second
        float speedps = speed * (6076.0f / 3600.0f);

        // calculation done in seconds
        /*
                     d  --
                  e  --  |
               e  --     |      ft/s (max)
            p  --      __| z
          s --  p)    |  |
          ----------------
                 s

            s = h * cos(p);
            z = h * sin(p);
            h = z / sin(p);
        */
        // find movement along z
        float fts = speedps * (float)Math.sin(pitch);
        alt += fts * dt;

        // find movement along s
        float s = speedps * (float)Math.cos(pitch);
        s = s / 30;

        // convert back to feet per minute
        float fpm = fts * 60; 

        //fprintf(stderr, "speed: %f, speedps: %f, ft/m: %f\n", speed, speedps, fpm);
        //fprintf(stderr, "pitch: %f, altitude: %f, dt: %f\n", elev, alt, dt);
        //fprintf(stderr, "fts: %f\n", fts);

        pos[2] = alt;

        float bank = euler_angles[2];

        // define roll rate
        float roll_rate = 90 * dt; // 90 degrees per second

        //android.util.Log.d(TAG, "aileron="+aileron+", bank="+bank);

        // find direction of roll
        if(aileron > -0.1 && aileron < 0.1)
        {
            if(bank > -0.1 && bank < 0.1)
            {
                bank = 0; 
            }
            else if(bank > 0.1)
            {
                roll_rate = bank * dt * 2;
                // 90 degrees per second
                //android.util.Log.d(TAG, "> 0.1, roll_rate="+roll_rate);
                bank -= roll_rate;
            }
            else if(bank < -0.1)
            {
                roll_rate = bank * dt * 2;
                //android.util.Log.d(TAG, "< -0.1, roll_rate="+roll_rate);
                bank -= roll_rate;
            }
        }
        else if(aileron - bank < -0.1)
        {
            // add roll rate to bank angle 
            android.util.Log.d(TAG, "< -0.1");
            roll_rate = (90 + bank) * dt; 
            if((aileron < -5 && bank > 5) ||
                aileron > 5 && bank < -5)
            {
                //android.util.Log.d(TAG, "< -5 && > 5");
                roll_rate *= 2;
            }
            //android.util.Log.d(TAG, "roll_rate="+roll_rate);
            bank -= roll_rate;
            if(bank < aileron)
            {
                bank = aileron;
            }
        }
        else if(aileron - bank > 0.1)
        {
            // add roll rate to bank angle 
            //android.util.Log.d(TAG, "> 0.1");
            roll_rate = (90 - bank) * dt; 
            if((aileron < -5 && bank > 5) ||
                aileron > 5 && bank < -5)
            {
                //android.util.Log.d(TAG, "< -5 && > 5");
                roll_rate *= 2;
            }
            //android.util.Log.d(TAG, "roll_rate="+roll_rate);
            bank += roll_rate; 
            if(bank > aileron)
            {
                bank = aileron;
            }
        }

        if(bank > 60)
        {
            bank = 60;
        }
        else if(bank < -60)
        {
            bank = -60;
        }

        euler_angles[2] = bank;
        float roll = bank;

        float bankdir = 0;
        if(bank > 0)
        {
            bankdir = 1;
        }
        else if(bank < 0)
        {
            bankdir = -1;
            bank = bank * -1; // make it positive for the calculations below
        }
        //fprintf(stderr, "aileron: %f, bank: %f, bankdir: %f\n", aileron, bank, bankdir);

        // given bank, find r (rate of turn)
        /*
            turn  | seconds | turn rate | bank angle
            ------+---------+-----------+-----------
                  |         |           |
             360  |  10     |   36.0    |    10
             360  |   9     |   40.0    |    15
             360  |   8     |   45.0    |    20
             360  |   6     |   60.0    |    45
             360  |   5     |   72.0    |    60
             360  |   4     |   90.0    |    90
        */

        float[][] table = new float[6][2];
        table[0][0] = 18; table[0][1] = 10;
        table[1][0] = 20; table[1][1] = 15;
        table[2][0] = 22.5f; table[2][1] = 20;
        table[3][0] = 24; table[3][1] = 45;
        table[4][0] = 36; table[4][1] = 60;
        table[5][0] = 90; table[5][1] = 90;

        float r = 0;

        // lerp the rate
        for(int i=0; i<5; i++)
        {

            float y0 = table[i][1];
            float y1 = table[i+1][1];

            if(bank <= y1 && bank > y0)
            {
                float x0 = table[i][0];
                float x1 = table[i+1][0];
                float ratio = (x1 - x0) / (y1 - y0);

                r = x0 + (bank - y0) * ratio;
            }
        }

        // make sure we're going in the correct direction
        r = r * bankdir;
        //fprintf(stderr, "r = %f, bank = %f, bankdir = %f\n", r, bank, bankdir);

        // account for time
        r = r * dt;
        //fprintf(stderr, "then time r = %f\n", r);

        // add r to heading
        float heading = euler_angles[0];
        heading += r;

        // account for wrapping
        if(heading == 0)
        {
            heading = 360;
        }
        if(heading > 360)
        {
            heading = heading - 360;
        }
        if(heading < 0)
        {
            heading = heading + 360;
        }
        euler_angles[0] = heading;

        // convert to radians for calculation of x & y
        float h = heading * ((float)Math.PI / 180.0f);

        //fprintf(stderr, "then radians r = %f\n", r);
        //fprintf(stderr, "heading: %f, bank: %f, dt: %f\n", euler_angles[0], euler_angles[2], dt);

        // find x
        /*
                        --
                     --  |
                s --     |     
               --      __| x
            --  h)    |  |
          ----------------
                 y

            y = s * sin(h);
        */
        float x = s * (float)Math.sin(h);

        // add x to position
        pos[0] += x;

        // find y
        /*
                        --
                     --  |
                s --     |     
               --      __| x
            --  h)    |  |
          ----------------
                 y

            y = s * cos(h);
        */
        float y = s * (float)Math.cos(h);

        // add y to position
        pos[1] += y;

        //android.util.Log.d(TAG, "heading: "+heading+", pitch: "+pitch+", roll: "+roll+", altitude: "+alt);
        android.util.Log.d(TAG, "heading: "+euler_angles[0]+", pitch: "+euler_angles[1]+", roll: "+euler_angles[2]);
        //fprintf(stderr, "x: %f, y: %f, z: %f\n", pos[0], pos[1], pos[2]);

    }
}
