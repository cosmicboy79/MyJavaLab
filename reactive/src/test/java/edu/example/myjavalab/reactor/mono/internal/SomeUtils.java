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

package edu.example.myjavalab.reactor.mono.internal;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import reactor.core.publisher.Mono;

/**
 * Singleton with utility operations used by test purposes.
 */
public enum SomeUtils {

  INSTANCE;

  public static final String ERROR_MESSAGE = "Negative number!";

  /**
   * Creates a Publisher (or Producer...) based on the input value.
   *
   * @param inputValue input value to use during creation
   * @return Producer, or Publisher, or Observable, or simply put {@link Mono}
   */
  public Mono<Integer> createPublisherForValue(int inputValue) {

    if (inputValue > 0) {

      return Mono.just(inputValue);
    }

    if (inputValue < 0) {

      return Mono.error(new Exception(ERROR_MESSAGE));
    }

    return Mono.empty();
  }

  /**
   * Sums-up a list of number via {@link CompletableFuture}.
   *
   * @param hasBeenExecuted to determine whether the operation was executed or not
   * @return {@link CompletableFuture}
   */
  public CompletableFuture<Integer> giveFutureSumToMe(AtomicBoolean hasBeenExecuted) {

    hasBeenExecuted.set(true);

    return CompletableFuture.supplyAsync(() -> {
      // simulating some heavy processing
      aBitOfSleeping(3);
      return Stream.of(6, 2, 9).mapToInt(v -> v).sum();
    });
  }

  /**
   * Sleeps for a number of seconds.
   *
   * @param seconds number of seconds
   */
  public void aBitOfSleeping(int seconds) {

    try {
      Thread.sleep(Duration.ofSeconds(seconds));
    }
    catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sums-up a list of numbers.
   *
   * @param hasBeenExecuted to determine whether the operation was executed or not
   * @return Sum-up number
   */
  public int giveSumToMe(AtomicBoolean hasBeenExecuted) {

    hasBeenExecuted.set(true);
    return Stream.of(6, 2, 9).mapToInt(v -> v).sum();
  }
}
