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

package io.fluxcapacitor.axonclient.common.configuration;

import io.fluxcapacitor.common.MessageType;
import io.fluxcapacitor.javaclient.common.websocket.ServiceUrlBuilder;
import io.fluxcapacitor.javaclient.configuration.client.WebSocketClient;
import io.fluxcapacitor.javaclient.eventsourcing.client.EventStoreClient;
import io.fluxcapacitor.javaclient.eventsourcing.client.WebSocketEventStoreClient;
import io.fluxcapacitor.javaclient.keyvalue.client.KeyValueClient;
import io.fluxcapacitor.javaclient.keyvalue.client.WebsocketKeyValueClient;
import io.fluxcapacitor.javaclient.publishing.client.GatewayClient;
import io.fluxcapacitor.javaclient.publishing.client.WebsocketGatewayClient;
import io.fluxcapacitor.javaclient.tracking.client.TrackingClient;
import io.fluxcapacitor.javaclient.tracking.client.WebsocketTrackingClient;
import org.axonframework.config.Configurer;

public class WebsocketFluxCapacitorConfiguration extends AbstractFluxCapacitorConfiguration {

    private final WebSocketClient.Properties clientProperties;

    public static Configurer configure(Configurer configurer, WebSocketClient.Properties clientProperties) {
        return new WebsocketFluxCapacitorConfiguration(clientProperties).configure(configurer);
    }

    public WebsocketFluxCapacitorConfiguration(WebSocketClient.Properties clientProperties) {
        super(WebSocketClient.newInstance(clientProperties));
        this.clientProperties = clientProperties;
    }

    @Override
    protected TrackingClient createConsumerService(MessageType type) {
        return new WebsocketTrackingClient(ServiceUrlBuilder.consumerUrl(type, clientProperties));
    }

    @Override
    protected GatewayClient createProducerService(MessageType type) {
        return new WebsocketGatewayClient(ServiceUrlBuilder.producerUrl(type, clientProperties));
    }

    @Override
    protected EventStoreClient createEventStore() {
        return new WebSocketEventStoreClient(ServiceUrlBuilder.eventSourcingUrl(clientProperties));
    }

    @Override
    protected KeyValueClient createKeyValueClient() {
        return new WebsocketKeyValueClient(ServiceUrlBuilder.keyValueUrl(clientProperties));
    }
}
