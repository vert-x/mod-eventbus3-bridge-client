package org.vertx.eventbus3.bridge;

import org.vertx.java.core.Handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class TestServer extends Thread {

  Handler<String> lineHandler;

  private void process(String line) {
    if (line.startsWith("INFO: ")) {
      if (lineHandler != null) {
        lineHandler.handle(line.substring(6));
      } else {
        System.err.println("Unexpected: " + line.substring(6));
      }
    }
  }

  @Override
  public void run() {
    try {

      File dir = new File("src/test/resources");
      final Process p = Runtime.getRuntime().exec("java -jar tcp-bridge-server.jar", new String[0], dir);

      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          p.destroy();
        }
      });

      new Thread(new Runnable() {
        @Override
        public void run() {
          try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getErrorStream()))) {
            String line;
            while ((line = input.readLine()) != null) {
              process(line);
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }).start();

      new Thread(new Runnable() {
        @Override
        public void run() {
          try (BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String line;
            while ((line = input.readLine()) != null) {
              process(line);
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }).start();

      int res = p.waitFor();
      System.out.println("DONE!");

    } catch (InterruptedException | IOException e) {
      throw new RuntimeException(e);
    }
  }
}
