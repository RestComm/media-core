package org.mobicents.media.server.impl.resource.mediaplayer.audio.wav;

import java.net.URL;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.server.spi.memory.Frame;

public class NioWavTrackImplTest {

    public NioWavTrackImplTest() {
    }

    @Test
    public void testDuration() throws Exception {
        URL url = WavTrackImplTest.class.getClassLoader().getResource(
                "org/mobicents/media/server/impl/resource/mediaplayer/audio/wav/wavSample.wav");

        NioWavTrackImpl track = new NioWavTrackImpl(url);
        assertEquals(2742902494L, track.getDuration());
        boolean isEOMReached = false;
        while (!isEOMReached) {
            Frame process = track.process(0);
            isEOMReached = process.isEOM();
        }
        track.close();
    }

}
