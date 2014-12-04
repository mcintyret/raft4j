package com.mcintyret.raft.state;

public enum SimpleSnapshotStrategy implements SnapshotStrategy {
    ALWAYS {
        @Override
        public boolean shouldSnapshot(long logsSinceSnapshot, long bytesSinceSnapshot) {
            return true;
        }
    },
    NEVER {
        @Override
        public boolean shouldSnapshot(long logsSinceSnapshot, long bytesSinceSnapshot) {
            return false;
        }
    };

}
