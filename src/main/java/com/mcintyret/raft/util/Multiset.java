package com.mcintyret.raft.util;

import java.util.HashMap;
import java.util.Map;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class Multiset<T> {

    private final Map<T, Long> map = new HashMap<>();

    public long add(T t) {
        return map.merge(t, 1L, (a, b) -> a + b);
    }

    public void clear() {
        map.clear();
    }

    public long count(T t) {
        return map.getOrDefault(t, 0L);
    }

}
