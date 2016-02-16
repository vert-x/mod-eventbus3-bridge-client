package org.vertx.eventbus3.bridge;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.eventbus3.bridge.BridgeMessage;
import org.vertx.java.eventbus3.bridge.EventBus3;
import org.vertx.testtools.TestVerticle;
import org.vertx.testtools.VertxAssert;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.vertx.testtools.VertxAssert.*;

public class EventBus3Test extends TestVerticle {

  private static TestServer vertx3;
  private EventBus3 eb;

  @BeforeClass
  public static void startServer() {
    final CountDownLatch latch = new CountDownLatch(1);
    vertx3 = new TestServer();

    vertx3.lineHandler = new Handler<String>() {
      @Override
      public void handle(String s) {
        if (s.contains("Succeeded in deploying verticle")) {
          latch.countDown();
        }
      }
    };

    vertx3.start();

    try {
      latch.await(5, TimeUnit.SECONDS);
      if (latch.getCount() != 0) {
        throw new RuntimeException("Failed to start server");
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public void start() {
    VertxAssert.initialize(vertx);

    eb = new EventBus3(
        vertx,
        new JsonObject()
            .putString("host", "localhost")
            .putNumber("port", Integer.parseInt(System.getProperty("tcpPort", "7000"))),
        new Handler<Void>() {
          @Override
          public void handle(Void aVoid) {
            vertx.runOnContext(new Handler<Void>() {
              @Override
              public void handle(Void aVoid) {
                EventBus3Test.super.start();
              }
            });
          }
        });
  }

  @Test
  public void testSend() {
    vertx3.lineHandler = new Handler<String>() {
      @Override
      public void handle(String s) {
        if (s.equals("{\"msg\":\"ping\"}")) {
          testComplete();
        }
      }
    };

    eb.send("send2", new JsonObject().putString("msg", "ping"));
  }

  @Test
  public void testPublish() {
    final AtomicInteger cnt = new AtomicInteger(2);

    vertx3.lineHandler = new Handler<String>() {
      @Override
      public void handle(String s) {
        if (s.contains("Consumer #")) {
          if (cnt.decrementAndGet() == 0) {
            testComplete();
          }
        }
      }
    };

    eb.publish("publish2", new JsonObject().putString("msg", "ping"));
  }

  @Test
  public void testPingPong() {
    // ignore
    vertx3.lineHandler = null;

    eb.send("reply2", new JsonObject().putString("msg", "ping"), new Handler<BridgeMessage>() {
      @Override
      public void handle(BridgeMessage msg) {
        assertEquals("pong", msg.body().getString("msg"));
        testComplete();
      }
    });
  }

  @Test
  public void testSendFrom3() {
    eb.registerHandler("send3", new Handler<BridgeMessage>() {
      @Override
      public void handle(BridgeMessage msg) {
        assertEquals("hello send", msg.body().getString("msg"));
        testComplete();
      }
    });
  }

  @Test
  public void testPublishFrom3() {
    final AtomicBoolean recv1 = new AtomicBoolean(false);
    final AtomicBoolean recv2 = new AtomicBoolean(false);

    eb.registerHandler("publish3", new Handler<BridgeMessage>() {
      @Override
      public void handle(BridgeMessage msg) {
        recv1.set(true);

        if (recv1.get() && recv2.get()) {
          testComplete();
        }
      }
    });

    eb.registerHandler("publish3", new Handler<BridgeMessage>() {
      @Override
      public void handle(BridgeMessage msg) {
        recv2.set(true);

        if (recv1.get() && recv2.get()) {
          testComplete();
        }
      }
    });
  }

  @Test
  public void testPingPongFrom3() {
    eb.registerHandler("reply3", new Handler<BridgeMessage>() {
      @Override
      public void handle(BridgeMessage msg) {
        vertx3.lineHandler = new Handler<String>() {
          @Override
          public void handle(String s) {
            if (s.equals("{\"msg\":\"pong\"}")) {
              testComplete();
            }
          }
        };

        msg.reply(new JsonObject().putString("msg", "pong"));
      }
    });
  }
}
