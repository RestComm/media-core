package org.restcomm.media.resource.player.audio;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by hamsterksu on 30.06.16.
 */
public class PatternProviderTest {

    RemoteStreamProvider direct;
    RemoteStreamProvider cached;

    private InputStream directIs = new ByteArrayInputStream(new byte[0]);
    private InputStream cachedIs = new ByteArrayInputStream(new byte[0]);

    @Before
    public void setUp() throws IOException, UnsupportedAudioFileException {
        direct = mock(RemoteStreamProvider.class);
        cached = mock(RemoteStreamProvider.class);
        when(direct.getStream(Mockito.<URL>any())).thenReturn(directIs);
        when(cached.getStream(Mockito.<URL>any())).thenReturn(cachedIs);
    }

    @Test
    public void testWildcard() throws IOException {
        PatternRemoteStreamProvider patternProvider = new PatternRemoteStreamProvider(".*", direct, cached);
        Assert.assertEquals(cachedIs, patternProvider.getStream(new URL("http://127.0.0.1/test.wav")));

        patternProvider = new PatternRemoteStreamProvider("http://.*", direct, cached);
        Assert.assertEquals(cachedIs, patternProvider.getStream(new URL("http://127.0.0.1/test.wav")));
    }

    @Test
    public void testPattern() throws IOException, UnsupportedAudioFileException {
        PatternRemoteStreamProvider patternProvider = new PatternRemoteStreamProvider(".*/static/.*", direct, cached);
        Assert.assertEquals(cachedIs, patternProvider.getStream(new URL("http://127.0.0.1/static/test.wav")));
        Assert.assertEquals(directIs, patternProvider.getStream(new URL("http://127.0.0.1/static_test.wav")));
    }

}
