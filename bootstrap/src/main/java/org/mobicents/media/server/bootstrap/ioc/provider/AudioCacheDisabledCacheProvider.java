package org.mobicents.media.server.bootstrap.ioc.provider;

import com.google.inject.Provider;
import org.mobicents.media.server.impl.resource.mediaplayer.audio.DisabledAudioCache;

/**
 * Created by achikin on 6/7/16.
 */
public class AudioCacheDisabledCacheProvider implements Provider<DisabledAudioCache> {
    private DisabledAudioCache instance;

    public AudioCacheDisabledCacheProvider() {
        instance = new DisabledAudioCache();
    }
    @Override
    public DisabledAudioCache get() {
        return instance;
    }
}
