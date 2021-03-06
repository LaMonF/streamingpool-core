// @formatter:off
/**
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
package org.streamingpool.core.service.streamid.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.Test;
import org.reactivestreams.Publisher;
import org.streamingpool.core.service.StreamId;
import org.streamingpool.core.support.RxStreamSupport;
import org.streamingpool.core.testing.AbstractStreamTest;

import io.reactivex.Flowable;
import io.reactivex.subscribers.TestSubscriber;

/**
 * Unit tests for {@link ComposedStreams}.
 *
 * @author timartin
 */
public class ComposedStreamsTest extends AbstractStreamTest implements RxStreamSupport {

    private static final StreamId<Object> DUMMY_STREAM_ID_1 = new StreamId<Object>() {
        /* FOR TESTINF PURPOSES */
    };

    private static final StreamId<Object> DUMMY_STREAM_ID_2 = new StreamId<Object>() {
        /* FOR TESTINF PURPOSES */
    };

    @Test(expected = NullPointerException.class)
    public void testMappedStreamWithNullSourceStreamId() {
        ComposedStreams.mappedStream(null, val -> Optional.of(val));
    }

    @Test(expected = NullPointerException.class)
    public void testMappedStreamWithNullConversionFunction() {
        StreamId<Integer> sourceStreamId = provide(Flowable.<Integer> empty()).withUniqueStreamId();
        ComposedStreams.mappedStream(sourceStreamId, null);
    }

    @Test
    public void testMappedStreamWithConversionThatAlwaysReturns() {
        StreamId<Integer> sourceStreamId = provide(Flowable.<Integer> just(1, 3)).withUniqueStreamId();
        StreamId<Integer> mappedStreamId = ComposedStreams.mappedStream(sourceStreamId, val -> val + 1);
        TestSubscriber<Integer> subscriber = createSubscriberAndWait(mappedStreamId);
        assertThat(subscriber.values()).hasSize(2).containsExactly(2, 4);
    }

    @Test
    public void testMappedStreamWithConversionThatDoesNotAlwaysReturns() {
        StreamId<Integer> sourceStreamId = provide(Flowable.<Integer> just(1, 3)).withUniqueStreamId();
        StreamId<Integer> mappedStreamId = ComposedStreams.mappedStream(sourceStreamId, val -> (val == 1) ? val : null);
        TestSubscriber<Integer> subscriber = createSubscriberAndWait(mappedStreamId);
        assertThat(subscriber.values()).hasSize(1).containsExactly(1);
    }

    @Test
    public void testEqualityOfMappedStreamIdOnEqualStreams() {
        StreamId<Object> sourceStreamId = DUMMY_STREAM_ID_1;
        Function<Object, Object> conversion = Optional::of;
        StreamId<Object> mappedStream1 = ComposedStreams.mappedStream(sourceStreamId, conversion);
        StreamId<Object> mappedStream2 = ComposedStreams.mappedStream(sourceStreamId, conversion);
        assertThat(mappedStream1).isEqualTo(mappedStream2);
    }

    @Test
    public void testEqualityOfMappedStreamIdOnNotEqualConversion() {
        StreamId<Object> sourceStreamId = DUMMY_STREAM_ID_1;
        StreamId<Object> mappedStream1 = ComposedStreams.mappedStream(sourceStreamId, Optional::of);
        StreamId<Object> mappedStream2 = ComposedStreams.mappedStream(sourceStreamId, Optional::of);
        assertThat(mappedStream1).isNotEqualTo(mappedStream2);
    }

    @Test
    public void testEqualityOfMappedStreamIdOnNotEqualStreamSources() {
        Function<Object, Object> conversion = Optional::of;
        StreamId<Object> mappedStream1 = ComposedStreams.mappedStream(DUMMY_STREAM_ID_1, conversion);
        StreamId<Object> mappedStream2 = ComposedStreams.mappedStream(DUMMY_STREAM_ID_2, conversion);
        assertThat(mappedStream1).isNotEqualTo(mappedStream2);
    }

    @Test(expected = NullPointerException.class)
    public void testFlatMappedStreamWithNullSourceStreamId() {
        ComposedStreams.flatMappedStream(null, val -> Flowable.just(val));
    }

    @Test(expected = NullPointerException.class)
    public void testFlatMappedStreamWithNullConversionFunction() {
        StreamId<Integer> sourceStreamId = provide(Flowable.<Integer> empty()).withUniqueStreamId();
        ComposedStreams.flatMappedStream(sourceStreamId, null);
    }

    @Test
    public void testFlatMappedStreamWithConversionAlwaysReturns() {
        StreamId<Integer> sourceStreamId = provide(Flowable.<Integer> just(1, 3)).withUniqueStreamId();
        StreamId<Integer> flatMappedStreamId = ComposedStreams.flatMappedStream(sourceStreamId,
                val -> Flowable.<Integer> just(val, val));
        TestSubscriber<Integer> subscriber = createSubscriberAndWait(flatMappedStreamId);
        assertThat(subscriber.values()).hasSize(4).containsExactly(1, 1, 3, 3);
    }

    @Test
    public void testFlatMappedStreamWithConversionThatDoesNotAlwaysReturns() {
        StreamId<Integer> sourceStreamId = provide(Flowable.<Integer> just(1, 3)).withUniqueStreamId();
        StreamId<Integer> flatMappedStreamId = ComposedStreams.flatMappedStream(sourceStreamId,
                val -> (val == 1) ? Flowable.<Integer> just(val, val) : Flowable.<Integer> empty());
        TestSubscriber<Integer> subscriber = createSubscriberAndWait(flatMappedStreamId);
        assertThat(subscriber.values()).hasSize(2).containsExactly(1, 1);
    }

    @Test
    public void testEqualityOfFlatMappedStreamIdOnEqualStreams() {
        StreamId<Object> sourceStreamId = DUMMY_STREAM_ID_1;
        Function<Object, Publisher<Object>> conversion = o -> Flowable.empty();
        StreamId<Object> flatMappedStream1 = ComposedStreams.flatMappedStream(sourceStreamId, conversion);
        StreamId<Object> flatMappedStream2 = ComposedStreams.flatMappedStream(sourceStreamId, conversion);
        assertThat(flatMappedStream1).isEqualTo(flatMappedStream2);
    }

    @Test
    public void testEqualityOfFlatMappedStreamIdOnNotEqualStreamSources() {
        Function<Object, Publisher<Object>> conversion = o -> Flowable.empty();
        StreamId<Object> flatMappedStream1 = ComposedStreams.flatMappedStream(DUMMY_STREAM_ID_1, conversion);
        StreamId<Object> flatMappedStream2 = ComposedStreams.flatMappedStream(DUMMY_STREAM_ID_2, conversion);
        assertThat(flatMappedStream1).isNotEqualTo(flatMappedStream2);
    }

    @Test
    public void testEqualityOfFlatMappedStreamIdOnNotEqualConversions() {
        StreamId<Object> sourceStreamId = DUMMY_STREAM_ID_1;
        StreamId<Object> flatMappedStream1 = ComposedStreams.flatMappedStream(sourceStreamId, o -> Flowable.empty());
        StreamId<Object> flatMappedStream2 = ComposedStreams.flatMappedStream(sourceStreamId, o -> Flowable.empty());
        assertThat(flatMappedStream1).isNotEqualTo(flatMappedStream2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergedStreamWithNullSourceStreamIds() {
        ComposedStreams.mergedStream(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMergedStreamWithEmptySourceStreamIds() {
        ComposedStreams.mergedStream(Collections.emptyList());
    }

    @Test
    public void testMergedStreamWithSingleSourceStreamId() {
        StreamId<Integer> sourceStreamId = provide(Flowable.<Integer> just(1, 3)).withUniqueStreamId();
        StreamId<Integer> mergedStreamId = ComposedStreams.mergedStream(Arrays.asList(sourceStreamId));
        TestSubscriber<Integer> subscriber = createSubscriberAndWait(mergedStreamId);
        assertThat(subscriber.values()).hasSize(2).containsExactly(1, 3);
    }

    @Test
    public void testMergedStreamWithMultipleSourceStreamIds() {
        StreamId<Integer> sourceStreamId1 = provide(Flowable.<Integer> just(1, 3)).withUniqueStreamId();
        StreamId<Integer> sourceStreamId2 = provide(Flowable.<Integer> just(2, 4)).withUniqueStreamId();
        StreamId<Integer> mergedStreamId = ComposedStreams
                .mergedStream(Arrays.asList(sourceStreamId1, sourceStreamId2));
        TestSubscriber<Integer> subscriber = createSubscriberAndWait(mergedStreamId);
        assertThat(subscriber.values()).hasSize(4).contains(1, 2, 3, 4);
    }

    @Test
    public void testEqualityOfMergedStreamIdOnEqualStreams() {
        StreamId<Object> sourceStreamId1 = DUMMY_STREAM_ID_1;
        StreamId<Object> sourceStreamId2 = DUMMY_STREAM_ID_2;
        StreamId<Object> mergedStream1 = ComposedStreams.mergedStream(Arrays.asList(sourceStreamId1, sourceStreamId2));
        StreamId<Object> mergedStream2 = ComposedStreams.mergedStream(Arrays.asList(sourceStreamId1, sourceStreamId2));
        assertThat(mergedStream1).isEqualTo(mergedStream2);
    }

    @Test
    public void testEqualityOfMergedStreamIdOnNotEqualStreamSources() {
        StreamId<Object> sourceStreamId1 = DUMMY_STREAM_ID_1;
        StreamId<Object> sourceStreamId2 = DUMMY_STREAM_ID_2;
        StreamId<Object> mergedStream1 = ComposedStreams.mergedStream(Arrays.asList(sourceStreamId1, sourceStreamId1));
        StreamId<Object> mergedStream2 = ComposedStreams.mergedStream(Arrays.asList(sourceStreamId2, sourceStreamId2));
        assertThat(mergedStream1).isNotEqualTo(mergedStream2);
    }

    @Test(expected = NullPointerException.class)
    public void testFilteredStreamWithNullSourceStreamId() {
        ComposedStreams.filteredStream(null, val -> true);
    }

    @Test(expected = NullPointerException.class)
    public void testFilteredStreamWithNullPredicate() {
        StreamId<Integer> sourceStreamId = provide(Flowable.<Integer> empty()).withUniqueStreamId();
        ComposedStreams.filteredStream(sourceStreamId, null);
    }

    @Test
    public void testFilteredStreamWithCorrectValues() {
        StreamId<Integer> sourceStreamId = provide(Flowable.<Integer> just(1, 3)).withUniqueStreamId();
        StreamId<Integer> filteredStreamId = ComposedStreams.filteredStream(sourceStreamId, val -> val == 1);
        TestSubscriber<Integer> subscriber = createSubscriberAndWait(filteredStreamId);
        assertThat(subscriber.values()).hasSize(1).contains(1);
    }

    @Test
    public void testEqualityOfFilteredStreamIdOnEqualStreams() {
        StreamId<Object> sourceStreamId = DUMMY_STREAM_ID_1;
        Predicate<Object> predicate = o -> true;
        StreamId<Object> filteredStream1 = ComposedStreams.filteredStream(sourceStreamId, predicate);
        StreamId<Object> filteredStream2 = ComposedStreams.filteredStream(sourceStreamId, predicate);
        assertThat(filteredStream1).isEqualTo(filteredStream2);
    }

    @Test
    public void testEqualityOfFilteredStreamIdOnNotEqualStreamSources() {
        Predicate<Object> predicate = o -> true;
        StreamId<Object> filteredStream1 = ComposedStreams.filteredStream(DUMMY_STREAM_ID_1, predicate);
        StreamId<Object> filteredStream2 = ComposedStreams.filteredStream(DUMMY_STREAM_ID_2, predicate);
        assertThat(filteredStream1).isNotEqualTo(filteredStream2);
    }

    @Test
    public void testEqualityOfFilteredStreamIdOnNotEqualPredicates() {
        StreamId<Object> sourceStreamId = DUMMY_STREAM_ID_1;
        StreamId<Object> filteredStream1 = ComposedStreams.filteredStream(sourceStreamId, o -> true);
        StreamId<Object> filteredStream2 = ComposedStreams.filteredStream(sourceStreamId, o -> true);
        assertThat(filteredStream1).isNotEqualTo(filteredStream2);
    }

    @Test(expected = NullPointerException.class)
    public void testDelayedStreamWithNullSourceStreamId() {
        ComposedStreams.delayedStream(null, Duration.ZERO);
    }

    @Test(expected = NullPointerException.class)
    public void testDelayedStreamWithNullDuration() {
        StreamId<Integer> sourceStreamId = provide(Flowable.<Integer> empty()).withUniqueStreamId();
        ComposedStreams.delayedStream(sourceStreamId, null);
    }

    @Test
    public void testDelayedStreamWithCorrectValues() throws InterruptedException {
        final long delay = 2000;
        final long deltaDelay = 500;
        StreamId<Integer> sourceStreamId = provide(Flowable.<Integer> just(1)).withUniqueStreamId();
        StreamId<Integer> delayedStreamId = ComposedStreams.delayedStream(sourceStreamId, Duration.ofMillis(delay));
        TestSubscriber<Integer> subscriber = TestSubscriber.create();
        discover(delayedStreamId).subscribe(subscriber);

        Instant before = Instant.now();
        subscriber.await();
        Instant after = Instant.now();

        assertThat(subscriber.values()).hasSize(1).containsExactly(1);
        assertThat(Duration.between(before, after).toMillis()).isBetween(delay - deltaDelay, delay + deltaDelay);
    }

    @Test
    public void testEqualityOfDelayedStreamIdOnEqualStreams() {
        StreamId<Object> sourceStreamId = DUMMY_STREAM_ID_1;
        StreamId<Object> delayedStream1 = ComposedStreams.delayedStream(sourceStreamId, Duration.ZERO);
        StreamId<Object> delayedStream2 = ComposedStreams.delayedStream(sourceStreamId, Duration.ZERO);
        assertThat(delayedStream1).isEqualTo(delayedStream2);
    }

    @Test
    public void testEqualityOfDelayedStreamIdOnNotEqualStreamSources() {
        StreamId<Object> delayedStream1 = ComposedStreams.delayedStream(DUMMY_STREAM_ID_1, Duration.ZERO);
        StreamId<Object> delayedStream2 = ComposedStreams.delayedStream(DUMMY_STREAM_ID_2, Duration.ZERO);
        assertThat(delayedStream1).isNotEqualTo(delayedStream2);
    }

    @Test
    public void testEqualityOfDelayedStreamIdOnNotEqualDurations() {
        StreamId<Object> sourceStreamId = DUMMY_STREAM_ID_1;
        StreamId<Object> delayedStream1 = ComposedStreams.delayedStream(sourceStreamId, Duration.ofNanos(0));
        StreamId<Object> delayedStream2 = ComposedStreams.delayedStream(sourceStreamId, Duration.ofNanos(1));
        assertThat(delayedStream1).isNotEqualTo(delayedStream2);
    }

    @Test(expected = NullPointerException.class)
    public void testZippedStreamWithNullLeftStreamSourceId() {
        StreamId<Integer> sourceStreamId = provide(Flowable.<Integer> empty()).withUniqueStreamId();
        ComposedStreams.zippedStream(null, sourceStreamId, (val1, val2) -> Optional.empty());
    }

    @Test(expected = NullPointerException.class)
    public void testZippedStreamWithNullRightStreamSourceId() {
        StreamId<Integer> sourceStreamId = provide(Flowable.<Integer> empty()).withUniqueStreamId();
        ComposedStreams.zippedStream(sourceStreamId, null, (val1, val2) -> Optional.empty());
    }

    @Test(expected = NullPointerException.class)
    public void testZippedStreamWithNullZipFunction() {
        StreamId<Integer> sourceStreamId1 = provide(Flowable.<Integer> empty()).withUniqueStreamId();
        StreamId<Integer> sourceStreamId2 = provide(Flowable.<Integer> empty()).withUniqueStreamId();
        ComposedStreams.zippedStream(sourceStreamId1, sourceStreamId2, null);
    }

    @Test
    public void testZippedStreamWithZipThatAlwaysReturns() {
        StreamId<Integer> sourceStreamId1 = provide(Flowable.<Integer> just(1, 3)).withUniqueStreamId();
        StreamId<Integer> sourceStreamId2 = provide(Flowable.<Integer> just(2, 4)).withUniqueStreamId();
        StreamId<Integer> zippedStreamId = ComposedStreams.zippedStream(sourceStreamId1, sourceStreamId2,
                (val1, val2) -> Optional.<Integer> of(val1 + val2));
        TestSubscriber<Integer> subscriber = createSubscriberAndWait(zippedStreamId);
        assertThat(subscriber.values()).hasSize(2).containsExactly(3, 7);
    }

    @Test
    public void testZippedStreamWithZipThatDoesNotAlwaysReturns() {
        StreamId<Integer> sourceStreamId1 = provide(Flowable.<Integer> just(1, 3)).withUniqueStreamId();
        StreamId<Integer> sourceStreamId2 = provide(Flowable.<Integer> just(2, 4)).withUniqueStreamId();
        StreamId<Integer> zippedStreamId = ComposedStreams.zippedStream(sourceStreamId1, sourceStreamId2,
                (val1, val2) -> (val1 == 1) ? Optional.<Integer> empty() : Optional.<Integer> of(val1 + val2));
        TestSubscriber<Integer> subscriber = createSubscriberAndWait(zippedStreamId);
        assertThat(subscriber.values()).hasSize(1).containsExactly(7);
    }

    @Test
    public void testEqualityOfZippedStreamIdOnEqualStreams() {
        StreamId<Object> sourceStreamId1 = DUMMY_STREAM_ID_1;
        StreamId<Object> sourceStreamId2 = DUMMY_STREAM_ID_2;
        BiFunction<Object, Object, Optional<Object>> conversion = (o1, o2) -> Optional.empty();
        StreamId<Object> zippedStream1 = ComposedStreams.zippedStream(sourceStreamId1, sourceStreamId2, conversion);
        StreamId<Object> zippedStream2 = ComposedStreams.zippedStream(sourceStreamId1, sourceStreamId2, conversion);
        assertThat(zippedStream1).isEqualTo(zippedStream2);
    }

    @Test
    public void testEqualityOfZippedStreamIdOnNotEqualStreamSources() {
        BiFunction<Object, Object, Optional<Object>> conversion = (o1, o2) -> Optional.empty();
        StreamId<Object> zippedStream1 = ComposedStreams.zippedStream(DUMMY_STREAM_ID_1, DUMMY_STREAM_ID_1, conversion);
        StreamId<Object> zippedStream2 = ComposedStreams.zippedStream(DUMMY_STREAM_ID_2, DUMMY_STREAM_ID_2, conversion);
        assertThat(zippedStream1).isNotEqualTo(zippedStream2);
    }

    @Test
    public void testEqualityOfZippedStreamIdOnNotEqualConverions() {
        StreamId<Object> sourceStreamId1 = DUMMY_STREAM_ID_1;
        StreamId<Object> sourceStreamId2 = DUMMY_STREAM_ID_2;
        StreamId<Object> zippedStream1 = ComposedStreams.zippedStream(sourceStreamId1, sourceStreamId2,
                (o1, o2) -> Optional.empty());
        StreamId<Object> zippedStream2 = ComposedStreams.zippedStream(sourceStreamId1, sourceStreamId2,
                (o1, o2) -> Optional.empty());
        assertThat(zippedStream1).isNotEqualTo(zippedStream2);
    }

    private final TestSubscriber<Integer> createSubscriberAndWait(StreamId<Integer> sourceStreamId) {
        TestSubscriber<Integer> subscriber = TestSubscriber.create();
        discover(sourceStreamId).subscribe(subscriber);
        try {
            subscriber.await();
        } catch (InterruptedException e) {
            fail("Interrupted wait", e);
        }
        return subscriber;
    }
}
