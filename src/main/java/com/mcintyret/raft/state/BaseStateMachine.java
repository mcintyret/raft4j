package com.mcintyret.raft.state;

import com.mcintyret.raft.core.LogEntry;
import com.mcintyret.raft.util.Utils;

import java.util.List;

public abstract class BaseStateMachine implements StateMachine {

    private final SnapshotStrategy snapshotStrategy;

    private long bytesSinceSnapshot;

    private Snapshot lastSnapshot;

    private long lastApplied = -1;

    public BaseStateMachine(SnapshotStrategy snapshotStrategy) {
        this.snapshotStrategy = snapshotStrategy;
    }

    protected long getInitialLastApplied() {
        // This works for volatile StateMachines; override to implement a persistent StateMachine by eg loading from disk
        return 0;
    }

    @Override
    public long getLastAppliedIndex() {
        if (lastApplied == -1) {
            lastApplied = getInitialLastApplied();
        }
        return lastApplied;
    }

    @Override
    public void apply(LogEntry entry) {
        doApply(entry);
        updateState(entry);
    }

    @Override
    public void applyAll(List<LogEntry> entries) {
        if (!entries.isEmpty()) {
            entries.forEach(this::doApply);
            updateState(Utils.getLast(entries));
        }
    }

    private void doApply(LogEntry entry) {
        applyInternal(entry);
        bytesSinceSnapshot += entry.getData().length;
    }

    private void updateState(LogEntry latestEntry) {
        lastApplied = latestEntry.getIndex();

//        long logsSinceSnapshot = lastApplied - (lastSnapshot == null ? 0 : lastSnapshot.getLastLogIndex());
        long logsSinceSnapshot = 0L;

        if (snapshotStrategy.shouldSnapshot(logsSinceSnapshot, bytesSinceSnapshot)) {
            lastSnapshot = takeSnapshot();
            bytesSinceSnapshot = 0;
        }
    }

    protected abstract Snapshot takeSnapshot();

    protected abstract void applyInternal(LogEntry entry);

    @Override
    public Snapshot getLatestSnapshot() {
        return lastSnapshot;
    }
}
