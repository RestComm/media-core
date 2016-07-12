/*
 * Copyright (C) 2016 TeleStax, Inc..
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package org.mobicents.media.server.impl.rtp;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import org.mobicents.media.server.io.sdp.format.AVProfile;
import org.mobicents.media.server.spi.memory.Frame;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public class JitterBenchmarkIT {

    public JitterBenchmarkIT() {
    }

    @Test public void 
    launchBenchmark() throws Exception {

            Options opt = new OptionsBuilder()
                    // Specify which benchmarks to run. 
                    // You can be more specific if you'd like to run only one benchmark per test.
                    .include("org.mobicents.media.server.impl.rtp.*JitterBenchmarkIT" + ".*")
                    // Set the following options as needed
                    .mode (Mode.AverageTime)
                    .timeUnit(TimeUnit.MICROSECONDS)
                    .warmupTime(TimeValue.seconds(1))
                    .warmupIterations(10)
                    .measurementTime(TimeValue.seconds(1))
                    .measurementIterations(20)
                    .threads(20)
                    .forks(1)
                    .shouldFailOnError(true)
                    .shouldDoGC(true)
                    .build();

            new Runner(opt).run();
        }
    
    private static RtpPacket[] createStream(int size) {
        RtpPacket[] stream = new RtpPacket[size];

        int it = 12345;

        for (int i = 0; i < stream.length; i++) {
            stream[i] = new RtpPacket(172, false);
            stream[i].wrap(false, 8, i + 1, 160 * (i + 1) + it, 123, new byte[160], 0, 160);
        }
        return stream;
    }    
    private final static int WRITER_FREQ = 20;
    private final static RtpPacket[] stream = createStream(100);    
    private final static int period = 20;
    private final static int jitter = 40;
    
    @Benchmark
    public void benchPlainJitterBuffer() throws Exception {      
        final MockWallClock wallClock = new MockWallClock();
        final RtpClock rtpClock = new RtpClock(wallClock);
        final JitterBuffer jitterBuffer = new JitterBuffer(rtpClock, jitter);
        final AtomicInteger writtenPackets = new AtomicInteger(0);
        final Frame[] media = new Frame[stream.length];
        final AtomicInteger readFrames = new AtomicInteger(0);
        Runnable reader = new Runnable() {
            @Override
            public void run() {
                wallClock.tick(20000000L);
                Frame read = jitterBuffer.read(wallClock.getTime());
                if (read != null) {
                    media[readFrames.getAndIncrement()] = read;
                    if (readFrames.get() >= stream.length) {
                        //throw to force task cancelling
                        throw new RuntimeException("Finish reading");
                    }
                }
            }

        };
        Runnable writer = new Runnable() {
            @Override
            public void run() {
                jitterBuffer.write(stream[writtenPackets.getAndIncrement()], AVProfile.audio.find(8));
                if (writtenPackets.get() >= stream.length) {
                    //throw to force task cancelling
                    throw new RuntimeException("Finish writing");
                }
            }

        };

        ScheduledExecutorService newScheduledThreadPool = Executors.newScheduledThreadPool(2);
        ScheduledFuture<?> scheduleAtFixedRate = newScheduledThreadPool.scheduleAtFixedRate(writer, 0, WRITER_FREQ, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> scheduleAtFixedRate1 = newScheduledThreadPool.scheduleAtFixedRate(reader, 0, WRITER_FREQ, TimeUnit.MILLISECONDS);

        try {
            scheduleAtFixedRate.get();
        } catch (Exception e) {

        }
        try {
            scheduleAtFixedRate1.get();
        } catch (Exception e) {

        }
    }

        @Benchmark
    public void benchQuqueJitterBuffer() throws Exception {      
        final MockWallClock wallClock = new MockWallClock();
        final RtpClock rtpClock = new RtpClock(wallClock);
        final JitterBufferQueue jitterBuffer = new JitterBufferQueue(rtpClock, jitter);
        final AtomicInteger writtenPackets = new AtomicInteger(0);
        final Frame[] media = new Frame[stream.length];
        final AtomicInteger readFrames = new AtomicInteger(0);
        Runnable reader = new Runnable() {
            @Override
            public void run() {
                wallClock.tick(20000000L);
                Frame read = jitterBuffer.read(wallClock.getTime());
                if (read != null) {
                    media[readFrames.getAndIncrement()] = read;
                    if (readFrames.get() >= stream.length) {
                        //throw to force task cancelling
                        throw new RuntimeException("Finish reading");
                    }
                }
            }

        };
        Runnable writer = new Runnable() {
            @Override
            public void run() {
                jitterBuffer.write(stream[writtenPackets.getAndIncrement()], AVProfile.audio.find(8));
                if (writtenPackets.get() >= stream.length) {
                    //throw to force task cancelling
                    throw new RuntimeException("Finish writing");
                }
            }

        };

        ScheduledExecutorService newScheduledThreadPool = Executors.newScheduledThreadPool(2);
        ScheduledFuture<?> scheduleAtFixedRate = newScheduledThreadPool.scheduleAtFixedRate(writer, 0, WRITER_FREQ, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> scheduleAtFixedRate1 = newScheduledThreadPool.scheduleAtFixedRate(reader, 0, WRITER_FREQ, TimeUnit.MILLISECONDS);

        try {
            scheduleAtFixedRate.get();
        } catch (Exception e) {

        }
        try {
            scheduleAtFixedRate1.get();
        } catch (Exception e) {

        }
    }
    
}
