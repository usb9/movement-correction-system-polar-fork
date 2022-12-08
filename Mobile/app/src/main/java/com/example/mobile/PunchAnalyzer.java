package com.example.mobile;

/*
 * Class for acceleration data analysis:
 * Identification of punches, if the hand dropped after connecting,
 * calculation of punch velocity.
 * Call nextFrame(...) to add the current measurement
 * -> returns analysis results(punch velocity, punch correct/incorrect) when analysis is done
 *
 * Author: Nicolas Schmitt
 * Last changed: 8.12.2022
 */

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class PunchAnalyzer {

    // Storage of consecutive raw/modified data values in these buffers
    private List<Float> xValueBuffer = new ArrayList<>();
    private List<Double> RootMeanSquareBuffer = new ArrayList<>();

    // for punch identification, a specified number of frames has to be ignored
    // (exact number depends on the sampling rate, see constructor)
    private int punchBlockedFrames;
    private int mistakeBlockedFrames;

    private Double punchSpeed = null;

    // used for counting ignored frames / frames for mistake identification
    private int ignoreCount = 0;
    private boolean ignoreFrames = false;
    private int identificationCount = 0;

    // x value thresholds for registering punches and arm drops
    private final static double X_PUNCH_THRESHOLD = 75.0;
    private final static double X_MISTAKE_THRESHOLD = -20.0;

    private int range;
    private static final int LOW_RANGE = 8;
    private static final int HIGH_RANGE = 16;

    private final static double MILLI_G_TO_METER_PER_SQUARE_SECOND = 9.81 / 1000.0;
    public static final double MPS_TO_KMH = 3.6;   // m/s -> km/h

    private int sampleRate;

    private int bufferSize;
    private static final int BUFFER_SIZE_26_HZ = 20;
    private static final int BUFFER_SIZE_52_HZ = 40;

    private double frameLengthInMs;
    private static final double FRAME_LENGTH_IN_MS_26_HZ = 38.46;
    private static final double FRAME_LENGTH_IN_MS_52_HZ = 19.23;

    private static final int P_BLOCKED_26_HZ = 5;
    private static final int P_BLOCKED_52_HZ = 10;

    private static final int M_BLOCKED_26_HZ = 5;
    private static final int M_BLOCKED_52_HZ = 10;

    private static final String TAG = "PUNCH_ANALYZER";

    public PunchAnalyzer()  {

    }

    public PunchAnalyzer(int SampleRate, int Range) {
        setSampleRate(SampleRate);
        setRange(Range);
    }

    public void setSampleRate(int SampleRate) throws IllegalArgumentException{         // also sets sample rate dependent constants, initializes buffers!

        if(sampleRate == 26) {
            bufferSize = BUFFER_SIZE_26_HZ;
            frameLengthInMs = FRAME_LENGTH_IN_MS_26_HZ;
            punchBlockedFrames = P_BLOCKED_26_HZ;
            mistakeBlockedFrames = M_BLOCKED_26_HZ;
            sampleRate = SampleRate;
        }
        else if(sampleRate == 52) {
            bufferSize = BUFFER_SIZE_52_HZ;
            frameLengthInMs = FRAME_LENGTH_IN_MS_52_HZ;
            punchBlockedFrames = P_BLOCKED_52_HZ;
            mistakeBlockedFrames = M_BLOCKED_52_HZ;
            sampleRate = SampleRate;
        }
        else {
            IllegalArgumentException e = new IllegalArgumentException(TAG + "Bad sample rate: " + SampleRate);
            throw e;
        }

        for(int i = 0; i < bufferSize; ++i) {       // buffer size should always stay the same afterwards!
            xValueBuffer.add((float) i + 1);   // adding data!
            RootMeanSquareBuffer.add((double) i + 1);
        }
        Log.d(TAG, "Sample rate set to " + sampleRate);
    }


    public void setRange(int Range) throws IllegalArgumentException{
        if(Range == LOW_RANGE) {
            range = Range;
            Log.d(TAG, "using Range +-" + LOW_RANGE);
        }

        else if (Range == HIGH_RANGE){
            range = Range;
            Log.d(TAG, "using Range +-" +HIGH_RANGE);
        }
        else {
            IllegalArgumentException e = new IllegalArgumentException(TAG + "Bad sample rate: " + Range);
            throw e;
        }

        Log.d(TAG, "Range set to +-" + range);
    }


    public Pair<Double, Boolean> nextFrame(float punchDirection, float wristRotationDirection, float verticalDirection){

        punchDirection *= MILLI_G_TO_METER_PER_SQUARE_SECOND;
        wristRotationDirection *= MILLI_G_TO_METER_PER_SQUARE_SECOND;
        verticalDirection *= MILLI_G_TO_METER_PER_SQUARE_SECOND;

        double meanSquareRoot = Math.sqrt((((punchDirection * punchDirection) + (wristRotationDirection * wristRotationDirection) + (verticalDirection * verticalDirection)) / 3.0));

        xValueBuffer.add(punchDirection);       // new element at the end,
        xValueBuffer.remove(0);              // oldest element removed
        RootMeanSquareBuffer.add(meanSquareRoot);
        RootMeanSquareBuffer.remove(0);

        return analyzeX(punchDirection);
    }

    /*
     * Analyzes current data buffer for punches and mistakes, calls calculatePunchVelocity()
     * when a punch is recognized
     * returns Pair containing Punch speed, true->correct punch / false->incorrect punch after a punch is recognized
     * returns null when no punch is recognized or more frames are needed for full analysis
     */
    private Pair<Double,Boolean> analyzeX(float punchingDirection) {        // change to public for testing

        Pair<Double, Boolean> result = null;
        boolean invertSign = false;

        /*
         * Punch recognition:
         * if x value is at least 75, a punch is registered and calculatePunchVelocity() is called
         * next 5 data frames are ignored to prevent registering
         * multiple punches
         */
        if(Math.abs(punchingDirection) >= X_PUNCH_THRESHOLD && !ignoreFrames) {
            System.out.println("punchingDirection over 75: Punch recognized");  // Punch recognised

            if(punchingDirection < 0)
                invertSign = true;

            ignoreFrames = true;
            ignoreCount = punchBlockedFrames;

            try {
                punchSpeed = calculatePunchVelocity(xValueBuffer);
            }
            catch (IllegalArgumentException e){
                Log.d(TAG, e.toString());
            }
        }

        /*
         * Identify if arm is dropped after punch:
         * after the skipped frames for punch recognition,
         * check if x drops below -20 at least once in the next 10
         * frames
         */
        if (ignoreFrames) {
            --ignoreCount;
            if (ignoreCount <= 0) {
                ignoreFrames = false;
                identificationCount = mistakeBlockedFrames;       // starts correct/incorrect identification
            }
        }

        if (identificationCount > 0) {
            --identificationCount;

            if (punchingDirection < X_MISTAKE_THRESHOLD || (invertSign && - punchingDirection < X_MISTAKE_THRESHOLD)) {          // correct punch recognised
                identificationCount = 0;                            // reset to prevent counting multiple times
                result = new Pair<>(punchSpeed, true);
                Log.d(TAG, "Correct, speed: " + result.first);

            } else if (identificationCount == 0) {
                result = new Pair<>(punchSpeed, false);
                Log.d(TAG, "Incorrect, speed: " + result.first);
            }
        }
        return result;
    }

    /*
     * Calculates and returns Punch velocity
     */

    private Double calculatePunchVelocity(List<Float> xValueBuffer) throws IllegalArgumentException {

        int startIndex = findStartIndex(xValueBuffer);
        int endIndex = findEndIndex(xValueBuffer);

        Log.d("Algorithm", "Start Index: " + startIndex);
        Log.d("Algorithm", "End Index: " + endIndex);

        if(startIndex == -1 || endIndex == -1) {

            System.out.println("Speed calculation: Index not found!");
            return null;
        }

        // ------ speed calculation     ------
        double speedInMps = 0;
        double speedRMS = 0;
        for (int i = startIndex; i < endIndex - 1; ++i) {
            speedInMps += (frameLengthInMs * Math.abs(xValueBuffer.get(i)));
            speedRMS += (frameLengthInMs * Math.abs(RootMeanSquareBuffer.get(i)));
            if (i < endIndex - 2) {
                speedInMps += ((frameLengthInMs * Math.abs(xValueBuffer.get(i)
                        - xValueBuffer.get(i + 1))) / 2.0);
                speedRMS += ((frameLengthInMs * Math.abs(RootMeanSquareBuffer.get(i)
                        - RootMeanSquareBuffer.get(i + 1))) / 2.0);
            }
            else {
                speedInMps += (((frameLengthInMs / 2.0) * Math.abs(xValueBuffer.get(i))) / 2.0);
                speedRMS += (((frameLengthInMs / 2.0) * Math.abs(RootMeanSquareBuffer.get(i))) / 2.0);
            }
            speedInMps -= frameLengthInMs * 10.0;
            speedRMS -= frameLengthInMs * 5.74;
        }

        speedInMps /= 1000; //framelength is in ms
        speedRMS /= 1000;
        // ------ end speed calculation ------

        Log.d(TAG, "Apprx. speed in meters per second: " + speedInMps);
        Log.d(TAG, "Apprx. speed in kilometer per hour: " + (speedInMps * MPS_TO_KMH));
        Log.d(TAG, "Apprx. speed(RMS) in meters per second: " + speedRMS);
        Log.d(TAG, "Apprx. speed(RMS) in kilometer per hour: " + (speedRMS * MPS_TO_KMH));

        return speedRMS;
    }

    /*
     * Finds and returns the Element indicating that the punching movement begins
     */
    public int findStartIndex(List<Float> xValueBuffer) throws IllegalArgumentException{

        if(xValueBuffer.size() != bufferSize) {
            IllegalArgumentException e = new IllegalArgumentException("findStartIndex invalid Buffer Size: " + xValueBuffer.size() + " expected: " + bufferSize);
            throw e;
        }
        int startIndex = 0;

        int frameBufferIndex = xValueBuffer.size() - 5;      // last elements are usually > 0!
        boolean continueStartSearch = true;


        while (continueStartSearch) {                       // search for element > -10.0 starts here,

            if (xValueBuffer.get(frameBufferIndex) > -10.0) {
                startIndex = frameBufferIndex;
                continueStartSearch = false;

                //for (int i = 0; i < xValueBuffer.size(); ++i) {    // only for debugging
                //    System.out.println(i + ": " + xValueBuffer.get(i));
                //}

            }

            if (frameBufferIndex == 0) {
                continueStartSearch = false;

                Log.d(TAG, "Start not found!");

                startIndex = -1;
            }
            --frameBufferIndex;
        }
        return startIndex;
    }

    /*
     * Finds and returns the Element indicating that the punch connects
     */
    public int findEndIndex(List<Float> xValueBuffer) throws IllegalArgumentException{
        if(xValueBuffer.size() != bufferSize) {
            IllegalArgumentException e = new IllegalArgumentException("findEndIndex invalid Buffer Size: " + xValueBuffer.size() + " expected: " + bufferSize);
            throw e;
        }

        int endIndex = 0;
        int frameBufferIndex = xValueBuffer.size() - 1;      // start from last element
        boolean continueEndSearch = true;

        while (continueEndSearch) {                       // search for element > -10.0 starts here,

            if (xValueBuffer.get(frameBufferIndex) < 0) {
                endIndex = frameBufferIndex;
                continueEndSearch = false;
            }

            if (frameBufferIndex == 0) {
                continueEndSearch = false;

                Log.d(TAG, "End not found!");

                endIndex = -1;
            }
            --frameBufferIndex;
        }

        return endIndex;
    }

}