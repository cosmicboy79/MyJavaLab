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

package edu.example.myjavalab.reactor.flux.hot;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import edu.example.myjavalab.reactor.common.SomeBusinessLogic;
import edu.example.myjavalab.reactor.common.SomeUtils;
import edu.example.myjavalab.reactor.flux.internal.NumberGenerator;
import java.time.Duration;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

/**
 * Playing around with {@link Flux} via JUnit tests.
 * <p>
 * The terms "producer" and "publisher" are used interchangeable in this test class.
 * Likewise, the terms "consumer" and "subscriber".
 * <p>
 * This Test class is located in the <code>hot</code> package, meaning that it contains
 * tests related to "hot publishing", i.e., a publisher who is able, if wanted, to emit data
 * without anyone subscribed to it, but also whose emitted data is shared among different subscribers.
 */
public class TestFlux {

  /**
   * GIVEN a producer associated with a numbers generator
   * AND two consumers already subscribed to it
   * WHEN numbers are created by generator
   * THEN and only then the same data is processed by both consumers
   */
  @Test
  public void testFluxSinkOfSharedData() {

    // preparation
    final SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    final int howManyNumbers = 5;

    // producer created, associated to a generator
    final NumberGenerator numbers = new NumberGenerator();

    var flux = Flux.create(numbers).share();

    // two implicit subscribers
    flux.subscribe(processNumber::process, processNumber::onError, processNumber::onComplete);
    flux.subscribe(processNumber::process, processNumber::onError, processNumber::onComplete);

    // generator has not created any numbers yet, so nothing has happened
    verify(processNumber, never()).process(anyInt());
    verify(processNumber, never()).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));

    // now numbers will be generated
    numbers.generate(howManyNumbers);
    numbers.complete();

    // ... and now they are processed
    verify(processNumber, times(howManyNumbers * 2)).process(anyInt());
    verify(processNumber, times(2)).onComplete();
    verify(processNumber, never()).onError(any(Throwable.class));
  }

  /**
   * GIVEN a cold producer of random numbers
   * AND as long as there is no subscriber, data is not produced
   * WHEN two consumers subscribe to it
   * THEN they receive two different sets of data
   */
  @Test
  public void testColdPublishing() {

    // preparation
    final Random random = new Random();

    final SomeBusinessLogic<Integer> firstProcessNumber = mock(SomeBusinessLogic.class);
    final SomeBusinessLogic<Integer> secondProcessNumber = mock(SomeBusinessLogic.class);

    final int howManyNumbersToTake = 5;

    // creating cold producer (think of it as Netflix!)
    Flux<Integer> coldFlux = Flux.generate(sink -> sink.next(random.nextInt()))
                                 .cast(Integer.class);

    // no subscribers yet, nothing happened
    verify(firstProcessNumber, never()).process(anyInt());
    verify(secondProcessNumber, never()).process(anyInt());
    verify(firstProcessNumber, never()).onComplete();
    verify(secondProcessNumber, never()).onComplete();
    verify(firstProcessNumber, never()).onError(any(Throwable.class));
    verify(secondProcessNumber, never()).onError(any(Throwable.class));

    // two implicit subscribers
    coldFlux.log("take")
            .take(howManyNumbersToTake)
            .log("sub")
            .subscribe(firstProcessNumber::process, firstProcessNumber::onError, firstProcessNumber::onComplete);
    coldFlux.log("take")
            .take(howManyNumbersToTake)
            .log("sub")
            .subscribe(secondProcessNumber::process, secondProcessNumber::onError, secondProcessNumber::onComplete);

    // verifying
    ArgumentCaptor<Integer> firstCaptor = ArgumentCaptor.captor();
    ArgumentCaptor<Integer> secondCaptor = ArgumentCaptor.captor();

    verify(firstProcessNumber, times(howManyNumbersToTake)).process(firstCaptor.capture());
    verify(secondProcessNumber, times(howManyNumbersToTake)).process(secondCaptor.capture());

    // different data, because it was not shared
    assertNotEquals(firstCaptor.getAllValues(), secondCaptor.getAllValues());
  }

  /**
   * GIVEN a hot producer of random numbers
   * AND data is produced right away, without subscribers
   * WHEN two consumers subscribe to it
   * THEN they receive the same set of data
   */
  @Test
  public void testHotPublishing() {

    // preparation
    final Random random = new Random();

    final SomeBusinessLogic<Integer> firstProcessNumber = mock(SomeBusinessLogic.class);
    final SomeBusinessLogic<Integer> secondProcessNumber = mock(SomeBusinessLogic.class);

    final int howManyNumbersToTake = 5;

    // creating hot producer (think of it as a movie theatre!)
    Flux<Integer> hotFlux = Flux.generate(sink -> sink.next(random.nextInt()))
                                .delayElements(Duration.ofMillis(500))
                                .cast(Integer.class)
                                .replay(howManyNumbersToTake - 2)
                                .refCount(1);

    // two implicit subscribers, one starting before the other
    hotFlux.log("take")
           .take(howManyNumbersToTake)
           .log("sub")
           .subscribe(firstProcessNumber::process, firstProcessNumber::onError, firstProcessNumber::onComplete);

    SomeUtils.INSTANCE.aBitOfSleeping(1);

    hotFlux.log("take")
           .take(howManyNumbersToTake)
           .log("sub")
           .subscribe(secondProcessNumber::process, secondProcessNumber::onError, secondProcessNumber::onComplete);

    SomeUtils.INSTANCE.aBitOfSleeping(2);

    // verifying
    ArgumentCaptor<Integer> firstCaptor = ArgumentCaptor.captor();
    ArgumentCaptor<Integer> secondCaptor = ArgumentCaptor.captor();

    verify(firstProcessNumber, atLeastOnce()).process(firstCaptor.capture());
    verify(secondProcessNumber, atLeastOnce()).process(secondCaptor.capture());

    // data was shared
    secondCaptor.getAllValues().forEach(value -> assertTrue(firstCaptor.getAllValues().contains(value)));
  }
}
