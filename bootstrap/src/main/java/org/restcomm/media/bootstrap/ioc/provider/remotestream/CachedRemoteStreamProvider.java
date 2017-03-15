package org.restcomm.media.bootstrap.ioc.provider.remotestream;

import org.apache.commons.lang3.StringUtils;
import org.restcomm.media.core.configuration.MediaServerConfiguration;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.restcomm.media.resource.player.audio.*;
import org.restcomm.media.resource.player.audio.DirectRemoteStreamProvider;

/**
 * Created by achikin on 6/3/16.
 */
public class CachedRemoteStreamProvider implements Provider<org.restcomm.media.resource.player.audio.RemoteStreamProvider> {

    private static org.restcomm.media.resource.player.audio.RemoteStreamProvider instance;

    @Inject
    public CachedRemoteStreamProvider(MediaServerConfiguration config) {
        String pattern = config.getResourcesConfiguration().getPlayerCacheUrlPattern();
        org.restcomm.media.resource.player.audio.RemoteStreamProvider cached = new org.restcomm.media.resource.player.audio.CachedRemoteStreamProvider(config.getResourcesConfiguration().getPlayerCacheSize());
        if (StringUtils.isEmpty(pattern) || "*".equals(pattern) || ".*".equals(pattern)) {
            instance = cached;
        } else {
            instance = new PatternRemoteStreamProvider(pattern, new DirectRemoteStreamProvider(), cached);
        }
    }

    @Override
    public org.restcomm.media.resource.player.audio.RemoteStreamProvider get() {
        return instance;
    }
}
