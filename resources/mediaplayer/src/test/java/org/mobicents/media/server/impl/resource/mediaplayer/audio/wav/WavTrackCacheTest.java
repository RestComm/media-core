package org.mobicents.media.server.impl.resource.mediaplayer.audio.wav;

import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.CachedRemoteStreamProvider;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.DirectRemoteStreamProvider;
import org.mobicents.media.server.spi.format.EncodingName;
import org.mobicents.media.server.spi.format.Format;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

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

        WavTrackImpl track2 = new WavTrackImpl(url2, cache);
        assertEquals(expectedFormat.getName(), track2.getFormat().getName());
        assertEquals(expectedDuration, track2.getDuration());

        WavTrackImpl track3 = new WavTrackImpl(url2, cache);
        assertEquals(expectedFormat.getName(), track3.getFormat().getName());
        assertEquals(expectedDuration, track3.getDuration());

        verify(mockConnection).getInputStream();
    }

    @Test
    public void testNoCache() throws IOException, UnsupportedAudioFileException {
        DirectRemoteStreamProvider noCache = new DirectRemoteStreamProvider();

        URL url1 = new URL(null, "http://test.wav", handler);
        URL url2 = new URL(null, "http://test.wav", handler);

        WavTrackImpl track1 = new WavTrackImpl(url1, noCache);
        assertEquals(expectedFormat.getName(), track1.getFormat().getName());
        assertEquals(expectedDuration, track1.getDuration());

        WavTrackImpl track2 = new WavTrackImpl(url2, noCache);
        assertEquals(expectedFormat.getName(), track2.getFormat().getName());
        assertEquals(expectedDuration, track2.getDuration());

        WavTrackImpl track3 = new WavTrackImpl(url2, noCache);
        assertEquals(expectedFormat.getName(), track3.getFormat().getName());
        assertEquals(expectedDuration, track3.getDuration());

        verify(mockConnection, times(3)).getInputStream();
    }

}
