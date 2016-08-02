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

import org.vertx.java.core.Handler
import org.vertx.java.core.eventbus.ReplyException
import org.vertx.java.core.json.JsonObject

import org.vertx.java.eventbus3.bridge.BridgeMessage as JBridgeMessage

/**
 * Message wrapper
 *
 * @author <a href="mailto:plopes@redhat.com">Paulo Lopes</a>
 */
class BridgeMessage {

    private final JBridgeMessage delegate

    BridgeMessage(JBridgeMessage delegate) {
        this.delegate = delegate
    }

    @Override
    public Object getAt(String key) {
        return delegate.getValue(key)
    }

    @Override
    public void putAt(String key, Object value) {
        delegate.putValue(key, value)
    }

    public boolean isSucceeded() {
        return delegate.succeeded()
    }

    public boolean isFailed() {
        return delegate.failed()
    }

    public void reply(Map<String, Object> message, Map<String, Object> headers = [:], Closure<BridgeMessage> callback = null) {
        if (callback == null) {
            delegate.reply(new JsonObject(message), new JsonObject(headers))
        } else {
            delegate.reply(new JsonObject(message), new JsonObject(headers), new Handler<JBridgeMessage>() {
                @Override
                void handle(JBridgeMessage msg) {
                    callback.call(new BridgeMessage(msg))
                }
            })
        }
    }

    public ReplyException getCause() {
        return delegate.cause()
    }

    public Map<String, Object> getBody() {
        return delegate.body()?.toMap()
    }

    public Map<String, Object> getHeaders() {
        return delegate.headers()?.toMap()
    }

}
