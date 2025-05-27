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

package edu.example.myjavalab.reactor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;

/**
 * Playing around with {@link Mono} via JUnit tests.
 */
public class TestMono {

  public static final String ERROR_MESSAGE = "Negative number!";

  private static Mono<Integer> createPublisherFromValue(int inputValue) {

    if (inputValue > 0) {

      return Mono.just(inputValue);
    }

    if (inputValue < 0) {

      return Mono.error(new Exception(ERROR_MESSAGE));
    }

    return Mono.empty();
  }

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
   * GIVEN a producer of a single string value
   * AND value is upper-cased after being produced
   * WHEN two consumers subscribe to this producer
   * THEN the upper-cased value is processed twice, as expected
   */
  @Test
  public void testSimpleMonoWithTwoSubscriptions() {

    String inputValue = "something";

    Mono<String> toUpperPublisher = Mono.just(inputValue)
        .map(String::toUpperCase);

    final SomeBusinessLogic<String> processWord = mock(SomeBusinessLogic.class);

    // Publisher or Producer or Observable with 2 subscribers
    // both must show that they received the upper-cased value
    toUpperPublisher.subscribe(processWord::process);
    toUpperPublisher.subscribe(processWord::process);

    ArgumentCaptor<String> valueToProcess = ArgumentCaptor.captor();

    verify(processWord, times(2)).process(valueToProcess.capture());
    assertEquals(inputValue.toUpperCase(), valueToProcess.getValue());
  }

  /**
   * GIVEN a producer of a single integer value
   * WHEN a consumer subscribes to this producer
   * THEN the value is processed, as expected
   * AND producer completes without errors
   */
  @Test
  public void testSimpleMonoProcessing() {

    int positiveNumber = 3;

    final SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    Consumer<Integer> subscriber = processNumber::process;
    Consumer<Throwable> onError = processNumber::onError;
    Runnable onComplete = processNumber::onComplete;

    createPublisherFromValue(positiveNumber).subscribe(subscriber, onError, onComplete);

    ArgumentCaptor<Integer> valueToProcess = ArgumentCaptor.captor();

    verify(processNumber, atMostOnce()).process(valueToProcess.capture());
    verify(processNumber, atMostOnce()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));

    assertEquals(positiveNumber, valueToProcess.getValue());
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

    createPublisherFromValue(0).subscribe(subscriber, onError, onComplete);

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

    createPublisherFromValue(negativeNumber).subscribe(subscriber, onError, onComplete);

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

    AtomicBoolean hasExecuted = new AtomicBoolean(false);

    Mono<Integer> producer = Mono.fromSupplier(() -> giveSumToMe(hasExecuted));

    // up until now, operation was not executed
    assertFalse(hasExecuted.get());

    producer.subscribe(processNumber::process);

    // now operation was executed ...
    assertTrue(hasExecuted.get());

    // ... as expected
    ArgumentCaptor<Integer> valueToProcess = ArgumentCaptor.captor();

    verify(processNumber, atMostOnce()).process(valueToProcess.capture());

    assertTrue(valueToProcess.getValue() > 0);
  }

  private int giveSumToMe(AtomicBoolean hasExecuted) {

    hasExecuted.set(true);
    return Stream.of(6, 2, 9).mapToInt(v -> v).sum();
  }

  /**
   * Private representation for testing purposes of some business logic
   * that must be carried on as a resulting of the Producer execution.
   *
   * @param <T> type of the value to be processed
   */
  private interface SomeBusinessLogic<T> {

    /**
     * Will do "something" with the produced value.
     *
     * @param value input of a type
     */
    void process(T value);

    /**
     * Will do "something" in case something went wrong during producing.
     *
     * @param t error occurred
     */
    void onError(Throwable t);

    /**
     * Will do "something" in case there's nothing more to produce.
     */
    void onComplete();
  }
}
