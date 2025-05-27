/*
 * MIT License
 *
 * Copyright (c) 2025 Cristiano Silva
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package edu.example.myjavalab.reactor.mono;

import static edu.example.myjavalab.reactor.mono.internal.SomeUtils.ERROR_MESSAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import edu.example.myjavalab.reactor.mono.internal.SomeBusinessLogic;
import edu.example.myjavalab.reactor.mono.internal.SomeUtils;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

/**
 * Playing around with {@link Mono} via JUnit tests.
 */
public class TestMono {

  /**
   * GIVEN a producer of a single integer value
   * AND value is multiplied by a factor after being produced
   * WHEN a consumer subscribes to this producer
   * THEN the multiplied value is processed, as expected
   * AND producer completes without errors
   */
  @Test
  public void testMonoWithMap() {

    final int initialValue = 23;
    final int multiplier = 2;

    final SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    Mono.just(initialValue)
        .map(value -> value * multiplier)
        .subscribe(
            processNumber::process,
            processNumber::onError,
            processNumber::onComplete,
            // requesting more than it can give
            subscription -> subscription.request(4));

    ArgumentCaptor<Integer> valueToProcess = ArgumentCaptor.captor();

    verify(processNumber, atMostOnce()).process(valueToProcess.capture());
    verify(processNumber, atMostOnce()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));

    assertEquals(initialValue * multiplier, valueToProcess.getValue());
  }

  /**
   * GIVEN a producer of a single integer value
   * WHEN two consumers subscribe to this producer
   * THEN the value is processed twice
   * AND producer completes without errors
   */
  @Test
  public void testSimpleMonoWithTwoSubscriptions() {

    int input = 3;

    // creating the publisher
    Mono<Integer> publisher = SomeUtils.INSTANCE.createPublisherForValue(input);

    final SomeBusinessLogic<Integer> firstProcessNumber = mock(SomeBusinessLogic.class);

    // subscribing for the first time with one (implicit) subscriber
    publisher.subscribe(firstProcessNumber::process, firstProcessNumber::onError, firstProcessNumber::onComplete);

    ArgumentCaptor<Integer> valueToProcess = ArgumentCaptor.captor();

    verify(firstProcessNumber, atMostOnce()).process(valueToProcess.capture());
    verify(firstProcessNumber, atMostOnce()).onComplete();
    verify(firstProcessNumber, never()).onError(any(Throwable.class));

    assertEquals(input, valueToProcess.getValue());

    // subscribing for the second time to the same publisher, with another (implicit) subscription
    final SomeBusinessLogic<Integer> secondProcessNumber = mock(SomeBusinessLogic.class);

    publisher.subscribe(secondProcessNumber::process, secondProcessNumber::onError, secondProcessNumber::onComplete);

    verify(secondProcessNumber, atMostOnce()).process(valueToProcess.capture());
    verify(secondProcessNumber, atMostOnce()).onComplete();
    verify(secondProcessNumber, never()).onError(any(Throwable.class));

    assertEquals(input, valueToProcess.getValue());
  }

  /**
   * GIVEN an empty producer
   * WHEN a consumer subscribes to this producer
   * THEN no value is processed
   * AND producer completes without errors
   */
  @Test
  public void testEmptyMono() {

    final SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    Consumer<Integer> subscriber = processNumber::process;
    Consumer<Throwable> onError = processNumber::onError;
    Runnable onComplete = processNumber::onComplete;

    SomeUtils.INSTANCE.createPublisherForValue(0)
        .subscribe(subscriber, onError, onComplete);

    verify(processNumber, never()).process(any());
    verify(processNumber, atMostOnce()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));
  }

  /**
   * GIVEN a producer that results in error
   * WHEN a consumer subscribes to this producer
   * THEN no value is processed
   * AND error is processed
   */
  @Test
  public void testMonoOnError() {

    int negativeNumber = -5;

    final SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    Consumer<Integer> subscriber = processNumber::process;
    Consumer<Throwable> onError = processNumber::onError;
    Runnable onComplete = processNumber::onComplete;

    SomeUtils.INSTANCE.createPublisherForValue(negativeNumber)
        .subscribe(subscriber, onError, onComplete);

    ArgumentCaptor<Throwable> expectedError = ArgumentCaptor.captor();

    verify(processNumber, never()).process(any());
    verify(processNumber, never()).onComplete();
    verify(processNumber, atMostOnce()).onError(expectedError.capture());

    assertEquals(ERROR_MESSAGE, expectedError.getValue().getMessage());
  }

  /**
   * GIVEN a producer of a single integer value
   * WHEN it depends on a supplier operation
   * THEN supplier operation is called only when a consumer subscribes to the producer
   */
  @Test
  public void testMonoLaziness() {

    final SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    AtomicBoolean hasBeenExecuted = new AtomicBoolean(false);

    Mono<Integer> producer = Mono.fromSupplier(
        () -> SomeUtils.INSTANCE.giveSumToMe(hasBeenExecuted));

    // up until now, operation was not executed
    assertFalse(hasBeenExecuted.get());

    producer.subscribe(processNumber::process);

    // now operation was executed ...
    assertTrue(hasBeenExecuted.get());

    // ... as expected
    ArgumentCaptor<Integer> valueToProcess = ArgumentCaptor.captor();

    verify(processNumber, atMostOnce()).process(valueToProcess.capture());

    assertTrue(valueToProcess.getValue() > 0);
  }

  /**
   * GIVEN a producer of a single integer value
   * AND it is based on a {@link CompletableFuture}
   * WHEN it depends on a supplier operation
   * THEN supplier operation is called only when a consumer subscribes to the producer
   */
  @Test
  public void testMonoLazinessOnFuture() {

    SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    AtomicBoolean hasBeenExecuted = new AtomicBoolean(false);

    // producer with no supplier: not lazy!
    Mono.fromFuture(SomeUtils.INSTANCE.giveFutureSumToMe(hasBeenExecuted));

    // operation is executed because there is no supplier
    assertTrue(hasBeenExecuted.get());

    // putting back to false
    hasBeenExecuted.set(false);

    // producer with supplier: lazy!
    Mono<Integer> producer = Mono.fromFuture(
        () -> SomeUtils.INSTANCE.giveFutureSumToMe(hasBeenExecuted));

    // operation is not executed because there is a supplier now
    assertFalse(hasBeenExecuted.get());

    // let's subscribe
    producer.subscribe(processNumber::process);

    // operation is now executed because there is a subscription
    assertTrue(hasBeenExecuted.get());

    // however, value is not yet produced: future is not completed
    verify(processNumber, never()).process(any());

    // putting back to false
    hasBeenExecuted.set(false);

    // so, now, we show create the future based publisher, and wait a bit for the value

    // recreating things
    processNumber = mock(SomeBusinessLogic.class);
    producer = Mono.fromFuture(
        () -> SomeUtils.INSTANCE.giveFutureSumToMe(hasBeenExecuted));

    assertFalse(hasBeenExecuted.get());

    producer.subscribe(processNumber::process);

    assertTrue(hasBeenExecuted.get());

    // waiting a bit
    SomeUtils.INSTANCE.aBitOfSleeping(5);

    // we should have now a produced value
    ArgumentCaptor<Integer> valueToProcess = ArgumentCaptor.captor();

    verify(processNumber, atMostOnce()).process(valueToProcess.capture());

    assertTrue(valueToProcess.getValue() > 0);
  }

  /**
   * GIVEN a producer of a single value
   * AND the creation of this producer is a heavyweight operation
   * WHEN deferring its creation
   * THEN producer is only created when there is a subscription to it
   */
  @Test
  public void testDeferMonoCreation() {

    AtomicBoolean hasBeenCreated = new AtomicBoolean(false);
    AtomicBoolean hasBeenExecuted = new AtomicBoolean(false);

    Mono<Integer> producer = Mono.defer(() -> Mono.fromSupplier(() -> {

      hasBeenCreated.set(true);
      return SomeUtils.INSTANCE.giveSumToMe(hasBeenExecuted);
    }));

    assertFalse(hasBeenCreated.get());
    assertFalse(hasBeenExecuted.get());

    SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    producer.subscribe(processNumber::process);

    assertTrue(hasBeenCreated.get());
    assertTrue(hasBeenExecuted.get());
  }
}
