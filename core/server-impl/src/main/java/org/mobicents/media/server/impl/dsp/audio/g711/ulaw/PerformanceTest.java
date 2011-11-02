/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.impl.dsp.audio.g711.ulaw;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.mobicents.media.Buffer;
import org.mobicents.media.server.spi.rtp.AVProfile;

/**
 *
 * @author kulikov
 */
public class PerformanceTest  {

    private byte data[] = new byte[160];
    private Decoder decoder = new Decoder();
    private Encoder encoder = new Encoder();
    private ExecutorService executor = Executors.newFixedThreadPool(4);
    private volatile int count = 0;
    private volatile long start;
    private Task[] tasks = new Task[4];

    public PerformanceTest() {
        tasks[0] = new Task();
        tasks[1] = new Task();
        tasks[2] = new Task();
        tasks[3] = new Task();
    }

    private void test2() {
        start = System.currentTimeMillis();
        count = -4;
        executor.execute(tasks[0]);
        executor.execute(tasks[1]);
        executor.execute(tasks[2]);
        executor.execute(tasks[3]);
    }

    private void test() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 2000; i++) {
            Buffer buff = new Buffer();
            buff.setData(data);
            buff.setOffset(0);
            buff.setLength(160);
            buff.setFormat(AVProfile.PCMA);
            decoder.process(buff);
            encoder.process(buff);
        }

        long finish = System.currentTimeMillis();
        System.out.println(finish - start);
    }

    private class Task implements Runnable {

        public void run() {
            for (int i = 0; i < 500; i++) {
                Buffer buff = new Buffer();
                buff.setData(data);
                buff.setOffset(0);
                buff.setLength(160);
                buff.setFormat(AVProfile.PCMA);
                decoder.process(buff);
                encoder.process(buff);
            }
            count++;
            if (count == 0) {
                long finish = System.currentTimeMillis();
                System.out.println(finish - start);
                executor.shutdown();
            }
        }
    }

    public static void main(String[] args) {
        PerformanceTest t = new PerformanceTest();
        t.test();
        t.test2();
    }
}
