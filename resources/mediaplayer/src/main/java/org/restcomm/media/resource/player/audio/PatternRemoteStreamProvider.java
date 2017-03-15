package org.restcomm.media.resource.player.audio;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * Created by gdubina on 15/03/17.
 */
public class PatternRemoteStreamProvider implements RemoteStreamProvider {

    private final Pattern urlPattern;
    private final RemoteStreamProvider direct;
    private final RemoteStreamProvider cached;

    public PatternRemoteStreamProvider(String urlRegexp, RemoteStreamProvider direct, RemoteStreamProvider cached) {
        this.urlPattern = Pattern.compile(urlRegexp);
        this.direct = direct;
        this.cached = cached;
    }

    @Override
    public InputStream getStream(URL uri) throws IOException {
        if (urlPattern.matcher(uri.toString()).matches()) {
            return cached.getStream(uri);
        }
        return direct.getStream(uri);
    }
}
