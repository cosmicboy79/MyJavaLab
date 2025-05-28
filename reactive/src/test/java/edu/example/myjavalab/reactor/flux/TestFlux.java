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

package edu.example.myjavalab.reactor.flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import edu.example.myjavalab.reactor.common.SomeNumberSubscriber;
import edu.example.myjavalab.reactor.flux.internal.NumberGenerator;
import edu.example.myjavalab.reactor.common.SomeBusinessLogic;
import edu.example.myjavalab.reactor.common.SomeUtils;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

/**
 * Playing around with {@link Flux} via JUnit tests.
 * <p>
 * The terms "producer" and "publisher" are used interchangeable in this test class.
 * Likewise, the terms "consumer" and "subscriber".
 */
public class TestFlux {

  /**
   * GIVEN a producer of a list of numbers
   * AND each value is multiplied by a factor after being produced
   * WHEN a consumer subscribes to this producer
   * THEN the multiplied values are processed, as expected
   * AND producer completes without errors
   */
  @Test
  public void testFluxWithMap() {

    // preparation
    List<Integer> numbers = List.of(1, 2, 3, 4);
    int multiplier = 2;

    final SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    // producer with implicit consumer
    Flux.fromIterable(numbers)
        .log("map")
        .map(value -> value * multiplier)
        .log("sub")
        .subscribe(
            processNumber::process,
            processNumber::onError,
            processNumber::onComplete,
            // requesting more than it can give
            subscription -> subscription.request(numbers.size() + 10));

    ArgumentCaptor<Integer> valuesToProcess = ArgumentCaptor.captor();

    // expected processing
    verify(processNumber, times(numbers.size())).process(valuesToProcess.capture());
    verify(processNumber, atMostOnce()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));

    for (int i = 0; i < numbers.size(); i++) {

      assertEquals(numbers.get(i) * multiplier, valuesToProcess.getAllValues().get(i));
    }
  }

  /**
   * GIVEN a producer of string values
   * AND each value is upper-cased after being produced
   * WHEN two consumers subscribe to this producer
   * THEN the upper-cased values are processed twice, as expected
   */
  @Test
  public void testSimpleFluxWithTwoSubscriptions() {

    // preparation
    List<String> strings = List.of("something", "another");

    final SomeBusinessLogic<String> processWord = mock(SomeBusinessLogic.class);

    // producer
    Flux<String> toUpperPublisher = Flux.fromIterable(strings)
        .map(String::toUpperCase).log();

    // Publisher or Producer or Observable with 2 implicit subscribers
    // both must show that they received the upper-cased value
    toUpperPublisher.subscribe(processWord::process);
    toUpperPublisher.subscribe(processWord::process);

    ArgumentCaptor<String> valuesToProcess = ArgumentCaptor.captor();

    verify(processWord, times(strings.size() * 2)).process(valuesToProcess.capture());

    for (int i = 0; i < strings.size(); i++) {

      assertEquals(strings.get(i).toUpperCase(), valuesToProcess.getAllValues().get(i));
    }
  }

  /**
   * GIVEN a producer of a list of numbers
   * WHEN a consumer subscribes to this producer
   * AND consumer request for data multiple times
   * THEN producer will give data back until it is completed
   */
  @Test
  public void testSimpleFluxWithOneSubscription() {

    // preparation
    List<Integer> numbers = List.of(9, 8, 7, 6, 5);
    List<ArgumentCaptor<Integer>> captors = List.of(ArgumentCaptor.captor(), ArgumentCaptor.captor(), ArgumentCaptor.captor());

    int howMany = 2;

    final SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    // creating the producer and subscribing to it with an explicit consumer
    Flux<Integer> toUpperPublisher = Flux.fromIterable(numbers).log();

    SomeNumberSubscriber subscriber = new SomeNumberSubscriber(processNumber);
    toUpperPublisher.subscribe(subscriber);

    // subscriber will ask for the first batch of data
    // publisher won't be completed
    subscriber.getSubscription().request(howMany);

    verify(processNumber, times(howMany)).process(captors.getFirst().capture());
    verify(processNumber, never()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));

    assertEquals(numbers.get(0), captors.getFirst().getAllValues().get(0));
    assertEquals(numbers.get(1), captors.getFirst().getAllValues().get(1));

    // subscriber will ask for the second batch of data
    // publisher won't be completed
    subscriber.getSubscription().request(howMany);

    verify(processNumber, times(howMany * 2)).process(captors.get(1).capture());
    verify(processNumber, never()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));

    assertEquals(numbers.get(2), captors.get(1).getAllValues().get(2));
    assertEquals(numbers.get(3), captors.get(1).getAllValues().get(3));

    // subscriber will ask for the last batch of data
    // publisher will give less back and be completed
    subscriber.getSubscription().request(howMany);

    verify(processNumber, times((howMany * 2) + 1)).process(captors.getLast().capture());
    verify(processNumber, atMostOnce()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));

    assertEquals(numbers.get(4), captors.getLast().getAllValues().get(4));
  }

  /**
   * GIVEN a producer of string values on a stream
   * WHEN one consumer subscribes and uses these values
   * THEN second consumer won't be able to use the same stream
   */
  @Test
  public void testFluxWithMultipleSubscriptionsOnStream() {

    // preparation
    List<String> strings = List.of("something", "another");

    SomeBusinessLogic<String> firstProcessWord = mock(SomeBusinessLogic.class);

    // producer
    Flux<String> publisher = Flux.fromStream(strings.stream()).log();

    // using an implicit subscriber
    publisher.subscribe(firstProcessWord::process);

    verify(firstProcessWord, times(strings.size())).process(anyString());
    verify(firstProcessWord, atMostOnce()).onComplete();
    verify(firstProcessWord, never()).onError(any(Throwable.class));

    SomeBusinessLogic<String> secondProcessWord = mock(SomeBusinessLogic.class);

    // using another implicit subscriber
    publisher.subscribe(secondProcessWord::process);

    // given that stream was already used by first subscriber...
    verify(secondProcessWord, never()).process(anyString());
    verify(secondProcessWord, never()).onComplete();
    // it receives an error because Java throws an exception about the closed stream
    verify(secondProcessWord, atMostOnce()).onError(any(Throwable.class));
  }

  /**
   * GIVEN a producer of string values on a supplied stream
   * WHEN one consumer subscribes and uses these values
   * THEN second consumer will also use values because stream is supplied
   */
  @Test
  public void testFluxWithMultipleSubscriptionsOnSuppliedStream() {

    // preparation
    SomeBusinessLogic<String> firstProcessWord = mock(SomeBusinessLogic.class);

    List<String> strings = List.of("something", "another");

    // producer
    Flux<String> publisher = Flux.fromStream(strings::stream).log();

    // using an implicit subscriber
    publisher.subscribe(firstProcessWord::process);

    verify(firstProcessWord, times(strings.size())).process(anyString());
    verify(firstProcessWord, atMostOnce()).onComplete();
    verify(firstProcessWord, never()).onError(any(Throwable.class));

    SomeBusinessLogic<String> secondProcessWord = mock(SomeBusinessLogic.class);

    // using another implicit subscriber
    publisher.subscribe(secondProcessWord::process);

    // given that stream was already used by first subscriber...
    verify(firstProcessWord, times(strings.size())).process(anyString());
    verify(firstProcessWord, atMostOnce()).onComplete();
    verify(firstProcessWord, never()).onError(any(Throwable.class));
  }

  /**
   * GIVEN a producer associated with a numbers generator
   * AND a consumer already subscribed to it
   * WHEN numbers are created by generator
   * THEN and only then they are processed
   */
  @Test
  public void testFluxSinkOfData() {

    // preparation
    final SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    final int howManyNumbers = 5;

    // producer created, associated to a generator and with an implicit consumer subscribed to it
    final NumberGenerator numbers = new NumberGenerator();

    Flux.create(numbers)
        .log("subscription!")
        .subscribe(processNumber::process, processNumber::onError, processNumber::onComplete);

    // generator has not created any numbers yet, so nothing has happened
    verify(processNumber, never()).process(anyInt());
    verify(processNumber, never()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));

    // now numbers will be generated
    numbers.generate(howManyNumbers);
    numbers.complete();

    // ... and now they are processed
    verify(processNumber, times(howManyNumbers)).process(anyInt());
    verify(processNumber, atMostOnce()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));
  }

  /**
   * GIVEN a producer associated with a numbers generator
   * AND a consumer already subscribed to it
   * WHEN numbers are created by generator
   * AND consumer requests for items
   * THEN and only then they are processed
   */
  @Test
  public void testFluxSinkOfDataMultipleRequests() {

    // preparation
    final int howManyNumbers = 5;
    final int howManyToRequest = 2;

    final SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);
    final SomeNumberSubscriber subscriber = new SomeNumberSubscriber(processNumber);

    // producer created, associated to a generator and with an explicit consumer subscribed to it
    final NumberGenerator numbers = new NumberGenerator();

    Flux.create(numbers)
        .log("subscription!")
        .subscribe(subscriber);

    // generator has not created any numbers yet, so nothing has happened
    verify(processNumber, never()).process(anyInt());
    verify(processNumber, never()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));

    // now numbers will be generated
    numbers.generate(howManyNumbers);
    numbers.complete();

    // still nothing happened, because subscriber did not request for it
    verify(processNumber, never()).process(anyInt());
    verify(processNumber, never()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));

    // now, requesting some data
    subscriber.getSubscription().request(howManyToRequest);

    // ... and now they are processed a bit
    verify(processNumber, times(howManyToRequest)).process(anyInt());
    verify(processNumber, never()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));

    // requesting more than it has already provided
    subscriber.getSubscription().request(howManyNumbers + howManyToRequest);

    // ... and now all is processed
    verify(processNumber, times(howManyNumbers)).process(anyInt());
    verify(processNumber, atMostOnce()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));
  }

  /**
   * GIVEN a producer associated with a numbers generator
   * AND a consumer already subscribed to it
   * WHEN numbers are created by generator via many threads
   * THEN numbers created by all threads are processed
   */
  @Test
  public void testFluxThreadSafety() {

    // preparation
    final SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    final int howManyThreads = 3;
    final int howManyNumbers = 5;

    // producer created, associated to a generator and with an implicit consumer subscribed to it
    final NumberGenerator numbers = new NumberGenerator();

    Flux.create(numbers)
        .log("subscription!")
        .subscribe(processNumber::process, processNumber::onError, processNumber::onComplete);

    // generator has not created any numbers yet, so nothing has happened
    verify(processNumber, never()).process(anyInt());
    verify(processNumber, never()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));

    // now numbers will be generated in N threads
    // FluxSink is thread-safe, it can be shared among many threads
    for (int i = 0; i < howManyThreads; i++) {

      Thread.ofPlatform().start(() -> numbers.generate(howManyNumbers));
    }

    // blocking a bit and completing
    SomeUtils.INSTANCE.aBitOfSleeping(2);

    numbers.complete();

    // ... and now all numbers generated by all threads are processed
    verify(processNumber, times(howManyNumbers * howManyThreads)).process(anyInt());
    verify(processNumber, atMostOnce()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));
  }

  /**
   * GIVEN a producer that generates the same value multiple times
   * WHEN a consumer subscribes to it
   * THEN consumer receives the same value, multiple times
   */
  @Test
  public void testFluxGeneration() {

    // preparation
    final SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    final int numberToGenerate = 35;
    final int howManyToTake = 7;

    // create producer with implicit consumer associated to it
    Flux.<Integer>generate(synchronousSink -> synchronousSink.next(numberToGenerate))
        .log("take")
        .take(howManyToTake)
        .log("sub")
        .subscribe(processNumber::process, processNumber::onError, processNumber::onComplete);

    ArgumentCaptor<Integer> valuesToProcess = ArgumentCaptor.captor();

    // check the processing accordingly
    verify(processNumber, times(howManyToTake)).process(valuesToProcess.capture());
    verify(processNumber, atMostOnce()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));

    assertEquals(howManyToTake, valuesToProcess.getAllValues().size());

    valuesToProcess.getAllValues().forEach(capturedValue -> assertEquals(numberToGenerate, capturedValue));
  }

  /**
   * GIVEN a producer that generates the same value multiple times
   * AND has a state accumulator for the number that is generating
   * WHEN a consumer subscribes to it
   * THEN consumer receives the same value, multiple times
   * AND accumulator has the expected value
   */
  @Test
  public void testFluxGenerationWithState() {

    // preparation
    final SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    final int numberToGenerate = 35;
    final int howManyToTake = 7;
    // not that good, but then again I just want to check the internal state in this test
    final AtomicInteger stateAccumulatorChecker = new AtomicInteger(0);

    // create producer with implicit consumer associated to it
    Flux.<Integer, Integer>generate(() -> 0, (accumulator, synchronousSink) -> {
          synchronousSink.next(numberToGenerate);
          accumulator += numberToGenerate ;
          stateAccumulatorChecker.set(accumulator);
          return accumulator;
        })
        .log("take")
        .take(howManyToTake)
        .log("sub")
        .subscribe(processNumber::process, processNumber::onError, processNumber::onComplete);

    ArgumentCaptor<Integer> valuesToProcess = ArgumentCaptor.captor();

    // check the processing accordingly
    verify(processNumber, times(howManyToTake)).process(valuesToProcess.capture());
    verify(processNumber, atMostOnce()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));

    assertEquals(howManyToTake * numberToGenerate, stateAccumulatorChecker.get());

    assertEquals(howManyToTake, valuesToProcess.getAllValues().size());

    valuesToProcess.getAllValues().forEach(capturedValue -> assertEquals(numberToGenerate, capturedValue));
  }
}
