/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.mts;

import java.util.*;
import org.cougaar.util.CircularQueue;
import org.cougaar.util.log.*;

/**
 * Copied unmodified from topology package because class is not public.
 * Cache for timed topology lookups.
 * <p>
 * This is almost generic enough for "org.cougaar.util".  It
 * needs some additional work to abstract the "isAggregate"
 * fetch/clearing design, etc.
 */
class Cache {

  /**
   * A fetcher is the "backer" for the cache.
   */
  public interface Fetcher {
    Object fetch(Object key);
  }

  /**
   * The cache return a CacheEntry to its clients.
   * <p>
   * @see #fetch(Object,boolean,long)
   */
  public static final class CacheEntry {

    private static final int HAS_VALUE  = 1;
    private static final int IS_PENDING = 2;
    private static final int IS_STALE   = 4;

    private Object value;
    private int state;

    private boolean hasValue;
    private boolean isPending;
    private boolean isStale;

    public Object getValue() {
      return value;
    }
    public boolean hasValue() {
      return ((state & HAS_VALUE) != 0);
    }
    public boolean isStale() {
      return ((state & IS_STALE) != 0);
    }


    private boolean isPending() {
      return ((state & IS_PENDING) != 0);
    }

    private void setValue(Object newValue) {
      value = newValue;
    }
    private void setHasValue(boolean b) {
      if (b) {
        state |= HAS_VALUE;
      } else {
        state &= ~HAS_VALUE;
      }
    }
    private void setIsStale(boolean b) {
      if (b) {
        state |= IS_STALE;
      } else {
        state &= ~IS_STALE;
      }
    }
    private void setIsPending(boolean b) {
      if (b) {
        state |= IS_PENDING;
      } else {
        state &= ~IS_PENDING;
      }
    }

    public String getStateString() {
      return 
        (isPending() ? "pending " : "resolved ")+
        (isStale() ? "stale " : "current ") +
        (hasValue() ? "value" : "unfetched");
    }

    public String toString() {
      String s = getStateString();
      if (hasValue()) s += " " + value;
      return s;
    }
  }

  public Cache(
      Fetcher fetcher,
      String name) {
    this.fetcher = fetcher;
    if (fetcher == null) {
      throw new IllegalArgumentException(
          "Null cache fetcher");
    }
    this.name = (name != null ? name : "Unknown");

    startFetcher();
  }

  private final String name;
  private final Fetcher fetcher;

  private static final Logger log = 
    LoggerFactory.getInstance().createLogger(Cache.class.getName());

  private final Object cacheLock = new Object();
  private final Map aggCache = new HashMap();
  private final Map elemCache = new HashMap();

  private static final long MIN_TIMEOUT = 5; // millis

  /**
   * Mark a cache entry as stale.
   * <p>
   * Stale entries are not automatically re-fetched; the
   * re-fetch takes place when the next client "fetch(..)"
   * is requested.
   */
  public void dirty(Object key) {
    //dirtyLater(key);
    dirtyNow(key);
  }

  /**
   * Fetch an entry from the cache.
   * <p>
   * The "key" identifies the cache entry and allows the Fetcher
   * to calculate the value if it's not in the cache.
   * <p>
   * The "isAggregate" is a hack to distinguish between cache
   * entries that are specific (for minimal dirty-clear) and
   * those that are complex (all lost upon any dirty).
   * <p>
   * The timeout indicates how long the client wishes to wait
   * for the response.  The valid timeout values are:<ul>
   *   <li>
   *   Negative indicates that, if the response is not cached
   *   and non-stale, the client wishes to wait as long as it 
   *   takes for the request to be fetched.
   *   </li><p>
   *   <li>
   *   Zero indicates that this request should quickly check the 
   *   cache, but not force a fetch if the response is not already 
   *   cached.
   *   </li><p>
   *   <li>
   *   Positive indicates the time in milliseconds to wait for the 
   *   request.
   *   </li><p>
   * </ul>
   * <p>
   * Once fetched, a client should check the entry as follows:
   * <pre>
   *    synchronized (entry) {
   *      if (entry.hasValue()) {
   *        Object value = entry.getValue();
   *        if (entry.isStale()) {
   *          // stale value is being fetched now
   *        } else {
   *          // current value
   *        }
   *      } else {
   *        // no value
   *      }
   *    }
   * </pre>
   */
  public CacheEntry fetch(
      Object key,
      boolean isAggregate,
      long timeout) {
    CacheEntry entry;
    synchronized (cacheLock) {
      Map cache = (isAggregate ? aggCache : elemCache);
      entry = (CacheEntry) cache.get(key);
      if (entry == null) {
        entry = new CacheEntry();
        cache.put(key, entry);
      }
    }
    synchronized (entry) {
      if (entry.hasValue() && !entry.isStale()) {
        if (log.isDebugEnabled()) {
          log.debug("HIT "+key+" "+entry.getStateString());
        }
        return entry;
      }

      if (timeout != 0) {
        if (!entry.isPending()) {
          entry.setIsPending(true);
          if (log.isDebugEnabled()) {
            log.debug("MISS <new fetch> "+key+" "+entry.getStateString());
          }
          fetch(key, entry);
        } else {
          if (log.isDebugEnabled()) {
            log.debug(
                "MISS <already pending> "+key+" "+entry.getStateString());
          }
        }
        if (timeout < 0 || timeout > MIN_TIMEOUT) {
          while (entry.isPending()) {
            try {
              if (timeout < 0) {
                entry.wait();
              } else {
                entry.wait(timeout);
                break;
              }
            } catch (InterruptedException ie) {
            }
          }
        }
      }

      return entry;
    }
  }

  private void dirtyNow(Object key) {
    List aggList = new ArrayList();
    synchronized (cacheLock) {
      aggList.addAll(aggCache.values());
      CacheEntry entry = (CacheEntry) elemCache.get(key);
      if (entry != null) {
        aggList.add(entry);
      }
    }
    for (int i = 0, n = aggList.size(); i < n; i++) {
      CacheEntry entry = (CacheEntry) aggList.get(i);
      synchronized (entry) {
        entry.setIsStale(true);
      }
    }
  }

  private void fetch(Object key, CacheEntry entry) {
    //fetchNow(key, entry);
    fetchLater(key, entry);
  }

  private void fetchNow(Object key, CacheEntry entry) {
    if (log.isDebugEnabled()) {
      log.debug("FETCHING "+key);
    }
    Object value = fetcher.fetch(key);
    synchronized (entry) {
      entry.setValue(value);
      entry.setIsStale(false);
      entry.setIsPending(false);
      entry.setHasValue(true);
      if (log.isDebugEnabled()) {
        log.debug("FETCHED "+key+" "+entry.getStateString());
      }
      entry.notifyAll();
    }
  }

  // fetch-later queue

  private Thread fetchThread;
  private final CircularQueue fetchQ = new CircularQueue();

  private static class FetchPair {
    public final Object key;
    public final CacheEntry entry;
    public FetchPair(Object key, CacheEntry entry) {
      this.key = key;
      this.entry = entry;
    }
    public String toString() { 
      return "("+key+", "+entry+")";
    }
  }

  private void fetchLater(Object key, CacheEntry entry) {
    synchronized (fetchQ) {
      fetchQ.add(new FetchPair(key, entry));
      fetchQ.notify();
    }
  }

  private void checkFetch(List toList) {
    synchronized (fetchQ) {
      while (fetchQ.isEmpty()) {
        try {
          fetchQ.wait();
        } catch (InterruptedException ie) {
        }
      }
      toList.addAll(fetchQ);
      fetchQ.clear();
    }
  }

  private final Runnable fetchRunner = 
    new Runnable() {
      public void run() {
        ArrayList tmp = new ArrayList();
        while (true) {
          checkFetch(tmp);
          for (int i = 0, n = tmp.size(); i < n; i++) {
            FetchPair fp = (FetchPair) tmp.get(i);
            fetchNow(fp.key, fp.entry);
          }
          tmp.clear();
        }
      }
    };

  private void startFetcher() {
    fetchThread = new Thread(fetchRunner, name+" cache fetch");
    fetchThread.start();
  }

  // dirty-later would look like the above fetch-later

}
