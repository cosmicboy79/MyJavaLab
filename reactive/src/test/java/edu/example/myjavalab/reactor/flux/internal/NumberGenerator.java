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

package edu.example.myjavalab.reactor.flux.internal;

import java.util.Random;
import java.util.function.Consumer;
import reactor.core.publisher.FluxSink;

/**
 * Creates a subscriber that works on a {@link FluxSink}.
 */
public class NumberGenerator implements Consumer<FluxSink<Integer>> {

  private FluxSink<Integer> fluxSink;
  private final Random random = new Random();

  @Override
  public void accept(FluxSink<Integer> sink) {

    fluxSink = sink;
  }

  /**
   * Generates random numbers.
   *
   * @param max maximum of random numbers to be generated
   */
  public void generate(int max) {

    for (int i = 0; i < max; i++) {

      fluxSink.next(random.nextInt());
    }
  }

  /**
   * Marks the operation as completed.
   */
  public void complete() {

    fluxSink.complete();
  }
}
