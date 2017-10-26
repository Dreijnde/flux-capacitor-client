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

package io.fluxcapacitor.javaclient.common.serialization.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.fluxcapacitor.common.api.Data;
import io.fluxcapacitor.javaclient.common.serialization.AbstractSerializer;
import io.fluxcapacitor.javaclient.common.serialization.upcasting.Upcaster;
import io.fluxcapacitor.javaclient.common.serialization.upcasting.UpcasterChain;

import java.util.Collection;
import java.util.Collections;

public class JacksonSerializer extends AbstractSerializer {
    public static final ObjectMapper defaultObjectMapper =
            new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private final ObjectMapper objectMapper;

    public JacksonSerializer() {
        this(Collections.emptyList());
    }

    public JacksonSerializer(Collection<?> upcasters) {
        this(defaultObjectMapper, upcasters);
    }

    public JacksonSerializer(ObjectMapper objectMapper, Collection<?> upcasters) {
        this(objectMapper, UpcasterChain.create(upcasters, new ObjectNodeConverter(objectMapper)));
    }

    public JacksonSerializer(ObjectMapper objectMapper, Upcaster<Data<byte[]>> upcasterChain) {
        super(upcasterChain);
        this.objectMapper = objectMapper;
    }

    @Override
    protected byte[] doSerialize(Object object) throws Exception {
        return objectMapper.writeValueAsBytes(object);
    }

    @Override
    protected <T> T doDeserialize(byte[] bytes, Class<? extends T> type) throws Exception {
        return objectMapper.readValue(bytes, type);
    }

}