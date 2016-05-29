/**
 * Copyright (c) 2016 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package stream.support;

import static akka.stream.javadsl.AsPublisher.WITH_FANOUT;

import org.reactivestreams.Publisher;

import akka.NotUsed;
import akka.stream.Attributes;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import stream.ReactStream;
import stream.ReactStreams;
import stream.StreamId;
import stream.akka.AkkaSourceProvidingService;

public interface AkkaStreamSupport extends StreamSupport {

    Materializer materializer();

    AkkaSourceProvidingService sourceProvidingService();

    default <Out, Mat> ReactStream<Out> streamFrom(Source<Out, Mat> akkaSource) {
        return ReactStreams.fromPublisher(publisherFrom(akkaSource));
    }

    default <Out, Mat> StreamSupport.OngoingProviding<Out> provideMaterialized(Source<Out, Mat> akkaSource) {
        return provide(streamFrom(akkaSource));
    }

    default <Out, Mat> OngoingAkkaSourceProviding<Out> provide(Source<Out, Mat> akkaSource) {
        return new OngoingAkkaSourceProviding<>(sourceProvidingService(), akkaSource);
    }

    default <T, U> Publisher<T> publisherFrom(Source<T, U> source) {
        Sink<T, Publisher<T>> akkaSink = Sink.<T> asPublisher(WITH_FANOUT).withAttributes(Attributes.inputBuffer(1, 1));
        return source.runWith(akkaSink, materializer());
    }

    default <T> Source<T, NotUsed> sourceFrom(StreamId<T> id) {
        return ReactStreams.sourceFrom(discover(id));
    }

    class OngoingAkkaSourceProviding<T> {
        private final Source<T, ?> akkaSource;
        private final AkkaSourceProvidingService sourceProvidingService;

        public OngoingAkkaSourceProviding(AkkaSourceProvidingService sourceProvidingService, Source<T, ?> akkaSource) {
            this.sourceProvidingService = sourceProvidingService;
            this.akkaSource = akkaSource;
        }

        public void as(StreamId<T> id) {
            sourceProvidingService.provide(id, akkaSource);
        }

    }

}
