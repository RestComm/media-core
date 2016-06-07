package org.mobicents.media.server.impl.resource.mediaplayer.audio;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

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
public class AudioCacheECache implements AudioCache {
    private CacheManager cacheManager;

    public AudioCacheECache(int size) {
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("preConfigured",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder().heap(size, MemoryUnit.MB))
                                .build())
                .build(true);
    }

    public Cache getCache() {
        return cacheManager.getCache("preConfigured", String.class, byte[].class);
    }

    public InputStream getStream(URL uri) throws IOException {
        Cache<URL, byte[]> cache = getCache();

        if (!cache.containsKey(uri)) {
            cache.put(uri, IOUtils.toByteArray(uri.openStream()));
        }
        return new ByteArrayInputStream(cache.get(uri));
    }
}
