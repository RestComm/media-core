package org.restcomm.media.bootstrap.ioc;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.resource.player.audio.RemoteStreamProvider;

/**
 * Created by hamsterksu on 3/15/17.
 */
public class RemoteStreamProviderTest {

    @Test
    public void testWildcard1() {

        String[] patterns = new String[]{null, "*", ".*"};
        for (String pattern : patterns) {
            final MediaServerConfiguration config = new MediaServerConfiguration();
            config.getResourcesConfiguration().setPlayerCache(true, 100, pattern);
            final Injector injector = Guice.createInjector(new MgcpModule(), new MediaModule(), new CoreModule(config));

            RemoteStreamProvider obj = injector.getInstance(RemoteStreamProvider.class);

            Assert.assertEquals(org.restcomm.media.resource.player.audio.CachedRemoteStreamProvider.class, obj.getClass());
        }
    }

    @Test
    public void testPattern() {
        final MediaServerConfiguration config = new MediaServerConfiguration();
        config.getResourcesConfiguration().setPlayerCache(true, 100, "http://.*/static/*");
        final Injector injector = Guice.createInjector(new MgcpModule(), new MediaModule(), new CoreModule(config));

        RemoteStreamProvider obj = injector.getInstance(RemoteStreamProvider.class);

        Assert.assertEquals(org.restcomm.media.resource.player.audio.PatternRemoteStreamProvider.class, obj.getClass());
    }

    @Test
    public void testDirect() {

        final MediaServerConfiguration config = new MediaServerConfiguration();
        //not the java pattern, but it's suitable for us
        config.getResourcesConfiguration().setPlayerCache(false, 100, "*");
        final Injector injector = Guice.createInjector(new MgcpModule(), new MediaModule(), new CoreModule(config));

        RemoteStreamProvider obj = injector.getInstance(RemoteStreamProvider.class);

        Assert.assertEquals(org.restcomm.media.resource.player.audio.DirectRemoteStreamProvider.class, obj.getClass());
    }
}
