package com.mcintyret.raft.state;

public class LogCountingSnapshotStrategy implements SnapshotStrategy {

    private final long n;

    public LogCountingSnapshotStrategy(long n) {
        this.n = n;
    }

    @Override
    public boolean shouldSnapshot(long logsSinceSnapshot, long bytesSinceSnapshot) {
        return logsSinceSnapshot > 0 && logsSinceSnapshot % n == 0;
    }
}
