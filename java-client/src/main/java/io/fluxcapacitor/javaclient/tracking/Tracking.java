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

package io.fluxcapacitor.javaclient.tracking;

import io.fluxcapacitor.common.Registration;
import io.fluxcapacitor.common.api.Message;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility that creates and starts one or more {@link DefaultTracker Trackers} of the same name and configuration. Each
 * tracker claims a single thread.
 */
public class Tracking {

    public static Registration start(String name, TrackingService trackingService, Consumer<List<Message>> consumer) {
        return start(name, 1, trackingService, consumer);
    }

    public static Registration start(String name, int threads, TrackingService trackingService,
                                     Consumer<List<Message>> consumer) {
        return start(name, consumer, trackingService, TrackingConfiguration.builder().threads(threads).build());
    }

    public static Registration start(String consumerName, Consumer<List<Message>> consumer,
                                     TrackingService trackingService, TrackingConfiguration configuration) {
        List<DefaultTracker> instances =
                IntStream.range(0, configuration.getThreads()).mapToObj(
                        i -> new DefaultTracker(consumerName, i, configuration, consumer, trackingService)).collect(
                        Collectors.toList());
        ExecutorService executor = Executors.newFixedThreadPool(configuration.getThreads());
        instances.forEach(executor::submit);
        return () -> {
            instances.forEach(DefaultTracker::cancel);
            executor.shutdown();
            return true;
        };
    }
}
