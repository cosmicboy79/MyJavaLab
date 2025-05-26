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

import static org.junit.jupiter.api.Assertions.assertNotEquals;

import edu.example.myjavalab.thread.classic.ComplexCalculation.Input;
import java.math.BigInteger;
import org.junit.jupiter.api.Test;

/**
 * Test class for {@link ComplexCalculation}.
 */
public class TestComplexCalculation {

  @Test
  public void testComplexCalculation() {

    Input input1 = new Input(BigInteger.valueOf(23), BigInteger.valueOf(500));
    Input input2 = new Input(BigInteger.valueOf(70), BigInteger.valueOf(800));

    ComplexCalculation complexCalculation = new ComplexCalculation();

    BigInteger result = complexCalculation.calculateResult(input1, input2);

    assertNotEquals(BigInteger.ZERO, result);
  }
}
