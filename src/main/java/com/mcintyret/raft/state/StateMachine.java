package com.mcintyret.raft.state;

import java.util.List;

import com.mcintyret.raft.core.LogEntry;

/**
 * User: tommcintyre
 * Date: 11/30/14
 */
public interface StateMachine {

    void apply(LogEntry entry);

    void applyAll(List<LogEntry> entries);

    long getLastAppliedIndex();

    Snapshot getLatestSnapshot();

}
