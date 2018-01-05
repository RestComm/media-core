package org.restcomm.media.resource.player.audio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.sizeof.annotations.IgnoreSizeOf;

/**
 * Created by achikin on 5/9/16.
 */
public class CachedRemoteStreamProvider implements RemoteStreamProvider {

    private final static Logger log = LogManager.getLogger(CachedRemoteStreamProvider.class);

    private CacheManager cacheManager;

    private ByteStreamCache.ISizeChangedListener sizeChangedListener;

    public CachedRemoteStreamProvider(int size) {
        log.info("Create AudioCache with size: " + size + "Mb");
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("preConfigured",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(URL.class, ByteStreamCache.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(size, MemoryUnit.MB))
                                .build())
                .build(true);
        sizeChangedListener = new ByteStreamCache.ISizeChangedListener() {
            @Override
            public void onSizeChanged(final URL uri, final ByteStreamCache self) {
                log.debug("onSizeChanged for " + uri);
                getCache().put(uri, self);
            }
        };
    }

    private Cache<URL, ByteStreamCache> getCache() {
        return cacheManager.getCache("preConfigured", URL.class, ByteStreamCache.class);
    }

    public InputStream getStream(URL uri) throws IOException {
        Cache<URL, ByteStreamCache> cache = getCache();

        ByteStreamCache stream = cache.get(uri);
        if (stream == null) {
            stream = new ByteStreamCache();
            ByteStreamCache exists = cache.putIfAbsent(uri, stream);
            if (exists != null) {
                stream = exists;
            }
        }
        return new ByteArrayInputStream(stream.getBytes(uri, sizeChangedListener));
    }

    private static class ByteStreamCache {

        @IgnoreSizeOf
        private Lock lock = new ReentrantLock();

        private volatile byte[] bytes;

        public byte[] getBytes(final URL uri, final ISizeChangedListener listener) throws IOException {
            if (bytes == null) {
                lock.lock();
                try {
                    //need to check twice
                    if (bytes == null) {
                        bytes = IOUtils.toByteArray(uri.openStream());
                        listener.onSizeChanged(uri, this);
                    }
                } finally {
                    lock.unlock();
                }
            }
            return bytes;
        }

        interface ISizeChangedListener {
            void onSizeChanged(URL uri, ByteStreamCache self);
        }
    }
}
