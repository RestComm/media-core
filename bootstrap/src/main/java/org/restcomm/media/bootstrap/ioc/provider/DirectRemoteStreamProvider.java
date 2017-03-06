package org.restcomm.media.bootstrap.ioc.provider;

import com.google.inject.Provider;

/**
 * Created by achikin on 6/7/16.
 */
public class DirectRemoteStreamProvider implements Provider<org.restcomm.media.resource.player.audio.DirectRemoteStreamProvider> {

    private org.restcomm.media.resource.player.audio.DirectRemoteStreamProvider instance;

    public DirectRemoteStreamProvider() {
        instance = new org.restcomm.media.resource.player.audio.DirectRemoteStreamProvider();
    }

    @Override
    public org.restcomm.media.resource.player.audio.DirectRemoteStreamProvider get() {
        return instance;
    }
}
