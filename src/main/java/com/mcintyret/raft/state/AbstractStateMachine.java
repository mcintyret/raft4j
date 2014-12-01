package com.mcintyret.raft.state;

public abstract class AbstractStateMachine implements StateMachine {

    private long lastApplied;

    public AbstractStateMachine() {
        this.lastApplied = getInitialLastApplied();
    }

    protected long getInitialLastApplied() {
        // This works for volatile StateMachines; override to implement a persistent StateMachine by eg loading from disk
        return 0;
    }

    @Override
    public long getLastApplied() {
        return lastApplied;
    }

    @Override
    public void setLastApplied(long lastApplied) {
        this.lastApplied = lastApplied;
    }
}
