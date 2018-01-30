package io.fluxcapacitor.javaclient.configuration.client;

import io.fluxcapacitor.common.MessageType;
import io.fluxcapacitor.javaclient.eventsourcing.client.EventStoreClient;
import io.fluxcapacitor.javaclient.eventsourcing.client.InMemoryEventStoreClient;
import io.fluxcapacitor.javaclient.eventsourcing.client.WebSocketEventStoreClient;
import io.fluxcapacitor.javaclient.keyvalue.client.InMemoryKeyValueClient;
import io.fluxcapacitor.javaclient.keyvalue.client.KeyValueClient;
import io.fluxcapacitor.javaclient.keyvalue.client.WebsocketKeyValueClient;
import io.fluxcapacitor.javaclient.publishing.client.GatewayClient;
import io.fluxcapacitor.javaclient.publishing.client.WebsocketGatewayClient;
import io.fluxcapacitor.javaclient.scheduling.client.InMemorySchedulingClient;
import io.fluxcapacitor.javaclient.scheduling.client.SchedulingClient;
import io.fluxcapacitor.javaclient.scheduling.client.WebsocketSchedulingClient;
import io.fluxcapacitor.javaclient.tracking.client.InMemoryMessageStore;
import io.fluxcapacitor.javaclient.tracking.client.TrackingClient;
import io.fluxcapacitor.javaclient.tracking.client.WebsocketTrackingClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static io.fluxcapacitor.common.ObjectUtils.memoize;
import static io.fluxcapacitor.javaclient.common.websocket.ServiceUrlBuilder.*;

public class FluxCapacitorClient {

    private final Function<MessageType, ? extends GatewayClient> gatewayClients;
    private final Function<MessageType, ? extends TrackingClient> trackingClients;
    private final EventStoreClient eventStoreClient;
    private final SchedulingClient schedulingClient;
    private final KeyValueClient keyValueClient;
    private final ClientProperties properties;

    public FluxCapacitorClient(
            Function<MessageType, ? extends GatewayClient> gatewayClients,
            Function<MessageType, ? extends TrackingClient> trackingClients,
            EventStoreClient eventStoreClient, SchedulingClient schedulingClient,
            KeyValueClient keyValueClient, ClientProperties properties) {
        this.gatewayClients = memoize(gatewayClients);
        this.trackingClients = memoize(trackingClients);
        this.eventStoreClient = eventStoreClient;
        this.schedulingClient = schedulingClient;
        this.keyValueClient = keyValueClient;
        this.properties = properties;
    }

    public static FluxCapacitorClient usingWebSockets(WebSocketClientProperties properties) {
        return new FluxCapacitorClient(
                type -> new WebsocketGatewayClient(producerUrl(type, properties)),
                type -> new WebsocketTrackingClient(consumerUrl(type, properties)),
                new WebSocketEventStoreClient(eventSourcingUrl(properties)),
                new WebsocketSchedulingClient(schedulingUrl(properties)),
                new WebsocketKeyValueClient(keyValueUrl(properties)), properties);
    }

    public static FluxCapacitorClient usingInMemory(InMemoryClientProperties properties) {
        InMemorySchedulingClient schedulingClient = new InMemorySchedulingClient();
        InMemoryEventStoreClient eventStoreClient = new InMemoryEventStoreClient();
        Map<MessageType, InMemoryMessageStore> messageStores = new ConcurrentHashMap<>();
        Function<MessageType, InMemoryMessageStore> messageStoreFactory = type -> messageStores.computeIfAbsent(
                type, t -> {
                    switch (t) {
                        case EVENT:
                            return eventStoreClient;
                        case SCHEDULE:
                            return schedulingClient;
                        default:
                            return new InMemoryMessageStore();
                    }
                });
        return new FluxCapacitorClient(messageStoreFactory, messageStoreFactory, eventStoreClient,
                                       schedulingClient, new InMemoryKeyValueClient(), properties);
    }

    public GatewayClient getGatewayClient(MessageType messageType) {
        return gatewayClients.apply(messageType);
    }

    public TrackingClient getTrackingClient(MessageType messageType) {
        return trackingClients.apply(messageType);
    }

    public EventStoreClient getEventStoreClient() {
        return eventStoreClient;
    }

    public SchedulingClient getSchedulingClient() {
        return schedulingClient;
    }

    public KeyValueClient getKeyValueClient() {
        return keyValueClient;
    }

    public ClientProperties getProperties() {
        return properties;
    }
}