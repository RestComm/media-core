package org.restcomm.media.resource.player.audio;

import com.google.common.cache.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Created by achikin on 5/9/16.
 */
public class CachedRemoteStreamProvider implements RemoteStreamProvider {

    private final static Logger log = Logger.getLogger(CachedRemoteStreamProvider.class);

    private Cache<String, ByteBuf> cache;

    public CachedRemoteStreamProvider(int size) {
        log.info("Create AudioCache with size: " + size + "Mb");
        cache = CacheBuilder.newBuilder().maximumWeight(size * 1024L * 1024L).weigher(new Weigher<String, ByteBuf>() {
            @Override
            public int weigh(String s, ByteBuf byteBuf) {
                return byteBuf.capacity();
            }
        }).removalListener(new RemovalListener<String, ByteBuf>() {
            @Override
            public void onRemoval(RemovalNotification<String, ByteBuf> removalNotification) {
                ByteBuf buf = removalNotification.getValue();
                if (buf != null) {
                    buf.release();
                }
            }
        }).build();
    }

    public InputStream getStream(final URL uri) throws IOException {
        final String key = uri.toString();
        try {
            ByteBuf buf = cache.get(key, new Callable<ByteBuf>() {
                @Override
                public ByteBuf call() throws Exception {
                    byte[] bytes = IOUtils.toByteArray(uri.openStream());
                    return Unpooled.directBuffer(bytes.length).writeBytes(bytes);
                }
            });
            return new ByteBufInputStream(buf.retainedDuplicate(), true);
        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    public void dump() {
        log.info("--- Cache dump ---");
        for (Map.Entry<String, ByteBuf> e : cache.asMap().entrySet()) {
            log.info(e.getKey() + "; " + e.getValue().refCnt());
        }
    }
}
