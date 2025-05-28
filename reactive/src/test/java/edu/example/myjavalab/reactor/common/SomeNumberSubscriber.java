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

package edu.example.myjavalab.reactor.common;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Represents an explicit subscriber (or consumer) able to handle business logic on numbers.
 */
public class SomeNumberSubscriber implements Subscriber<Integer> {

  private final SomeBusinessLogic<Integer> someBusinessLogic;
  private Subscription subscription;

  public SomeNumberSubscriber(SomeBusinessLogic<Integer> someBusinessLogic) {

    this.someBusinessLogic = someBusinessLogic;
  }

  @Override
  public void onSubscribe(Subscription subscription) {

    this.subscription = subscription;
  }

  @Override
  public void onNext(Integer t) {

    someBusinessLogic.process(t);
  }

  @Override
  public void onError(Throwable throwable) {

    someBusinessLogic.onError(throwable);
  }

  @Override
  public void onComplete() {

    someBusinessLogic.onComplete();
  }

  /**
   * @return {@link Subscription}
   */
  public Subscription getSubscription() {

    return subscription;
  }
}
