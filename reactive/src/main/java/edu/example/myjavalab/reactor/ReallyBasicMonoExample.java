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

import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

/**
 * As the name implies, basic example with {@link Mono}.
 * Playing around with this basic Publisher, and giving
 * Subscriber and Subscription to it, in a functional way (lambdas).
 */
public class ReallyBasicMonoExample {

  private static final Logger log = LoggerFactory.getLogger(ReallyBasicMonoExample.class);

  public static void main(String[] args) {

    example1();

    example2();

    example3();
  }

  private static void example1() {

    log.info("--- EXAMPLE 1 ---");

    // it will print out that it received value multiplied via logger
    // and then that it is completed
    Mono.just(23)
        .map(value -> value * 2)
        .subscribe(
            value -> log.info("Received {}", value),
            error -> log.error("Something bad has happened!"),
            () -> log.info("Processing is completed!"),
            // requesting more than it can give
            subscription -> subscription.request(4));
  }

  private static void example2() {

    log.info("--- EXAMPLE 2 ---");

    Mono<String> toUpperPublisher = Mono.just("something")
        .map(String::toUpperCase);

    // Publisher or Producer or Observable with 2 subscribers
    // both will show that they received the upper cased value
    toUpperPublisher.subscribe(value -> log.info("Received at first subscriber: {}", value));
    toUpperPublisher.subscribe(value -> log.info("Received at second subscriber as well: {}", value));
  }

  private static void example3() {

    log.info("--- EXAMPLE 3 ---");

    Consumer<Integer> subscriber = value -> log.info("Received: {}", value);
    Consumer<Throwable> onError = error -> log.error("Error received: {}", error.getMessage());
    Runnable onComplete = () -> log.info("Completed!");

    // it will print the number and that it is completed
    createPublisherFromValue(3).subscribe(subscriber, onError, onComplete);
    // it will only print that it is completed
    createPublisherFromValue(0).subscribe(subscriber, onError, onComplete);
    // in this case, it will only print the error
    createPublisherFromValue(-5).subscribe(subscriber, onError, onComplete);
  }

  private static Mono<Integer> createPublisherFromValue(int inputValue) {

    if (inputValue > 0) {

      return Mono.just(inputValue);
    }

    if (inputValue < 0) {

      return Mono.error(new Exception("Negative number!"));
    }

    return Mono.empty();
  }
}
