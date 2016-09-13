package org.mobicents.media.server.impl.resource.mediaplayer.audio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.io.IOUtils;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

/**
 * Created by achikin on 5/9/16.
 */
public class CachedRemoteStreamProvider implements RemoteStreamProvider {

    private CacheManager cacheManager;

    public CachedRemoteStreamProvider(int size) {
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("preConfigured",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(URL.class, AudioStreamCache.class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(size, MemoryUnit.MB))
                                .build())
                .build(true);
    }

    private Cache<URL, AudioStreamCache> getCache() {
        return cacheManager.getCache("preConfigured", URL.class, AudioStreamCache.class);
    }

    public InputStream getStream(URL uri) throws IOException {
        Cache<URL, AudioStreamCache> cache = getCache();

        AudioStreamCache stream = cache.get(uri);
        if (stream == null) {
            stream = new AudioStreamCache(uri);
            AudioStreamCache exists = cache.putIfAbsent(uri, stream);
            if (exists != null) {
                stream = exists;
            }
        }
        return new ByteArrayInputStream(stream.getBytes());
    }

    private static class AudioStreamCache {

        private URL uri;

        private Lock lock = new ReentrantLock();

        private volatile byte[] bytes;

        public AudioStreamCache(URL uri) {
            this.uri = uri;
        }

        public byte[] getBytes() throws IOException {
            if (bytes == null) {
                lock.lock();
                try {
                    //need to check twice
                    if (bytes == null) {
                        bytes = IOUtils.toByteArray(uri.openStream());
                    }
                } finally {
                    lock.unlock();
                }
            }
            return bytes;
        }
    }
}
