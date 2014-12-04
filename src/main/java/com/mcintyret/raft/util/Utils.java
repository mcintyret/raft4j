package com.mcintyret.raft.util;

import java.util.List;

public class Utils {

    public static <T> T getLast(List<T> list) {
        return list.get(list.size() - 1);
    }

}
