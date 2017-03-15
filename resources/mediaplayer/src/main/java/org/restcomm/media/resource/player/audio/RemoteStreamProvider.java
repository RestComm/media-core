package org.restcomm.media.resource.player.audio;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;

/**
 * Created by achikin on 5/9/16.
 */
public interface RemoteStreamProvider {
    InputStream getStream(URL uri) throws IOException;
}
