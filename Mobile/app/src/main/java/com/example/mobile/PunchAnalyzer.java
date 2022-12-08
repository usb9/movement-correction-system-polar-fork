package com.example.mobile;

/*
 * Class for acceleration data analysis:
 * Identification of punches, if the hand dropped after connecting,
 * calculation of punch velocity.
 * Currently prints out results to console, this has to be changed for use in the app.
 *
 * Author: Nicolas Schmitt
 * Date: 1.12.2022
 */

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class PunchAnalyzer {
    private int sampleRate;
    private int range;
    private int frameLengthInMs;
    public static final double MPS_TO_KMH = 3.6;   // m/s -> km/h

    // Punch result
    public boolean isPunch;
    public boolean isCorrectPunch;
    public float mySpeed;

    // Storage of 20 consecutive raw x values, meanSquareRoot in these buffers
    private List<Float> xValueBuffer = new ArrayList<>();
    private List<Double> meanSquareRootBuffer = new ArrayList<>();

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
    private final double X_PUNCH_THRESHOLD = 75.0;
    private final double X_MISTAKE_THRESHOLD = -20.0;

    private boolean isRangeSupported;

    private final static double MILLI_G_TO_METER_PER_SQUARE_SECOND = 9.81 / 1000.0;
    private int BUFFER_SIZE;
    private static final double BUFFER_SIZE_RATIO = 0.8;
    private static final String TAG = "PUNCH_ANALYZER";
    private static final int SUPPORTED_RANGE_EQUAL_OR_HIGHER = 8;
    //
    public PunchAnalyzer(int samplingRate, int gRange)  {        // buffer size calculation not safe?!

        if(gRange < SUPPORTED_RANGE_EQUAL_OR_HIGHER) {
            isRangeSupported = false;
        }
        else {
            isRangeSupported = true;
        }


        sampleRate = samplingRate;
        frameLengthInMs = 1000 / sampleRate;
        BUFFER_SIZE = (int)(samplingRate / BUFFER_SIZE_RATIO);
        punchBlockedFrames = samplingRate / 5;
        mistakeBlockedFrames = 2 * punchBlockedFrames;

        for(int i = 0; i < BUFFER_SIZE; ++i) {       // buffer size should always stay the same afterwards!
            xValueBuffer.add((float) i + 1);   // adding data!
            meanSquareRootBuffer.add((double) i + 1);
        }
    }

    public void setSampleRate(int SampleRate) {
        sampleRate = SampleRate;
        Log.d(TAG, "Sample rate set to " + sampleRate);
    }
    public void setRange(int Range) {
        range = Range;
        Log.d(TAG, "Range set to " + range);
    }

    public Pair<Double, Boolean> nextFrame(float punchDirection, float wristRotationDirection, float verticalDirection) throws Exception{

        if(!isRangeSupported) {
            Exception e = new Exception("Sensor g range too low!");
            throw e;
        }

        punchDirection *= MILLI_G_TO_METER_PER_SQUARE_SECOND;
        wristRotationDirection *= MILLI_G_TO_METER_PER_SQUARE_SECOND;
        verticalDirection *= MILLI_G_TO_METER_PER_SQUARE_SECOND;

        double meanSquareRoot = Math.sqrt((((punchDirection * punchDirection) + (wristRotationDirection * wristRotationDirection) + (verticalDirection * verticalDirection)) / 3.0));
        xValueBuffer.add(punchDirection);       // new element at the end,
        xValueBuffer.remove(0);              // oldest element removed
        meanSquareRootBuffer.add(meanSquareRoot);
        meanSquareRootBuffer.remove(0);

        return analyzeX(punchDirection);
    }

    /*
     * Analyzes current data buffer for punches and mistakes, calls calculatePunchVelocity()
     * when a punch is recognized
     * returns Pair containing Punch speed, true->correct punch / false->incorrect punch after a punch is recognized
     * returns null when no punch is recognized or more frames are needed for full analysis
     */
    private Pair<Double,Boolean> analyzeX(float punchingDirection) {        // change to public for testing
        /*
         * Punch recognition:
         * if x value is at least 75, a punch is registered and calculatePunchVelocity() is called
         * next 5 data frames are ignored to prevent registering
         * multiple punches
         */
        Pair<Double, Boolean> result = null;

        if(punchingDirection >= X_PUNCH_THRESHOLD && !ignoreFrames) {
            System.out.println("punchingDirection over 75: Punch recognized");  // Punch recognised

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
         * Identifying if arm is dropped after punch:
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

            if (punchingDirection < X_MISTAKE_THRESHOLD) {                  // correct punch recognised
                identificationCount = 0;        // reset to prevent counting multiple times
                result = new Pair<>(punchSpeed, true);
                Log.d(TAG, "Correct, speed: " + result.first);

            } else if (identificationCount == 0) {
                result = new Pair<>(punchSpeed, false);
                Log.d(TAG, "Incorrect, speed: " + result.first);
            }
        }
        return result;
        // Log.d("My draft", "--------------------------------- analyzeX");
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
        double speedMSR = 0;
        for (int i = startIndex; i < endIndex - 1; ++i) {
            speedInMps += (frameLengthInMs * Math.abs(xValueBuffer.get(i)));
            speedMSR += (frameLengthInMs * Math.abs(meanSquareRootBuffer.get(i)));
            if (i < endIndex - 2) {
                speedInMps += ((frameLengthInMs * Math.abs(xValueBuffer.get(i)
                        - xValueBuffer.get(i + 1))) / 2.0);
                speedMSR += ((frameLengthInMs * Math.abs(meanSquareRootBuffer.get(i)
                        - meanSquareRootBuffer.get(i + 1))) / 2.0);
            }
            else {
                speedInMps += (((frameLengthInMs / 2.0) * Math.abs(xValueBuffer.get(i))) / 2.0);
                speedMSR += (((frameLengthInMs / 2.0) * Math.abs(meanSquareRootBuffer.get(i))) / 2.0);
            }
            speedInMps -= frameLengthInMs * 10.0;
            speedMSR -= frameLengthInMs * 5.74;
        }

        speedInMps /= 1000; //framelength is in ms
        speedMSR /= 1000;

        Log.d("Algorithm", "Apprx. speed in meters per second: " + speedInMps);
        Log.d("Algorithm", "Apprx. speed in kilometer per hour: " + (speedInMps * MPS_TO_KMH));
        Log.d("Algorithm", "Apprx. speed(MSR) in meters per second: " + speedMSR);
        Log.d("Algorithm", "Apprx. speed(MSR) in kilometer per hour: " + (speedMSR * MPS_TO_KMH));

        //mySpeed = speedMSR;
        // ------ end speed calculation ------
        return speedMSR;
    }

    /*
     * Finds and returns the Element indicating that the punching movement begins
     */
    public int findStartIndex(List<Float> xValueBuffer) throws IllegalArgumentException{

        if(xValueBuffer.size() != BUFFER_SIZE) {
            IllegalArgumentException e = new IllegalArgumentException("findStartIndex invalid Buffer Size: " + xValueBuffer.size() + " expected: " + BUFFER_SIZE );
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
        if(xValueBuffer.size() != BUFFER_SIZE) {
            IllegalArgumentException e = new IllegalArgumentException("findEndIndex invalid Buffer Size: " + xValueBuffer.size() + " expected: " + BUFFER_SIZE );
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