package com.ymdt.cache;

import java.lang.reflect.Type;

public interface ICache {

    void put(String key, Object obj);

    void remove(String key);

    <G> G get(String key, Type type);

    String cacheSize();

    void clear();
}
