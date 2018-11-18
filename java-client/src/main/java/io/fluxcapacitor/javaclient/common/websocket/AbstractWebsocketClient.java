/*
 * Copyright (c) 2016-2017 Flux Capacitor.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.fluxcapacitor.javaclient.common.websocket;

import io.fluxcapacitor.common.Awaitable;
import io.fluxcapacitor.common.TimingUtils;
import io.fluxcapacitor.common.api.JsonType;
import io.fluxcapacitor.common.api.QueryResult;
import io.fluxcapacitor.common.api.Request;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.tyrus.client.ClientManager;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.Session;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static javax.websocket.CloseReason.CloseCodes.NO_STATUS_CODE;

@Slf4j
public abstract class AbstractWebsocketClient implements AutoCloseable {
    private final ClientManager client;
    private final URI endpointUri;
    private final Duration reconnectDelay;
    private final Map<Long, WebSocketRequest> requests = new ConcurrentHashMap<>();
    private final AtomicReference<Session> session = new AtomicReference<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    public AbstractWebsocketClient(URI endpointUri) {
        this(ClientManager.createClient(), endpointUri, Duration.ofSeconds(1));
    }

    public AbstractWebsocketClient(ClientManager client, URI endpointUri, Duration reconnectDelay) {
        this.client = client;
        this.endpointUri = endpointUri;
        this.reconnectDelay = reconnectDelay;
    }

    @SneakyThrows
    protected Awaitable send(Object object) {
        getSession().getBasicRemote().sendObject(object);
        return Awaitable.ready();
    }

    @SuppressWarnings("unchecked")
    protected <R extends QueryResult> R sendRequest(Request request) {
        WebSocketRequest webSocketRequest = new WebSocketRequest(request);
        requests.put(request.getRequestId(), webSocketRequest);
        try {
            webSocketRequest.send(getSession());
            return (R) webSocketRequest.getResult();
        } catch (Exception e) {
            requests.remove(request.getRequestId());
            throw new IllegalStateException("Failed to handle request " + request, e);
        }
    }

    @OnMessage
    public void onMessage(JsonType value) {
        QueryResult readResult = (QueryResult) value;
        WebSocketRequest webSocketRequest = requests.remove(readResult.getRequestId());
        if (webSocketRequest == null) {
            log.warn("Could not find outstanding read request for id {}", readResult.getRequestId());
        } else {
            webSocketRequest.complete(readResult);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        if (closeReason.getCloseCode().getCode() > NO_STATUS_CODE.getCode()) {
            log.warn("Connection to endpoint {} closed with reason {}", session.getRequestURI(), closeReason);
        }
        retryOutstandingRequests(session.getId());
    }

    protected void retryOutstandingRequests(String sessionId) {
        if (!closed.get() && !requests.isEmpty()) {
            try {
                sleep(reconnectDelay.toMillis());
            } catch (InterruptedException e) {
                currentThread().interrupt();
                throw new IllegalStateException("Thread interrupted while trying to retry outstanding requests", e);
            }
            requests.values().stream().filter(r -> sessionId.equals(r.getSessionId())).forEach(r -> {
                try {
                    r.send(getSession());
                } catch (Exception e) {
                    r.completeExceptionally(e);
                }
            });
        }
    }

    @OnError
    public void onError(Session session, Throwable e) {
        log.error("Client side error for web socket connected to endpoint {}", session.getRequestURI(), e);
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            Session session = getSession();
            if (session != null) {
                try {
                    session.close();
                } catch (IOException e) {
                    log.warn("Failed to closed websocket session connected to endpoint {}. Reason: {}", 
                             session.getRequestURI(), e.getMessage());
                }
            }
            if (session != null && !requests.isEmpty()) {
                log.warn("Closed websocket session to endpoint {} with {} outstanding requests",
                         session.getRequestURI(), requests.size());
            }
        }
    }

    protected Session getSession() {
        return session.updateAndGet(s -> {
            while (!closed.get() && (s == null || !s.isOpen())) {
                s = TimingUtils.retryOnFailure(() -> client.connectToServer(this, endpointUri), 
                                               reconnectDelay, e -> !closed.get());
            }
            return s;
        });
    }

    @RequiredArgsConstructor
    protected class WebSocketRequest {
        private final Request request;
        private final CompletableFuture<QueryResult> result = new CompletableFuture<>();
        @Getter private volatile String sessionId;
        
        protected void send(Session session) {
            this.sessionId = session.getId();
            AbstractWebsocketClient.this.send(request);
        }

        protected void completeExceptionally(Throwable e) {
            result.completeExceptionally(e);
        }

        protected void complete(QueryResult value) {
            result.complete(value);
        }
        
        public QueryResult getResult() throws ExecutionException, InterruptedException {
            return result.get();
        }
    }

}
