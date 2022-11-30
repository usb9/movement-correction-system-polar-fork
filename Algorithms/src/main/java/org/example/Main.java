package org.example;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

  public static List<Float> frameBuffer = new ArrayList<>();

  public static List<Double> meanSquareRootBuffer = new ArrayList<>();
  public static int SAMPLING_RATE = 25;   // in Hz
  public static int FRAME_LENGTH_IN_MS = 1000 / SAMPLING_RATE;
  public static double MPS_TO_KMH = 3.6;
  public static String FILE_NAME = "Sample_2.txt";    // correct samples in Sample_1.txt,
  // incorrect samples in Sample_2.txt
  public static int startIndexSearch() {

    int startIndex = 0;

    int frameBufferIndex = frameBuffer.size() - 5;      // last elements are usually > 0!
    boolean continueStartSearch = true;


    while (continueStartSearch) {                       // search for element > -10.0 starts here,

      if (frameBuffer.get(frameBufferIndex) > -10.0) {
        startIndex = frameBufferIndex;
        continueStartSearch = false;
        for (int i = 0; i < frameBuffer.size(); ++i) {    // only for debugging
          System.out.println(i + ": " + frameBuffer.get(i));
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
  public static int endIndexSearch() {
    int endIndex = 0;
    int frameBufferIndex = frameBuffer.size() - 1;      // start from last element
    boolean continueEndSearch = true;

    while (continueEndSearch) {                       // search for element > -10.0 starts here,

      if (frameBuffer.get(frameBufferIndex) < 0) {
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
  // DO NOT CHANGE FRAMEBUFFER IN HERE!
  public static void calculateSpeed() {

    int startIndex= startIndexSearch();
    int endIndex = endIndexSearch();

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
      speedInMps += (FRAME_LENGTH_IN_MS * Math.abs(frameBuffer.get(i)));
      speedMSR += (FRAME_LENGTH_IN_MS * Math.abs(meanSquareRootBuffer.get(i)));
      if (i < endIndex - 2) {
        speedInMps += ((FRAME_LENGTH_IN_MS * Math.abs(frameBuffer.get(i)
            - frameBuffer.get(i + 1))) / 2.0);
        speedMSR += ((FRAME_LENGTH_IN_MS * Math.abs(meanSquareRootBuffer.get(i)
            - meanSquareRootBuffer.get(i + 1))) / 2.0);
      }
      else {
        speedInMps += (((FRAME_LENGTH_IN_MS / 2.0) * Math.abs(frameBuffer.get(i))) / 2.0);
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
  public static void analyzeDataFile(String fileName) {

    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
      String line;

      //for blocking data frames
      int blockedCount = 0;
      boolean blocked = false;

      //for identifying correct/incorrect punches
      int identificationCount = 0;


      while ((line = br.readLine()) != null) {

        String[] values = line.split(",");

        float x = Float.valueOf(values[0]);
        float y = Float.valueOf(values[1]);     // currently not used
        float z = Float.valueOf(values[2]);

        double meanSquareRoot = Math.sqrt((((x * x) + (y * y) + (z * z)) / 3.0));
        frameBuffer.add(x);                       // new element at the end,
        frameBuffer.remove(0);              // oldest element removed
        meanSquareRootBuffer.add(meanSquareRoot);
        meanSquareRootBuffer.remove(0);


        /*
         * Punch recognition:
         * if x value is at least 75, a punch is registered
         * next 5 data frames are ignored to prevent registering
         * multiple punches
         *
         * Identifying if arm is dropped after punch:
         * after the skipped frames for punch recognition,
         * check if x drops below -20 at least once in the next 10
         * frames
         * */

        if (x >= 75.0 && !blocked) {
          System.out.println("X over 75: Punch recognized");  // Punch recognised
          blocked = true;
          blockedCount = 5;                                   // ignore next 5 frames

          calculateSpeed();
        }

        if (blocked) {
          --blockedCount;
          if (blockedCount <= 0) {
            blocked = false;
            identificationCount = 10;       // starts correct/incorrect identification
          }
        }

        if (identificationCount > 0) {
          --identificationCount;
          if (x < -20.0) {                  // correct punch recognised
            System.out.println("correct Punch!");
            identificationCount = 0;        // reset to prevent counting multiple times
          } else if (identificationCount == 0)
            System.out.println("Incorrect Punch!");
        }


      }

    }

    catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {


    //File directory = new File("./");      // check if sample files are saved in this directory!
    //System.out.println(directory.getAbsolutePath());

    for(int i = 0; i < 20; ++i) {       // buffer size should always stay the same after adding
      frameBuffer.add((float) i + 1);   // real data!
      meanSquareRootBuffer.add((double) i + 1);
    }

    analyzeDataFile(FILE_NAME);

  }
}