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

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

/**
 * Class to test {@link Object#wait()} and  {@link Object#notify()}
 * as well as the synchronized keyword.
 */
public class WaitNotifyExercise {
  
  public static void main(String[] args) throws InterruptedException, ExecutionException {

    System.out.println("Starting " + Thread.currentThread());

    ExecutorService executorService = createExecutorService();

    SomeComponentDoingStuff stuff = new SomeComponentDoingStuff();

    Future<Boolean> f1 = executorService.submit(stuff::doSomethingAndWait);
    Future<Boolean> f2 = executorService.submit(stuff::doSomethingAndNotify);

    Boolean r1 = f1.get();
    Boolean r2 = f2.get();

    if (!r1) {
      System.out.println("Something went wrong with the first thread");
    }

    if (!r2) {
      System.out.println("Something went wrong with the second thread");
    }

    executorService.shutdown();

    System.out.println("Finishing " + Thread.currentThread());
  }

  /**
   * @return Pool of named Threads.
   */
  private static ExecutorService createExecutorService() {

    return Executors.newFixedThreadPool(2, new ThreadFactory() {

      private int count = 0;

      @Override
      public Thread newThread(Runnable r) {

        return new Thread(r, "Thread-" + ++count);
      }
    });
  }

  /**
   * Helper class that simulates some operations.
   */
  private static class SomeComponentDoingStuff {

    private static final int SECONDS = 10;

    // because of synchronized in both methods, whichever Thread executes
    // this operation blocks the other operation for other Threads as well
    // as if there was one single lock being used by both methods
    synchronized boolean doSomethingAndWait() {

      System.out.println(Thread.currentThread() + " has to do something and then wait!");

      try {

        System.out.println(Thread.currentThread() + " doing something for " + SECONDS + " seconds");
        Thread.sleep(Duration.ofSeconds(SECONDS));

        System.out.println(Thread.currentThread() + " waiting a bit now...");
        wait();
      }
      catch (InterruptedException e) {
        System.out.println(Thread.currentThread() + " is interrupted!");
        return false;
      }

      System.out.println(Thread.currentThread() + " woke up!");
      return true;
    }

    synchronized boolean doSomethingAndNotify() {

      System.out.println(
          Thread.currentThread() + " has to do something and then notify others about it!");

      try {

        System.out.println(Thread.currentThread() + " doing something for " + SECONDS + " seconds");
        Thread.sleep(Duration.ofSeconds(SECONDS));
      }
      catch (InterruptedException e) {
        System.out.println(Thread.currentThread() + " is interrupted!");
        return false;
      }

      System.out.println(Thread.currentThread() + " is notifying now...");
      notify();

      return true;
    }
  }
}
