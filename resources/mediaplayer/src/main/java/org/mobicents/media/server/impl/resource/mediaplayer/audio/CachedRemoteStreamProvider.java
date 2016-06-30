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

    private Lock lock = new ReentrantLock();

    public CachedRemoteStreamProvider(int size) {
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("preConfigured",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(URL.class, byte[].class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(size, MemoryUnit.MB))
                                .build())
                .build(true);
    }

    public Cache getCache() {
        return cacheManager.getCache("preConfigured", URL.class, byte[].class);
    }

    public InputStream getStream(URL uri) throws IOException {
        Cache<URL, byte[]> cache = getCache();

        if (!cache.containsKey(uri)) {
            lock.lock();
            try {
                //need to check twice
                if (!cache.containsKey(uri)) {
                    cache.put(uri, IOUtils.toByteArray(uri.openStream()));
                }
            } finally {
                lock.unlock();
            }
        }
        return new ByteArrayInputStream(cache.get(uri));
    }
}
