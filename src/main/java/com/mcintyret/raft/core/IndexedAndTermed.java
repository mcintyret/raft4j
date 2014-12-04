package com.mcintyret.raft.core;

public interface IndexedAndTermed {

    long getIndex();

    long getTerm();

}
