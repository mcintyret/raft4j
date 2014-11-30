package com.mcintyret.raft.persist;

import com.mcintyret.raft.core.LogEntry;

import java.util.List;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public interface PersistentState {

    public static final LogEntry EMPTY_LOG = new LogEntry(0, 0, null);

    long getCurrentTerm();

    void setCurrentTerm(long currentTerm);

    // -1 if none
    int getVotedFor();

    void setVotedFor(int votedFor);

    List<LogEntry> getLogEntries();

    LogEntry getLogEntry(long logIndex);

    LogEntry getLastLogEntry();

    void appendLogEntry(LogEntry newEntry);

}
