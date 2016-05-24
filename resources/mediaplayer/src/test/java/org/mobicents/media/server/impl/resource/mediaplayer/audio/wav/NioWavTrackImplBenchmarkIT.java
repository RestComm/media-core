package org.mobicents.media.server.impl.resource.mediaplayer.audio.wav;

import java.net.URL;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.mobicents.media.server.spi.memory.Frame;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

public class NioWavTrackImplBenchmarkIT {

    private static URL url = NioWavTrackImplBenchmarkIT.class.getClassLoader().getResource(
                "org/mobicents/media/server/impl/resource/mediaplayer/audio/wav/wavSample.wav");
    
    public NioWavTrackImplBenchmarkIT() {
    }

    @Test public void 
    launchBenchmark() throws Exception {

            Options opt = new OptionsBuilder()
                    // Specify which benchmarks to run. 
                    // You can be more specific if you'd like to run only one benchmark per test.
                    .include("org.mobicents.media.server.impl.resource.mediaplayer.audio.wav.*NioWavTrackImplBenchmarkIT" + ".*")
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
    
    @Benchmark
    public void benchPlainWavTrack() throws Exception {      
        WavTrackImpl wavTrackImpl = new WavTrackImpl(url);
        boolean isEOMReached = false;
        while (!isEOMReached) {
            Frame process = wavTrackImpl.process(0);
            isEOMReached = process.isEOM();
        }
        wavTrackImpl.close();
        
    }

        @Benchmark
    public void benchNioWavTrack() throws Exception {      
        NioWavTrackImpl nioWavTrackImpl = new NioWavTrackImpl(url);
        boolean isEOMReached = false;
        while (!isEOMReached) {
            Frame process = nioWavTrackImpl.process(0);
            isEOMReached = process.isEOM();
        }
        nioWavTrackImpl.close();
    }
    
}
