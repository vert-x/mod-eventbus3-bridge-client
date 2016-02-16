/*
 * Copyright 2015 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package org.vertx.java.eventbus3.bridge;

import org.vertx.java.core.Handler;
import org.vertx.java.core.eventbus.ReplyException;
import org.vertx.java.core.eventbus.ReplyFailure;
import org.vertx.java.core.json.JsonObject;

/**
 * Message wrapper
 *
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class BridgeMessage extends JsonObject {

  private final EventBus3 eb;
  private final String replyAddress;
  private final boolean failure;

  BridgeMessage(EventBus3 eb, JsonObject json) {
    super(json.toMap());
    this.eb = eb;

    if (json.containsField("replyAddress")) {
      replyAddress = json.getString("replyAddress");
    } else {
      replyAddress = null;
    }

    failure = "err".equals(json.getString("type"));
  }

  public boolean succeeded() {
    return !failure;
  }

  public boolean failed() {
    return failure;
  }

  public void reply(JsonObject message, JsonObject headers, Handler<BridgeMessage> callback) {
    eb.send(replyAddress, message, headers, callback);
  }

  public void reply(JsonObject message, Handler<BridgeMessage> callback) {
    eb.send(replyAddress, message, callback);
  }

  public void reply(JsonObject message, JsonObject headers) {
    eb.send(replyAddress, message, headers);
  }

  public void reply(JsonObject message) {
    eb.send(replyAddress, message);
  }

  public ReplyException cause() {
    if (failed()) {
      return new ReplyException(
          ReplyFailure.fromInt(getInteger("failureType", 2)),
          getInteger("failureCode"),
          getString("message"));
    }

    return null;
  }

  public JsonObject body() {
    return getObject("body");
  }

  public JsonObject headers() {
    return getObject("headers");
  }
}
