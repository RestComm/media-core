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
package org.mobicents.media.server.impl.resource.mediaplayer.audio.wav;

/**
 *
 * @author apollo
 */

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.mobicents.media.server.spi.memory.Frame;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

@State(Scope.Thread)
public class WavTrackBenchmarkTest {
    
    @Param({"128", "512", "1024"})
    private int buffSizes;

    private static final int NUM_BYTES = 128;
    private static Logger logger = Logger.getLogger(WavTrackBenchmarkTest.class);
    private static URL url = WavTrackBenchmarkTest.class.getClassLoader().getResource("welcome.wav");
    private ReadableByteChannel rbc;
    
    public WavTrackBenchmarkTest() {
        
    }

    @Ignore
    public void launchBechmark() throws Exception {

            Options opt = new OptionsBuilder()
                    // Specify which benchmarks to run. 
                    // You can be more specific if you'd like to run only one benchmark per test.
                    .include(this.getClass().getName() + ".*")
                    // Set the following options as needed
                    .mode (Mode.AverageTime)
                    .timeUnit(TimeUnit.MICROSECONDS)
                    .warmupTime(TimeValue.seconds(1))
                    .warmupIterations(5)//10
                    .measurementTime(TimeValue.seconds(1))
                    .measurementIterations(5)
                    .threads(5)
                    .forks(1)
                    .build();

            new Runner(opt).run();
        }
    
    @Benchmark
    public void benchNioWavTrack() throws Exception {      
        WavTrackImpl wavTrackImpl = new WavTrackImpl(url);
        boolean isEOMReached = false;
        while (!isEOMReached) {
            Frame process = wavTrackImpl.process(0);
            isEOMReached = process.isEOM();
        }
        wavTrackImpl.close();
        
    }

    @Benchmark
    public void benchWavTrack() throws Exception {      
        WavTrack wavTrack = new WavTrack(url);
        boolean isEOMReached = false;
        while (!isEOMReached) {
            Frame process = wavTrack.process(0);
            isEOMReached = process.isEOM();
        }
        wavTrack.close();
    }
    
    
    @Benchmark
    public void readDirectBuffer() throws Exception {
        logger.info("Speed test with NIO direct buffers");
        readNIOTest(buffSizes, true);
    }
    
    @Benchmark
    public void readHeapBuffer() throws Exception {
        logger.info("Speed test with NIO heap buffers");
        readNIOTest(buffSizes, false);
    }
    
    @Benchmark
    public void readInputStream() throws Exception {
            logger.info("Speed test with input stream");
            readStreamTest(url.openStream());
    }
    
    
//    @Benchmark
//    @BenchmarkMode(Mode.Throughput)
//    @OutputTimeUnit(TimeUnit.SECONDS)
//    public void measureThroughput() throws InterruptedException {
//        TimeUnit.MILLISECONDS.sleep(100);
//    }

    
    private void readNIOTest(int bufferSize, boolean direct) throws Exception {
            
        try {
            ByteBuffer buf = direct ? ByteBuffer.allocateDirect(bufferSize) : ByteBuffer.allocate(bufferSize);
            rbc = Channels.newChannel(url.openStream());

            buf.clear();
            rbc.read(buf);
            buf.flip();

            int bytesLeft = NUM_BYTES;
            while (bytesLeft > 0) {
                if (buf.remaining() < 4) {
                        buf.compact();
                        rbc.read(buf);
                        buf.flip();
                }

                bytesLeft--;
            }
            rbc.close();
        }
        finally {
            rbc.close();
        }
   }
    
    private void readStreamTest(InputStream inStream) throws Exception {

        int bytesLeft = NUM_BYTES;
        try {
            while (inStream.available() > 0 && bytesLeft > 0) {
                int len = inStream.read();
                bytesLeft--;
                if (len == -1) throw new Exception();
            }
        } finally{
            inStream.close();
        }

    }
}
