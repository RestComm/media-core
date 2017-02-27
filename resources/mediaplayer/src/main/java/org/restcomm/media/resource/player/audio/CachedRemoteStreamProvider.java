package org.restcomm.media.resource.player.audio;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by achikin on 5/9/16.
 */
public class CachedRemoteStreamProvider implements RemoteStreamProvider {

    private final static Logger log = Logger.getLogger(CachedRemoteStreamProvider.class);

    private CacheManager cacheManager;

    private ConcurrentHashMap<String, ByteStreamDownloader> inProgress = new ConcurrentHashMap<>();

    public CachedRemoteStreamProvider(int size) {
        log.info("Create AudioCache with size: " + size + "Mb");
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                .withCache("preConfigured",
                        CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, byte[].class,
                                ResourcePoolsBuilder.newResourcePoolsBuilder().offheap(size, MemoryUnit.MB))
                                .build())
                .build(true);
    }

    private Cache<String, byte[]> getCache() {
        return cacheManager.getCache("preConfigured", String.class, byte[].class);
    }

    public InputStream getStream(URL uri) throws IOException {
        String key = uri.toString();
        Cache<String, byte[]> cache = getCache();

        byte[] stream = cache.get(key);
        if (stream == null) {
            stream = download(cache, uri);
        }

        return new ByteArrayInputStream(stream);
    }

    private byte[] download(Cache<String, byte[]> cache, final URL uri) throws IOException {
        String key = uri.toString();
        ByteStreamDownloader stream = inProgress.get(key);
        if (stream == null) {
            stream = new ByteStreamDownloader();
            ByteStreamDownloader prev = inProgress.putIfAbsent(key, stream);
            if (prev == null) {
                //check bytes in cache again too, maybe it's already added
                byte[] bytes = cache.get(key);
                if (bytes != null) {
                    return bytes;
                }
            } else {
                stream = prev;
            }
        }
        try {
            byte[] bytes = stream.download(uri);
            if (bytes != null) {
                cache.putIfAbsent(key, bytes);
            } else {
                bytes = cache.get(key);
            }
            if (bytes == null) {
                throw new IOException("No data for " + uri);
            }
            return bytes;
        } finally {
            inProgress.remove(key);
        }
    }

    private static class ByteStreamDownloader {

        private Lock lock = new ReentrantLock();

        volatile boolean downloaded;

        public byte[] download(final URL uri) throws IOException {
            if (downloaded) {
                return null;
            }
            lock.lock();
            try {
                //need to check twice
                if (downloaded) {
                    return null;
                }
                byte[] bytes = IOUtils.toByteArray(uri.openStream());
                downloaded = bytes != null;
                return bytes;
            } finally {
                lock.unlock();
            }
        }
    }
}
