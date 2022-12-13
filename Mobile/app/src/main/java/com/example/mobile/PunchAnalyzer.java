package com.example.mobile;

/*
 * Class for acceleration data analysis:
 * Identification of punches, if the hand dropped after connecting,
 * calculation of punch velocity.
 * Call nextFrame(...) to add the current measurement
 * -> returns analysis results(punch velocity, punch correct/incorrect) when analysis is done
 *
 * Author: Nicolas Schmitt
 * Last changed: 9.12.2022
 */

import android.util.Log;
import android.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class PunchAnalyzer {

    // Storage of consecutive raw/modified data values in these buffers
    private List<Double> xValueBuffer = new ArrayList<>();
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
    private final static double START_THRESHOLD = - 12.0;

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

    boolean invertSign;

    public PunchAnalyzer()  {
        invertSign = false;
    }

    public PunchAnalyzer(int SampleRate, int Range) {
        invertSign = false;
        setSampleRate(SampleRate);
        setRange(Range);
        Log.d(TAG, "buffer sizes: " + xValueBuffer.size() + ", " + RootMeanSquareBuffer.size());
    }

    public void setSampleRate(int SampleRate) throws IllegalArgumentException{         // also sets sample rate dependent constants, initializes buffers!

        if(!xValueBuffer.isEmpty() || !RootMeanSquareBuffer.isEmpty()) {
            xValueBuffer.clear();
            RootMeanSquareBuffer.clear();
        }
        Log.d(TAG, "SR: " +  SampleRate);
        if(SampleRate == 26) {
            bufferSize = BUFFER_SIZE_26_HZ;
            frameLengthInMs = FRAME_LENGTH_IN_MS_26_HZ;
            punchBlockedFrames = P_BLOCKED_26_HZ;
            mistakeBlockedFrames = M_BLOCKED_26_HZ;
            sampleRate = SampleRate;
        }
        if(SampleRate == 52) {
            bufferSize = BUFFER_SIZE_52_HZ;
            frameLengthInMs = FRAME_LENGTH_IN_MS_52_HZ;
            punchBlockedFrames = P_BLOCKED_52_HZ;
            mistakeBlockedFrames = M_BLOCKED_52_HZ;
            sampleRate = SampleRate;
        }

        for(int i = 0; i < bufferSize; ++i) {       // buffer size should always stay the same afterwards!
            xValueBuffer.add((double) i + 1);   // adding data!
            RootMeanSquareBuffer.add((double) i + 1);
        }
        Log.d(TAG, "buffer sizes: " + xValueBuffer.size() + ", " + RootMeanSquareBuffer.size());
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


    public Pair<Double, Boolean> nextFrame(int PunchDirection, int WristRotationDirection, int VerticalDirection){

        double punchDirection = (double) PunchDirection;
        double wristRotationDirection = (double) WristRotationDirection;
        double verticalDirection = (double) VerticalDirection;

        punchDirection *= MILLI_G_TO_METER_PER_SQUARE_SECOND;
        wristRotationDirection *= MILLI_G_TO_METER_PER_SQUARE_SECOND;
        verticalDirection *= MILLI_G_TO_METER_PER_SQUARE_SECOND;

        double meanSquareRoot = Math.sqrt((((punchDirection * punchDirection) + (wristRotationDirection * wristRotationDirection) + (verticalDirection * verticalDirection)) / 3.0));
        //double XYRootMeanSquare = Math.sqrt((((punchDirection * punchDirection) + (wristRotationDirection * wristRotationDirection))  / 2.0));

        //Log.d(TAG, "Next: adding new values...");
        xValueBuffer.add(punchDirection);       // new element at the end,
        xValueBuffer.remove(0);              // oldest element removed
        RootMeanSquareBuffer.add(meanSquareRoot);
        RootMeanSquareBuffer.remove(0);

        //XYBuffer.add(XYRootMeanSquare);
        //XYBuffer.remove(0);

        //Log.d(TAG, "Next: values added");

        return analyzeX(punchDirection);
    }

    /*
     * Analyzes current data buffer for punches and mistakes, calls calculatePunchVelocity()
     * when a punch is recognized
     * returns Pair containing Punch speed, true->correct punch / false->incorrect punch after a punch is recognized
     * returns null when no punch is recognized or more frames are needed for full analysis
     */
    private Pair<Double,Boolean> analyzeX(double punchingDirection) {        // change to public for testing


        //Pair<Double, Boolean> result = null;
        Pair<Double, Boolean> result = new Pair<Double, Boolean>(-1.0, false);

        /*
         * Punch recognition:
         * if x value is at least 75, a punch is registered and calculatePunchVelocity() is called
         * next 5 data frames are ignored to prevent registering
         * multiple punches
         */
        if(Math.abs(punchingDirection) >= X_PUNCH_THRESHOLD && !ignoreFrames) {
            Log.d(TAG, "punchingDirection over 75: Possible punch recognized");  // Punch recognised

            /*
            * temporary solution?! when measured values exceed the sensor range,
            * the received value always seems to be the maximum positive value
            * even when it should be negative!
            * However, the measurement before SHOULD be negative in that case... <----- THIS IS ONLY BASED ON ANALYSIS OF OUR OWN SAMPLE DATA!!!
            */
            if(xValueBuffer.get(xValueBuffer.size() - 2) < 0) {
                invertSign = true;
                Log.d(TAG, "Values inverted!");
            }
            else {
                invertSign = false;
                Log.d(TAG, "Values NOT inverted!");
            }

            ignoreFrames = true;
            ignoreCount = punchBlockedFrames;

            Log.d(TAG, "Analyze: starting speed calculation...");
            punchSpeed = calculatePunchVelocity();

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

            if (punchingDirection < X_MISTAKE_THRESHOLD || (invertSign && invertDoubleSign(punchingDirection) < X_MISTAKE_THRESHOLD)) {          // correct punch recognised
                identificationCount = 0;                            // reset to prevent counting multiple times
                result = new Pair<>(punchSpeed, true);
                Log.d(TAG, "Correct, speed: " + result.first);

            }
            else if (identificationCount == 0) {
                result = new Pair<>(punchSpeed, false);
                Log.d(TAG, "Incorrect, speed: " + result.first);
            }
        }
        return result;
    }

    /*
     * Calculates and returns Punch velocity
     */
    private Double calculatePunchVelocity(){

        int startIndex = findStartIndex();
        int endIndex = findEndIndex();


        Log.d(TAG, "Start Index: " + startIndex + " " + xValueBuffer.get(startIndex));
        Log.d(TAG, "End Index: " + endIndex + " " + xValueBuffer.get(endIndex));

        for(int i = 0; i < xValueBuffer.size(); ++i)
            Log.d(TAG, "xBuf: " + xValueBuffer.get(i));

        //for(int i = 0; i < RootMeanSquareBuffer.size(); ++i)
        //    Log.d(TAG, "RMSBuf: " + RootMeanSquareBuffer.get(i));


        if(startIndex == -1 || endIndex == -1 || startIndex >= endIndex) {
            Log.d(TAG, "Speed calculation: Index not found!");
            return null;
        }

        // ------ speed calculation     ------

        double speedRMS = 0;

        for (int i = startIndex; i <= endIndex; ++i) {
            speedRMS += (frameLengthInMs * RootMeanSquareBuffer.get(i));
            if (i < endIndex - 2) {
                speedRMS += ((frameLengthInMs * RootMeanSquareBuffer.get(i)
                        - RootMeanSquareBuffer.get(i + 1)) / 2.0);
            }
            else {
                speedRMS += (((frameLengthInMs / 2.0) * RootMeanSquareBuffer.get(i)) / 2.0);
            }
        }

        speedRMS /= 1000;
        // ------ end speed calculation ------

        Log.d(TAG, "Apprx. speed(RMS) in meters per second: " + speedRMS);
        Log.d(TAG, "Apprx. speed(RMS) in kilometer per hour: " + (speedRMS * MPS_TO_KMH));


        return speedRMS * MPS_TO_KMH;
    }

    /*
     * Finds and returns the Element indicating that the punching movement begins
     */
    private int findStartIndex(){

        Log.d(TAG, "startIndex: starting...");
        if(xValueBuffer.size() != bufferSize ||(bufferSize != BUFFER_SIZE_26_HZ && bufferSize != BUFFER_SIZE_52_HZ )) {
            Log.d(TAG, "findStartIndex invalid Buffer Size: " + xValueBuffer.size() + " expected: " + bufferSize);
            return -1;
        }

        int frameBufferIndex = 0;

        /*
        * see documentation for detailed explanation of frame skipping
        */
        if(bufferSize == BUFFER_SIZE_52_HZ) {           // skip 7 frames at 52 Hz
            frameBufferIndex = xValueBuffer.size() - 8;
        }
        if(bufferSize == BUFFER_SIZE_26_HZ) {           // skip 3 frames at 26 Hz
            frameBufferIndex = xValueBuffer.size() - 4;
        }

        int startIndex = 0;
        boolean continueStartSearch = true;

        Log.d(TAG, "startIndex: first loop at " + frameBufferIndex + " size " + xValueBuffer.size() + " starting...");
        while (continueStartSearch) {                       // search for element > -15.0 starts here,

            if (!invertSign && xValueBuffer.get(frameBufferIndex) > START_THRESHOLD) {
                startIndex = frameBufferIndex;
                continueStartSearch = false;
            }
            if (invertSign && invertDoubleSign(xValueBuffer.get(frameBufferIndex)) > START_THRESHOLD) {
                startIndex = frameBufferIndex;
                continueStartSearch = false;
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
     * Finds and returns the Element indicating that the arm is no longer accelerating in punching direction
     */
    private int findEndIndex(){
        if(xValueBuffer.size() != bufferSize) {
           Log.d(TAG, "findEndIndex invalid Buffer Size: " + xValueBuffer.size() + " expected: " + bufferSize);
        }
        int frameBufferIndex;
        int endIndex = 0;

        if(!invertSign) {
            frameBufferIndex = xValueBuffer.size() - 1;      // start from last element
        }
        else {
            frameBufferIndex = xValueBuffer.size() - 2;     // if values are inverted, skip the last element as it has the wrong sign(see comment at beginning of analyzeX(...))
        }

        boolean continueEndSearch = true;

        while (continueEndSearch) {                       // search for element < 0.0 starts here,

            if (!invertSign && xValueBuffer.get(frameBufferIndex) < 0) {
                endIndex = frameBufferIndex;
                continueEndSearch = false;
            }
            if (invertSign && xValueBuffer.get(frameBufferIndex) > 0) {
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

    private double invertDoubleSign(double d) {
        return d * -1.0;
    }

}