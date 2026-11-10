package dev.shirokai.clear;

import java.util.*;

/** Incremental ordered index: no full entity snapshot at the beginning of a job. Server thread only. */
public final class LoadedIndex<T> {
    private final NavigableMap<Long,T> entries = new TreeMap<>();
    private final Map<T,Long> keys = new IdentityHashMap<>();
    private long sequence;
    public void add(T value) { if (!keys.containsKey(value)) { long id = ++sequence; keys.put(value,id); entries.put(id,value); } }
    public void remove(T value) { Long id = keys.remove(value); if (id != null) entries.remove(id); }
    public long boundary() { return sequence; }
    public Map.Entry<Long,T> after(long cursor) { return entries.higherEntry(cursor); }
    public void clear() { entries.clear(); keys.clear(); sequence=0; }
}
