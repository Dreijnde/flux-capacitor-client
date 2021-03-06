package io.fluxcapacitor.javaclient.publishing.dataprotection;

import io.fluxcapacitor.common.Guarantee;
import io.fluxcapacitor.common.api.SerializedMessage;
import io.fluxcapacitor.common.handling.Handler;
import io.fluxcapacitor.common.reflection.ReflectionUtils;
import io.fluxcapacitor.javaclient.common.Message;
import io.fluxcapacitor.javaclient.common.serialization.DeserializingMessage;
import io.fluxcapacitor.javaclient.common.serialization.Serializer;
import io.fluxcapacitor.javaclient.keyvalue.KeyValueStore;
import io.fluxcapacitor.javaclient.publishing.DispatchInterceptor;
import io.fluxcapacitor.javaclient.tracking.handling.HandlerInterceptor;
import lombok.AllArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static io.fluxcapacitor.common.reflection.ReflectionUtils.getAnnotatedFields;
import static io.fluxcapacitor.common.reflection.ReflectionUtils.getProperty;
import static io.fluxcapacitor.common.reflection.ReflectionUtils.setField;
import static java.util.UUID.randomUUID;

@AllArgsConstructor
public class DataProtectionInterceptor implements DispatchInterceptor, HandlerInterceptor {

    public static String METADATA_KEY = "$protectedData";

    private final KeyValueStore keyValueStore;
    private final Serializer serializer;
    
    @Override
    @SuppressWarnings("unchecked")
    public Function<Message, SerializedMessage> interceptDispatch(Function<Message, SerializedMessage> function) {
        return m -> {
            Map<String, String> protectedFields = new HashMap<>();
            if (m.getMetadata().containsKey(METADATA_KEY)) {
                protectedFields.putAll(m.getMetadata().get(METADATA_KEY, Map.class));
            } else {
                Object payload = m.getPayload();
                getAnnotatedFields(m.getPayload(), ProtectData.class).forEach(field -> {
                    Object value = getProperty(field, payload);
                    if (value != null) {
                        String key = randomUUID().toString();
                        keyValueStore.store(key, value, Guarantee.STORED);
                        protectedFields.put(field.getName(), key);
                    }
                });
                if (!protectedFields.isEmpty()) {
                    m.getMetadata().put(METADATA_KEY, protectedFields);
                }
            }
            if (!protectedFields.isEmpty()) {
                Object payloadCopy = serializer.deserialize(serializer.serialize(m.getPayload()));
                protectedFields.forEach((name, key) -> setField(name, payloadCopy, null));
                m = m.withPayload(payloadCopy);
            }
            return function.apply(m);
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    public Function<DeserializingMessage, Object> interceptHandling(Function<DeserializingMessage, Object> function,
                                                                    Handler<DeserializingMessage> handler,
                                                                    String consumer) {
        return m -> {
            if (m.getMetadata().containsKey(METADATA_KEY)) {
                Object payload = m.getPayload();
                Map<String, String> protectedFields = m.getMetadata().get(METADATA_KEY, Map.class);
                boolean dropProtectedData = handler.getMethod(m).isAnnotationPresent(DropProtectedData.class);
                protectedFields.forEach((fieldName, key) -> {
                    ReflectionUtils.setField(fieldName, payload, keyValueStore.get(key));
                    if (dropProtectedData) {
                        keyValueStore.delete(key);
                    }
                });
            }
            return function.apply(m);
        };
    }
}
