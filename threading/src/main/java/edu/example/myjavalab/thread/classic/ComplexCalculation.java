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

package edu.example.myjavalab.thread.classic;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

/**
 * Class to test {@link Thread#join()}.
 */
public class ComplexCalculation {

  /**
   * Calculate the power of numbers based on the given list of {@link Input}, and sums them up. The
   * power calculation is carried on in different threads, who are joined into the main thread for
   * the summing up.
   *
   * @param inputs List of {@link Input}
   * @return Sum of all powers of numbers
   */
  public BigInteger calculateResult(Input... inputs) {

    List<PowerCalculatingThread> allCreatedThreads =
        Arrays.stream(inputs)
            .map(input -> new PowerCalculatingThread(input.base(), input.power()))
            .toList();

    allCreatedThreads.forEach(Thread::start);

    allCreatedThreads.forEach(thread -> {

      try {

        thread.join();
      }
      catch (InterruptedException e) {

        throw new RuntimeException(e);
      }
    });

    return allCreatedThreads.stream()
        .map(PowerCalculatingThread::getResult)
        .reduce(BigInteger.ZERO, BigInteger::add);
  }

  record Input(BigInteger base, BigInteger power) {

    // nothing to add
  }

  private static class PowerCalculatingThread extends Thread {

    private final BigInteger base;
    private final BigInteger power;
    private BigInteger result = BigInteger.ONE;

    public PowerCalculatingThread(BigInteger base, BigInteger power) {

      this.base = base;
      this.power = power;

      setName(base + "-to-" + power);
    }

    @Override
    public void run() {

      for (BigInteger i = BigInteger.ZERO; i.compareTo(power) != 0; i = i.add(BigInteger.ONE)) {

        result = result.multiply(base);
      }
    }

    BigInteger getResult() {

      return result;
    }
  }
}
