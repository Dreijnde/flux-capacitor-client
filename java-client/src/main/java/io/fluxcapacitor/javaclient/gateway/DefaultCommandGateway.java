package io.fluxcapacitor.javaclient.gateway;

import io.fluxcapacitor.common.api.Metadata;
import io.fluxcapacitor.javaclient.common.serialization.MessageSerializer;
import lombok.AllArgsConstructor;

import java.util.concurrent.CompletableFuture;

@AllArgsConstructor
public class DefaultCommandGateway implements CommandGateway {

    private final GatewayClient commandGateway;
    private final RequestHandler requestHandler;
    private final MessageSerializer serializer;

    @Override
    public void sendAndForget(Object payload, Metadata metadata) {
        try {
            commandGateway.send(serializer.serialize(payload, metadata));
        } catch (Exception e) {
            throw new GatewayException(String.format("Failed to send and forget command %s", payload), e);
        }
    }

    @Override
    public <R> CompletableFuture<R> send(Object payload, Metadata metadata) {
        try {
            return requestHandler.sendRequest(serializer.serialize(payload, metadata), commandGateway::send)
                    .thenApply(s -> serializer.deserialize(s).getPayload());
        } catch (Exception e) {
            throw new GatewayException(String.format("Failed to send command %s", payload), e);
        }
    }
}
