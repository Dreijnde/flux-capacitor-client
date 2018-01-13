package io.fluxcapacitor.common.api;

import lombok.Value;
import lombok.experimental.Wither;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;

@Value
public class Metadata implements Map<String, String> {
    private static Metadata emptyMetadata = new Metadata(emptyMap());

    @Wither
    Map<String, String> entries;

    public static Metadata empty() {
        return emptyMetadata;
    }

    public static Metadata from(String key, String value) {
        return new Metadata(singletonMap(key, value));
    }

    public static Metadata from(Map<String, String> map) {
        return map instanceof Metadata ? (Metadata) map : new Metadata(map);
    }

    public Metadata withEntry(String key, String value) {
        return this.withEntries(singletonMap(key, value));
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return entries.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return entries.containsValue(value);
    }

    @Override
    public String get(Object key) {
        return entries.get(key);
    }

    @Override
    public String put(String key, String value) {
        return entries.put(key, value);
    }

    @Override
    public String remove(Object key) {
        return entries.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        entries.putAll(m);
    }

    @Override
    public void clear() {
        entries.clear();
    }

    @Override
    public Set<String> keySet() {
        return entries.keySet();
    }

    @Override
    public Collection<String> values() {
        return entries.values();
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return entries.entrySet();
    }

}
