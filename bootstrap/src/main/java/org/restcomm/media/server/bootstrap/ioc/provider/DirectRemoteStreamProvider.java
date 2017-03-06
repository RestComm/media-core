package org.restcomm.media.server.bootstrap.ioc.provider;

import com.google.inject.Provider;

/**
 * Created by achikin on 6/7/16.
 */
public class DirectRemoteStreamProvider implements Provider<org.mobicents.media.server.impl.resource.mediaplayer.audio.DirectRemoteStreamProvider> {

    private org.mobicents.media.server.impl.resource.mediaplayer.audio.DirectRemoteStreamProvider instance;

    public DirectRemoteStreamProvider() {
        instance = new org.mobicents.media.server.impl.resource.mediaplayer.audio.DirectRemoteStreamProvider();
    }

    @Override
    public org.mobicents.media.server.impl.resource.mediaplayer.audio.DirectRemoteStreamProvider get() {
        return instance;
    }
}
