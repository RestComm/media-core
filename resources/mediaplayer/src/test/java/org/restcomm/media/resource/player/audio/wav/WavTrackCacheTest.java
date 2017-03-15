package org.restcomm.media.resource.player.audio.wav;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import javax.sound.sampled.UnsupportedAudioFileException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.resource.player.audio.CachedRemoteStreamProvider;
import org.restcomm.media.resource.player.audio.DirectRemoteStreamProvider;
import org.restcomm.media.spi.format.EncodingName;
import org.restcomm.media.spi.format.Format;

/**
 * Created by hamsterksu on 30.06.16.
 */
public class WavTrackCacheTest {

    private long expectedDuration = 3854625000L;
    private Format expectedFormat = new Format(new EncodingName("linear"));

    URLStreamHandler handler;
    URLConnection mockConnection;

    @Before
    public void setUp() throws IOException, UnsupportedAudioFileException {
        mockConnection = mock(URLConnection.class);

        //we need use answer to return new stream each time
        when(mockConnection.getInputStream()).thenAnswer(new Answer<InputStream>() {
            @Override
            public InputStream answer(InvocationOnMock invocationOnMock) throws Throwable {
                return new FileInputStream(new File("src/test/resources/demo-prompt.wav"));
            }
        });

        handler = new URLStreamHandler() {
            @Override
            protected URLConnection openConnection(final URL arg0) throws IOException {
                return mockConnection;
            }
        };
    }

    @Test
    public void testCache() throws IOException, UnsupportedAudioFileException {
        CachedRemoteStreamProvider cache = new CachedRemoteStreamProvider(10);

        URL url1 = new URL(null, "http://test.wav", handler);
        URL url2 = new URL(null, "http://test.wav", handler);

        WavTrackImpl track1 = new WavTrackImpl(url1, cache);
        assertEquals(expectedFormat.getName(), track1.getFormat().getName());
        assertEquals(expectedDuration, track1.getDuration());
        track1.close();

        WavTrackImpl track2 = new WavTrackImpl(url2, cache);
        assertEquals(expectedFormat.getName(), track2.getFormat().getName());
        assertEquals(expectedDuration, track2.getDuration());
        track2.close();

        WavTrackImpl track3 = new WavTrackImpl(url2, cache);
        assertEquals(expectedFormat.getName(), track3.getFormat().getName());
        assertEquals(expectedDuration, track3.getDuration());
        track3.close();

        cache.dump();
        verify(mockConnection).getInputStream();
    }

    @Test
    public void testCacheOverflow() throws IOException, UnsupportedAudioFileException {
        //file size is 61712 bytes
        //1Mb cache contains have 15 full files
        //int cacheSize = 1;
        //double fileSize = 61712d;
        //we have 4 segments in guava cache
        int iteration = 8;//(int) Math.floor(cacheSize * 1024d * 1024d / fileSize) - 1;
        CachedRemoteStreamProvider cache = new CachedRemoteStreamProvider(1);

        for (int j = 0; j < 10; j++) {
            for (int i = 0; i < iteration; i++) {
                URL url = new URL(null, "http://test" + i + ".wav", handler);
                WavTrackImpl track = new WavTrackImpl(url, cache);
                assertEquals(expectedFormat.getName(), track.getFormat().getName());
                assertEquals(expectedDuration, track.getDuration());
                track.close();
            }
        }
        verify(mockConnection, Mockito.times(iteration)).getInputStream();
        for (int i = iteration; i < 2 * iteration; i++) {
            URL url = new URL(null, "http://test" + i + ".wav", handler);
            WavTrackImpl track = new WavTrackImpl(url, cache);
            assertEquals(expectedFormat.getName(), track.getFormat().getName());
            assertEquals(expectedDuration, track.getDuration());
            track.close();
        }
        verify(mockConnection, Mockito.times(2 * iteration)).getInputStream();
    }

    @Test
    public void testNoCache() throws IOException, UnsupportedAudioFileException {
        DirectRemoteStreamProvider noCache = new DirectRemoteStreamProvider();

        URL url1 = new URL(null, "http://test.wav", handler);
        URL url2 = new URL(null, "http://test.wav", handler);

        WavTrackImpl track1 = new WavTrackImpl(url1, noCache);
        assertEquals(expectedFormat.getName(), track1.getFormat().getName());
        assertEquals(expectedDuration, track1.getDuration());
        track1.close();

        WavTrackImpl track2 = new WavTrackImpl(url2, noCache);
        assertEquals(expectedFormat.getName(), track2.getFormat().getName());
        assertEquals(expectedDuration, track2.getDuration());
        track2.close();

        WavTrackImpl track3 = new WavTrackImpl(url2, noCache);
        assertEquals(expectedFormat.getName(), track3.getFormat().getName());
        assertEquals(expectedDuration, track3.getDuration());
        track3.close();

        verify(mockConnection, times(3)).getInputStream();
    }

}
