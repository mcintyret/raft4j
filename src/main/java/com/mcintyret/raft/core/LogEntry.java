package com.mcintyret.raft.core;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class LogEntry {

    private final long term;

    private final long index;

    private final byte[] data;

    public LogEntry(long term, long index, byte[] data) {
        this.term = term;
        this.index = index;
        this.data = data;
    }

    public long getTerm() {
        return term;
    }

    public long getIndex() {
        return index;
    }

    public byte[] getData() {
        return data;
    }
}
