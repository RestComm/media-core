package org.restcomm.media.resource.player.audio;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by achikin on 6/7/16.
 */
public class DirectRemoteStreamProvider implements RemoteStreamProvider {

    private final int connectionTimeout = 2000;

    @Override
    public InputStream getStream(URL uri) throws IOException {
        URLConnection connection = uri.openConnection();
        connection.setConnectTimeout(connectionTimeout);
        connection.setReadTimeout(connectionTimeout);
        return connection.getInputStream();
    }
}
