package org.example;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;


public class Main {

  public static int SAMPLING_RATE = 25;   // in Hz

  public static String FILE_NAME = "Sample_2.txt";    // correct samples in Sample_1.txt,
  // incorrect samples in Sample_2.txt

  public static void readDataFile(String fileName, PunchAnalyzer punchAnalyzer) {

    try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
      String line;

      while ((line = br.readLine()) != null) {

        String[] values = line.split(",");

        float x = Float.valueOf(values[0]);
        float y = Float.valueOf(values[1]);     // currently not used
        float z = Float.valueOf(values[2]);

        punchAnalyzer.nextFrame(x, y, z);
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

    PunchAnalyzer punchAnalyzer = new PunchAnalyzer(SAMPLING_RATE);
    readDataFile(FILE_NAME, punchAnalyzer);

  }
}