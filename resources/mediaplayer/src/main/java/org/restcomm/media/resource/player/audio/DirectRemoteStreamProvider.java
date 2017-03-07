package org.restcomm.media.resource.player.audio;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Created by achikin on 6/7/16.
 */
public class DirectRemoteStreamProvider implements RemoteStreamProvider {

    @Override
    public InputStream getStream(URL uri) throws IOException {
        return uri.openStream();
    }
}
