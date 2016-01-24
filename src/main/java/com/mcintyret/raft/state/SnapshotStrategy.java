package com.mcintyret.raft.state;

public interface SnapshotStrategy {

    boolean shouldSnapshot(long logsSinceSnapshot, long bytesSinceSnapshot);

}
