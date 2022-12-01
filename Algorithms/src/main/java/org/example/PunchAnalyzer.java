package org.example;
/*
 * Class for acceleration data analysis:
 * Identification of punches, if the hand dropped after connecting,
 * calculation of punch velocity.
 * Currently prints out results to console, this has to be changed for use in the app.
 *
 * Author: Nicolas Schmitt
 * Date: 1.12.2022
 */

import java.util.ArrayList;
import java.util.List;

public class PunchAnalyzer {
  private int SAMPLING_RATE;
  private int FRAME_LENGTH_IN_MS;
  public static final double MPS_TO_KMH = 3.6;   // m/s -> km/h

  // Storage of 20 consecutive raw x values, meanSquareRoot in these buffers
  private List<Float> xValueBuffer = new ArrayList<>();
  private List<Double> meanSquareRootBuffer = new ArrayList<>();

  // for punch identification, a specified number of frames has to be ignored
  // (exact number depends on the sampling rate, see constructor)
  private int PUNCH_BLOCKED_FRAMES;
  private int MISTAKE_BLOCKED_FRAMES;

  // used for counting ignored frames / frames for mistake identification
  private int ignoreCount = 0;
  private boolean ignoreFrames = false;
  private int identificationCount = 0;

  // x value thresholds for registering punches and arm drops
  private double X_PUNCH_THRESHOLD = 75.0;
  private double X_MISTAKE_THRESHOLD = -20.0;




  public PunchAnalyzer(int samplingRate) {

    for(int i = 0; i < 20; ++i) {       // buffer size should always stay the same after
      xValueBuffer.add((float) i + 1);   // adding data!
      meanSquareRootBuffer.add((double) i + 1);
    }

    SAMPLING_RATE = samplingRate;
    FRAME_LENGTH_IN_MS = 1000 / SAMPLING_RATE;
    PUNCH_BLOCKED_FRAMES = samplingRate / 5;
    MISTAKE_BLOCKED_FRAMES = 2 * PUNCH_BLOCKED_FRAMES;
  }

  public void nextFrame(float x, float y, float z) {

    double meanSquareRoot = Math.sqrt((((x * x) + (y * y) + (z * z)) / 3.0));
    xValueBuffer.add(x);                       // new element at the end,
    xValueBuffer.remove(0);              // oldest element removed
    meanSquareRootBuffer.add(meanSquareRoot);
    meanSquareRootBuffer.remove(0);

    analyzeX(x);
  }

  /*
   * Analyzes current data buffer for punches and mistakes, calls calculatePunchVelocity()
   * when a punch is recognized
   */
  private void analyzeX(float X) {
    /*
     * Punch recognition:
     * if x value is at least 75, a punch is registered and calculatePunchVelocity() is called
     * next 5 data frames are ignored to prevent registering
     * multiple punches
     */
    if(X >= X_PUNCH_THRESHOLD && !ignoreFrames) {
      System.out.println("X over 75: Punch recognized");  // Punch recognised
      ignoreFrames = true;
      ignoreCount = PUNCH_BLOCKED_FRAMES;

      calculatePunchVelocity();
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
        identificationCount = MISTAKE_BLOCKED_FRAMES;       // starts correct/incorrect identification
      }
    }

    if (identificationCount > 0) {
      --identificationCount;
      if (X < X_MISTAKE_THRESHOLD) {                  // correct punch recognised
        System.out.println("correct Punch!");
        identificationCount = 0;        // reset to prevent counting multiple times
      } else if (identificationCount == 0)
        System.out.println("Incorrect Punch!");
    }
  }

  /*
   * Calculates Punch velocity, currently prints result to console
   */
  private void calculatePunchVelocity() {

    int startIndex= findStartIndex();
    int endIndex = findEndIndex();

    System.out.println("Start Index: " + startIndex);
    System.out.println("End Index: " + endIndex);

    if(startIndex == -1 || endIndex == -1) {
      System.out.println("Speed calculation: Index error!");
      return;
    }

    // ------ speed calculation     ------

    float speedInMps = 0;
    float speedMSR = 0;
    for (int i = startIndex; i < endIndex - 1; ++i) {
      speedInMps += (FRAME_LENGTH_IN_MS * Math.abs(xValueBuffer.get(i)));
      speedMSR += (FRAME_LENGTH_IN_MS * Math.abs(meanSquareRootBuffer.get(i)));
      if (i < endIndex - 2) {
        speedInMps += ((FRAME_LENGTH_IN_MS * Math.abs(xValueBuffer.get(i)
            - xValueBuffer.get(i + 1))) / 2.0);
        speedMSR += ((FRAME_LENGTH_IN_MS * Math.abs(meanSquareRootBuffer.get(i)
            - meanSquareRootBuffer.get(i + 1))) / 2.0);
      }
      else {
        speedInMps += (((FRAME_LENGTH_IN_MS / 2.0) * Math.abs(xValueBuffer.get(i))) / 2.0);
        speedMSR += (((FRAME_LENGTH_IN_MS / 2.0) * Math.abs(meanSquareRootBuffer.get(i))) / 2.0);
      }
      speedInMps -= FRAME_LENGTH_IN_MS * 10.0;
      speedMSR -= FRAME_LENGTH_IN_MS * 5.74;
    }

    speedInMps /= 1000; //framelength is in ms
    speedMSR /= 1000;

    System.out.println("Apprx. speed in meters per second: " + speedInMps);
    System.out.println("Apprx. speed in kilometer per hour: " + (speedInMps * MPS_TO_KMH));
    System.out.println("Apprx. speed(MSR) in meters per second: " + speedMSR);
    System.out.println("Apprx. speed(MSR) in kilometer per hour: " + (speedMSR * MPS_TO_KMH));
    // ------ end speed calculation ------

  }

  /*
   * Finds the Element indicating that the punching movement begins
   */
  private int findStartIndex() {

    int startIndex = 0;

    int frameBufferIndex = xValueBuffer.size() - 5;      // last elements are usually > 0!
    boolean continueStartSearch = true;


    while (continueStartSearch) {                       // search for element > -10.0 starts here,

      if (xValueBuffer.get(frameBufferIndex) > -10.0) {
        startIndex = frameBufferIndex;
        continueStartSearch = false;
        for (int i = 0; i < xValueBuffer.size(); ++i) {    // only for debugging
          System.out.println(i + ": " + xValueBuffer.get(i));
        }
      }

      if (frameBufferIndex == 0) {
        continueStartSearch = false;
        System.out.println("Start not found!");
        startIndex = -1;
      }
      --frameBufferIndex;
    }
    return startIndex;
  }

  /*
   * Finds the Element indicating that the punch connects
   */
  private int findEndIndex() {
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
        System.out.println("End not found!");
        endIndex = -1;
      }
      --frameBufferIndex;
    }

    return endIndex;
  }

}
