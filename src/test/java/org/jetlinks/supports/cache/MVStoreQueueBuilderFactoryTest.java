package org.jetlinks.supports.cache;

import lombok.SneakyThrows;
import org.jetlinks.core.cache.FileQueue;
import org.jetlinks.core.utils.Reactors;
import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MVStoreQueueBuilderFactoryTest {


    @Test
    @SneakyThrows
    public void testBack() {
        AtomicInteger total = new AtomicInteger(10_0000);

        Sinks.Many<Integer> sink = FileQueue
                .<Integer>builder()
                .name("test")
                .path(Paths.get("./target/buf.queue"))
                .buildFluxProcessor(false);

        sink.asFlux()
            .subscribe(i -> total.addAndGet(-1));

        Flux.range(0, total.get())
            .flatMap(i -> Mono.fromRunnable(() -> sink.emitNext(i, Reactors.emitFailureHandler()))
                              .subscribeOn(Schedulers.parallel()))
            .then()
            .as(StepVerifier::create)
            .expectComplete()
            .verify();

        assertEquals(0, total.get());

    }

    @Test
    @SneakyThrows
    public void test() {

        FileQueue<String> strings = FileQueue.<String>builder()
                                             .name("test")
                                             .path(Paths.get("./target/.queue"))
                                             .build();
        int numberOf = 20_0000;
        long time = System.currentTimeMillis();
        Duration writeTime = Flux
                .range(0, numberOf)
                .doOnNext(i -> {
                    strings.add("data:" + i);
                })
                .buffer(10000)
                .then()
                .as(StepVerifier::create)
                .verifyComplete();
        System.out.println("writeTime:" + writeTime);
        strings.flush();
        assertEquals(strings.size(), numberOf);

        Duration pollTime = Flux
                .range(0, numberOf)
                .map(i -> strings.poll())
                .as(StepVerifier::create)
                .expectNextCount(numberOf)
                .verifyComplete();
        System.out.println("pollTime:" + pollTime);

        assertTrue(strings.isEmpty());

        System.out.println(System.currentTimeMillis() - time);
        strings.flush();
        strings.close();
    }

    @Test
    public void testFlux() {
        Sinks.Many<String> processor = FileQueue
                .<String>builder()
                .name("test-flux")
                .path(Paths.get("./target/.queue"))
                .buildFluxProcessor(true);

        processor.tryEmitNext("test");
        processor
                .asFlux()
                .take(1)
                .as(StepVerifier::create)
                .expectNext("test")
                .verifyComplete();

    }
}