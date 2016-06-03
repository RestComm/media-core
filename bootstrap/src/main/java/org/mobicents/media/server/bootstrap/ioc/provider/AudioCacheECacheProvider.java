package org.mobicents.media.server.bootstrap.ioc.provider;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.AudioCacheECache;

/**
 * Created by achikin on 6/3/16.
 */
public class AudioCacheECacheProvider implements Provider<AudioCacheECache> {

    private static AudioCacheECache instance;

    @Inject
    public AudioCacheECacheProvider(MediaServerConfiguration config) {
        instance = new AudioCacheECache(config.getResourcesConfiguration().getPlayerCacheSize());
    }

    @Override
    public AudioCacheECache get() {
        return instance;
    }
}
