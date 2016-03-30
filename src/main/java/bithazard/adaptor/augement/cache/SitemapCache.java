package bithazard.adaptor.augement.cache;

import bithazard.sitemap.parser.SitemapParser;
import bithazard.sitemap.parser.model.Link;
import bithazard.sitemap.parser.model.Sitemap;
import bithazard.sitemap.parser.model.SitemapEntry;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class SitemapCache {
    private Map<String, Map<String, SitemapEntry>> caches;
    private Map<String, ReadWriteLock> readWriteLocks;

    public SitemapCache() {
        caches = new HashMap<>();
        readWriteLocks = new HashMap<>();
    }

    public SitemapEntry getSitemapEntry(final String sitemapUrl, final String sitemapEntryUrl) {
        Map<String, SitemapEntry> cache = getCacheForUrl(sitemapUrl);
        ReadWriteLock readWriteLock = getReadWriteLockForUrl(sitemapUrl);
        if (cache.size() == 0) {
            Lock writeLock = readWriteLock.writeLock();
            writeLock.lock();
            try {
                if (cache.size() == 0) {
                    SitemapParser sitemapParser = new SitemapParser();
                    Sitemap sitemap = sitemapParser.parseSitemap(sitemapUrl);
                    fillCache(cache, sitemap);
                }
            } finally {
                writeLock.unlock();
            }
        }
        Lock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            return cache.get(sitemapEntryUrl);
        } finally {
            readLock.unlock();
        }
    }

    public Collection<SitemapEntry> getSitemapEntriesAfter(final String sitemapUrl, final Date minDate) {
        Map<String, SitemapEntry> cache = getCacheForUrl(sitemapUrl);
        ReadWriteLock readWriteLock = getReadWriteLockForUrl(sitemapUrl);
        Lock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            SitemapParser sitemapParser = new SitemapParser();
            boolean alreadyAfterMinDate;
            Sitemap sitemap;
            if (cache.size() == 0) {
                alreadyAfterMinDate = false;
                sitemap = sitemapParser.parseSitemap(sitemapUrl);
            } else {
                alreadyAfterMinDate = true;
                sitemap = sitemapParser.parseSitemap(sitemapUrl, minDate);
                if (minDate == null) {
                    cache.clear();
                }
            }
            fillCache(cache, sitemap);
            if (alreadyAfterMinDate || minDate == null) {
                return Collections.unmodifiableCollection(sitemap.getSitemapEntries());
            } else {
                return Collections.unmodifiableCollection(sitemap.getSitemapModifiedAfter(minDate).getSitemapEntries());
            }
        } finally {
            writeLock.unlock();
        }
    }

    private void fillCache(final Map<String, SitemapEntry> cache, final Sitemap sitemap) {
        for (SitemapEntry sitemapEntry : sitemap.getSitemapEntries()) {
            cache.put(sitemapEntry.getLoc(), sitemapEntry);
            if (sitemapEntry.getLinks() != null) {
                for (Link link : sitemapEntry.getLinks()) {
                    cache.put(link.getHref(), sitemapEntry);
                }
            }
        }
    }

    private synchronized Map<String, SitemapEntry> getCacheForUrl(final String sitemapUrl) {
        Map<String, SitemapEntry> cache = this.caches.get(sitemapUrl);
        if (cache == null) {
            cache = new HashMap<>();
            caches.put(sitemapUrl, cache);
        }
        return cache;
    }

    private synchronized ReadWriteLock getReadWriteLockForUrl(final String sitemapUrl) {
        ReadWriteLock readWriteLock = readWriteLocks.get(sitemapUrl);
        if (readWriteLock == null) {
            readWriteLock = new ReentrantReadWriteLock(true);
            readWriteLocks.put(sitemapUrl, readWriteLock);
        }
        return readWriteLock;
    }
}
