package org.vertx.java.eventbus3.bridge;

import org.vertx.java.core.Handler;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.platform.Verticle;

public class EventBus3BridgeClientVerticle extends Verticle {

  private EventBus3 eb;

  @Override
  public void start() {
    eb = new EventBus3(vertx, getContainer().config(), new Handler<Void>() {
      @Override
      public void handle(Void done) {
        // case #1 vertx2 send a message to vertx3
        eb.send("send2", new JsonObject().putString("msg", "hello send"));

        // case #2 vertx2 publish a message to vertx3
        eb.publish("publish2", new JsonObject().putString("msg", "hello publish"));

        // case #3 vertx2 send a message to vertx3 and expects a reply
        eb.send("reply2", new JsonObject().putString("msg", "ping"), new Handler<BridgeMessage>() {
          @Override
          public void handle(BridgeMessage msg) {
            System.out.println(msg.body());
          }
        });

        // reverse

        // case #1 vertx3 send a message to vertx2
        eb.registerHandler("send3", new Handler<BridgeMessage>() {
          @Override
          public void handle(BridgeMessage msg) {
            System.out.println(msg.body());
          }
        });

        // case #2 vertx3 publish a message to vertx2
        eb.registerHandler("publish3", new Handler<BridgeMessage>() {
          @Override
          public void handle(BridgeMessage msg) {
            System.out.println("Consumer #1: " + msg.body());
          }
        });

        eb.registerHandler("publish3", new Handler<BridgeMessage>() {
          @Override
          public void handle(BridgeMessage msg) {
            System.out.println("Consumer #2: " + msg.body());
          }
        });

        // case #3 vertx3 send a message to vertx2 and expects a reply
        eb.registerHandler("reply3", new Handler<BridgeMessage>() {
          @Override
          public void handle(BridgeMessage msg) {
            System.out.println(msg.body());
            msg.reply(new JsonObject().putString("msg", "pong"));
          }
        });
      }
    });

    eb.exceptionHandler(new Handler<Throwable>() {
      @Override
      public void handle(Throwable throwable) {
        throwable.printStackTrace();
      }
    });
  }

  @Override
  public void stop() {
    eb.close();
  }
}
