package io.vertx.ext.eventbus.bridge.tcp;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.bridge.BridgeOptions;
import io.vertx.ext.bridge.PermittedOptions;

public class TcpEventBusBridgePingPongServer {

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();
    TcpEventBusBridge bridge = TcpEventBusBridge.create(
        vertx,
        new BridgeOptions()
            .addInboundPermitted(new PermittedOptions())
            .addOutboundPermitted(new PermittedOptions()));

    bridge.listen(7000, res -> {

      final EventBus eb = vertx.eventBus();

      // case #1 vertx2 send a message to vertx3
      eb.consumer("send2", msg -> {
        System.out.println(msg.body());
      });

      // case #2 vertx2 publish a message to vertx3
      eb.consumer("publish2", msg -> {
        System.out.println("Consumer #1: " + msg.body());
      });

      eb.consumer("publish2", msg -> {
        System.out.println("Consumer #2: " + msg.body());
      });

      // case #3 vertx2 send a message to vertx3 and expects a reply
      eb.consumer("reply2", msg -> {
        System.out.println(msg.body());
        msg.reply(new JsonObject().put("msg", "pong"));
      });

      // reverse test

      vertx.setPeriodic(10000, v -> {
        // case #1 vertx3 send a message to vertx2
        eb.send("send3", new JsonObject().put("msg", "hello send"));

        // case #2 vertx3 publish a message to vertx2
        eb.publish("publish3", new JsonObject().put("msg", "hello publish"));

        // case #3 vertx3 send a message to vertx2 and expects a reply
        eb.send("reply3", new JsonObject().put("msg", "ping"), msg -> {
          System.out.println(msg.result().body());
        });
      });
    });
  }
}
