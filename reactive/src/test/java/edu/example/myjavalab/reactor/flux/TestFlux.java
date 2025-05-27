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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atMostOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import edu.example.myjavalab.reactor.mono.internal.SomeBusinessLogic;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Flux;

/**
 * Playing around with {@link Flux} via JUnit tests.
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

    List<Integer> numbers = List.of(1, 2, 3, 4);
    int multiplier = 2;

    final SomeBusinessLogic<Integer> processNumber = mock(SomeBusinessLogic.class);

    Flux.fromIterable(numbers)
        .map(value -> value * multiplier)
        .subscribe(
            processNumber::process,
            processNumber::onError,
            processNumber::onComplete,
            // requesting more than it can give
            subscription -> subscription.request(numbers.size() + 10));

    ArgumentCaptor<Integer> valuesToProcess = ArgumentCaptor.captor();

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

    List<String> strings = List.of("something", "another");

    Flux<String> toUpperPublisher = Flux.fromIterable(strings)
        .map(String::toUpperCase);

    final SomeBusinessLogic<String> processWord = mock(SomeBusinessLogic.class);

    // Publisher or Producer or Observable with 2 subscribers
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
   * GIVEN a producer of string values on a stream
   * WHEN one consumer subscribes and uses these values
   * THEN second consumer won't be able to use the same stream
   */
  @Test
  public void testFluxWithMultipleSubscriptionsOnStream() {

    List<String> strings = List.of("something", "another");

    Flux<String> publisher = Flux.fromStream(strings.stream());

    SomeBusinessLogic<String> firstProcessWord = mock(SomeBusinessLogic.class);

    // using one (implicit) subscriber
    publisher.subscribe(firstProcessWord::process);

    verify(firstProcessWord, times(strings.size())).process(anyString());
    verify(firstProcessWord, atMostOnce()).onComplete();
    verify(firstProcessWord, never()).onError(any(Throwable.class));

    SomeBusinessLogic<String> secondProcessWord = mock(SomeBusinessLogic.class);

    // using another (implicit) subscriber
    publisher.subscribe(secondProcessWord::process);

    // given that stream was already used by first subscriber...
    verify(secondProcessWord, never()).process(anyString());
    verify(secondProcessWord, never()).onComplete();
    // it receives an error because Java throws an exception about the closed stream
    verify(secondProcessWord, atMostOnce()).onError(any(Throwable.class));
  }
}
