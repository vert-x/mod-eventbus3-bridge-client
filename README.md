# Vert.x3 eventbus bridge client for Vert.x2

This project will allow bidirectional communication between a vert.x2 and a vert.x3 application.

## Installation

Add the following dependency to your project:

### maven

```xml
<dependency>
  <groupId>io.vertx</groupId>
  <artifactId>mod-eventbus3-bridge-client</artifactId>
  <version>1.0.0</version>
</dependency>
```

### gradle

```
compile 'io.vertx:mod-eventbus3-bridge-client:1.0.0'
```

## Configuration

This module assumes that the [TCP bridge](http://vertx.io/docs/vertx-tcp-eventbus-bridge/java/) is installed on Vert.x3,
refer to the documentation on how to get it on your Vert.x3 application. Once a bridge is installed it is possible to
connect to it from your Vert.x2 application using this module.

The configuration for this module is read from the container config, this means that it should be provided in the
standard `config.json` file.

The full list os configuration properties are:
----------------------------------

| Property          | Mandatory | Default Value | Comments                                         |
|:------------------|:----------|:-------------:|:-------------------------------------------------|
| host              | ✔         |               | hostname where the vert.x3 tcp bridge is running |
| port              |           | 7000          | tcp port of the vert.x3 bridge                   |
| reconnectAttempts |           |               |                                                  |
| reconnectInterval |           |               |                                                  |
| connectionTimeout |           |               |                                                  |
| pingInterval      |           | 5000          |                                                  |
| defaultHeaders    |           | {}            | a json object                                    |

And and example would be:

```json
{
  "host": "localhost",
  "port": 7000,
  "reconnectAttempts": 3,
  "reconnectInterval": 5000,
  "connectionTimeout": 30000,
  "pingInterval": 5000,

  "defaultHeaders": {}
}
```

## QuickStart

The eventbus allows message communication between verticles and has 3 modes of operation:

* point to point message (send)
* fan out messages (publish)
* rpc (send-reply)

The client and bridge allow this communication no matter which side the messages are originated.

### Send from vert.x3 to vert.x2

#### Vert.x3 code:

```java
public class Example {

  public static void main(String[] args) {

    Vertx vertx = Vertx.vertx();
    TcpEventBusBridge bridge = TcpEventBusBridge.create(
        vertx,
        new BridgeOptions()
            .addInboundPermitted(new PermittedOptions())
            .addOutboundPermitted(new PermittedOptions()));

    bridge.listen(7000, res -> {
      // case #1 vertx3 send a message to vertx2
      vertx.eventBus().send("send3", new JsonObject().put("msg", "hello send"));
    });
  }
}
```

In this example a bridge is created allowing any messages in and out of the eventbus. The bridge will accept TCP connections on port 7000 and once it is ready it will send a message to address `send3` with the payload `{msg: "hello send"}`.


#### Vert.x2 (this module) code:

```java
public class Example extends Verticle {

  @Override
  public void start() {
    final EventBus3 eb = new EventBus3(vertx, getContainer().config(), new Handler<Void>() {
      @Override
      public void handle(Void done) {
        // case #1 vertx3 send a message to vertx2
        eb.registerHandler("send3", new Handler<BridgeMessage>() {
          @Override
          public void handle(BridgeMessage msg) {
            System.out.println(msg.body());
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
}
```

This verticle starts by creating a connection to an existing bridge using the container config options (see above for what configurations are possible) and once the TCP connection is established, registers an handler on address `send3`. Once a message is received at that address it will be printed on the console.

To handle any error an exception handler is also set up and will print the stack trace of the error.


### Publish from vert.x3 to vert.x2

For simplicity the configuration is ommited since it is the same as the previous example.

#### Vert.x3 code:

```java
eb.publish("publish3", new JsonObject().put("msg", "hello publish"));
```

This will publish (send one message to all registered addresses).


#### Vert.x2 (this module) code:

```java
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
```

This code registers 2 handlers on the same address (`publish3`) so when a publish command is sent from Vert.x3 it will be received by both handlers.


### Send Reply from vert.x3 to vert.x2

#### Vert.x3 code:

```java
eb.send("reply3", new JsonObject().put("msg", "ping"), msg -> {
  System.out.println(msg.result().body());
});
```

Send a message and when a reply is received the reply is printed to the standard output.


#### Vert.x2 (this module) code:

```java
eb.registerHandler("reply3", new Handler<BridgeMessage>() {
  @Override
  public void handle(BridgeMessage msg) {
    System.out.println(msg.body());
    msg.reply(new JsonObject().putString("msg", "pong"));
  }
});
```

Set up a handler to receive a a message to address `reply3` and reply back with `{msg: "pong"}`.


### Send from vert.x2 to vert.x3

#### Vert.x2 (this module) code:

```java
eb.send("send2", new JsonObject().putString("msg", "hello send"));
```

Send a message from Vert.x2 to an address on Vert.x3.


#### Vert.x3 code:

```java
eb.consumer("send2", msg -> {
  System.out.println(msg.body());
});
```

Once a message is received on Vert.x3 print out its body.


### Publish from vert.x2 to vert.x3

#### Vert.x2 (this module) code:

```java
eb.publish("publish2", new JsonObject().putString("msg", "hello publish"));
```

Send one to many messages.

#### Vert.x3 code:

```java
eb.consumer("publish2", msg -> {
  System.out.println("Consumer #1: " + msg.body());
});

eb.consumer("publish2", msg -> {
  System.out.println("Consumer #2: " + msg.body());
});
```

In this example register 2 handlers on the same address so once a message is published to the address `publish2` then both handlers will print out its body.


### Send Reply from vert.x2 to vert.x3

#### Vert.x2 (this module) code:

```java
eb.send("reply2", new JsonObject().putString("msg", "ping"), new Handler<BridgeMessage>() {
  @Override
  public void handle(BridgeMessage msg) {
    System.out.println(msg.body());
  }
});
```

Send a message to Vert.x3 and once a reply is received print out the reply body.

#### Vert.x3 code:

```java
eb.consumer("reply2", msg -> {
  System.out.println(msg.body());
  msg.reply(new JsonObject().put("msg", "pong"));
});
```

Register a consumer for messages addressed to `reply2` and once a message is received reply back with the message `{msg: "pong"}`.


For complete source code examples refer to:

* [examples/vertx2/EventBus3BridgeClientVerticle.java](examples/vertx2/EventBus3BridgeClientVerticle.java)
* [examples/vertx3/TcpEventBusBridgePingPongServer.java](examples/vertx3/TcpEventBusBridgePingPongServer.java)
