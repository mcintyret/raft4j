package com.mcintyret.raft.persist;

import com.mcintyret.raft.core.LogEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
// This contradiction in terms is used for dev & debugging
public class InMemoryPersistentState implements PersistentState {

    private long currentTerm;

    private final List<LogEntry> logEntries = new ArrayList<>();

    private int votedFor = -1;

    @Override
    public long getCurrentTerm() {
        return currentTerm;
    }

    @Override
    public void setCurrentTerm(long currentTerm) {
        this.currentTerm = currentTerm;
    }

    @Override
    public int getVotedFor() {
        return votedFor;
    }

    @Override
    public void setVotedFor(int votedFor) {
        this.votedFor = votedFor;
    }

    @Override
    public List<LogEntry> getLogEntries() {
        return Collections.unmodifiableList(logEntries);
    }

    @Override
    public LogEntry getLogEntry(long logIndex) {
        return logEntries.get((int) logIndex);
    }

    @Override
    public void appendLogEntry(LogEntry newEntry) {
        logEntries.add(newEntry);
    }
}
