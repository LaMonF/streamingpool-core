// @formatter:off
/*
*
* This file is part of streaming pool (http://www.streamingpool.org).
* 
* Copyright (c) 2017-present, CERN. All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* 
*/
// @formatter:on

package org.streamingpool.core.service.streamfactory;

import static io.reactivex.BackpressureOverflowStrategy.DROP_OLDEST;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.reactivex.Flowable;
import io.reactivex.flowables.ConnectableFlowable;
import org.reactivestreams.Publisher;
import org.streamingpool.core.domain.ErrorStreamPair;
import org.streamingpool.core.service.DiscoveryService;
import org.streamingpool.core.service.StreamFactory;
import org.streamingpool.core.service.StreamId;
import org.streamingpool.core.service.streamid.BufferSpecification;
import org.streamingpool.core.service.streamid.BufferSpecification.EndStreamMatcher;
import org.streamingpool.core.service.streamid.OverlapBufferStreamId;
import org.streamingpool.core.service.util.DoAfterFirstSubscribe;

/**
 * Factory for {@link OverlapBufferStreamId}
 * 
 * @see OverlapBufferStreamId
 * @author acalia
 */
public class OverlapBufferStreamFactory implements StreamFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(OverlapBufferStreamFactory.class);

    @SuppressWarnings("unchecked")
    @Override
    public <T> ErrorStreamPair<T> create(StreamId<T> id, DiscoveryService discoveryService) {
        if (!(id instanceof OverlapBufferStreamId)) {
            return ErrorStreamPair.empty();
        }

        OverlapBufferStreamId<?> analysisId = (OverlapBufferStreamId<?>) id;

        BufferSpecification bufferSpecification = analysisId.bufferSpecification();
        long bufferCapacity = analysisId.getBackpressureBufferCapacity();

        StreamId<?> startId = bufferSpecification.startId();
        StreamId<?> sourceId = analysisId.sourceId();

        Flowable<?> timeout = bufferSpecification.timeout();

        ConnectableFlowable<?> startStream = Flowable.fromPublisher(discoveryService.discover(startId)).publish();
        ConnectableFlowable<?> sourceStream = Flowable.fromPublisher(discoveryService.discover(sourceId)).publish();

        Set<EndStreamMatcher<?, ?>> matchers = bufferSpecification.endStreamMatchers();
        Map<EndStreamMatcher<Object, Object>, ConnectableFlowable<?>> endStreams = matchers.stream()
                .collect(Collectors.toMap(m -> (EndStreamMatcher<Object, Object>) m,
                        m -> Flowable.fromPublisher(discoveryService.discover(m.endStreamId())).publish()));

        Flowable<?> bufferStream = sourceStream
                .compose(new DoAfterFirstSubscribe<>(() -> {
                    endStreams.values().forEach(ConnectableFlowable::connect);
                    startStream.connect();
                    sourceStream.connect();
                }))
                .buffer(startStream,
                        opening -> closingStreamFor(opening, endStreams, timeout))
                .onBackpressureBuffer(bufferCapacity,
                        () -> LOGGER.warn("Dropping element on backpressure"),
                        DROP_OLDEST);
        return ErrorStreamPair.ofData((Publisher<T>) bufferStream);
    }

    private Flowable<?> closingStreamFor(Object opening,
            Map<EndStreamMatcher<Object, Object>, ConnectableFlowable<?>> endStreams, Flowable<?> timeout) {

        Set<Flowable<?>> matchingEndStreams = endStreams.entrySet().stream()
                .map(e -> e.getValue().filter(v -> e.getKey().matching().test(opening, v))).collect(Collectors.toSet());

        matchingEndStreams.add(timeout);
        return Flowable.merge(matchingEndStreams).take(1);
    }
}
