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
package org.vertx.groovy.eventbus3.bridge

import org.vertx.groovy.core.Vertx
import org.vertx.java.core.Handler
import org.vertx.java.core.json.JsonObject

import org.vertx.java.eventbus3.bridge.EventBus3 as JEventBus3
import org.vertx.java.eventbus3.bridge.BridgeMessage as JBridgeMessage

/**
 * TCP EventBus bridge client for Vert.x3
 *
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
public class EventBus3 {

    private final JEventBus3 eb

    public EventBus3(Vertx vertx, Map<String, Object> config, final Closure<Void> ready) {
        eb = new org.vertx.java.eventbus3.bridge.EventBus3(vertx.toJavaVertx(), new JsonObject(config), new Handler<Void>() {
            @Override
            public void handle(Void aVoid) {
                ready.call()
            }
        })
    }

    public EventBus3(Vertx vertx, Map<String, Object> config) {
        eb = new org.vertx.java.eventbus3.bridge.EventBus3(vertx.toJavaVertx(), new JsonObject(config))
    }

    public void send(String address, Map<String, Object> message, Map<String, Object> headers = [:], Closure<BridgeMessage> callback = null) {
        if (callback == null) {
            eb.send(address, new JsonObject(message), new JsonObject(headers))
        } else {
            eb.send(address, new JsonObject(message), new JsonObject(headers), new Handler<JBridgeMessage>() {
                @Override
                void handle(JBridgeMessage msg) {
                    callback.call(new BridgeMessage(msg))
                }
            })
        }
    }

    public void publish(String address, Map<String, Object> message, Map<String, Object> headers = [:]) {
        eb.publish(address, new JsonObject(message), new JsonObject(headers))
    }

    public void registerHandler(String address, Map<String, Object> headers = [:], Closure<BridgeMessage> callback) {
        eb.registerHandler(address, new JsonObject(headers), new Handler<JBridgeMessage>() {
            @Override
            void handle(JBridgeMessage msg) {
                callback.call(new BridgeMessage(msg))
            }
        })
    }

    public void unregisterHandler(String address, Map<String, Object> headers = [:], Closure<BridgeMessage> callback) {
        eb.unregisterHandler(address, new JsonObject(headers), new Handler<JBridgeMessage>() {
            @Override
            void handle(JBridgeMessage msg) {
                callback.call(new BridgeMessage(msg))
            }
        })
    }

    public void close() {
        eb.close()
    }

    public void exceptionHandler(Closure<Throwable> handler) {
        eb.exceptionHandler(new Handler<Throwable>() {
            @Override
            void handle(Throwable throwable) {
                handler.call(throwable)
            }
        })
    }

    public void endHandler(Closure<Void> handler) {
        eb.endHandler(new Handler<Void>() {
            @Override
            void handle(Void v) {
                handler.call(v)
            }
        })
    }
}
