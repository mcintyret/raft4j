package com.mcintyret.raft.persist;

import com.mcintyret.raft.core.LogEntry;

import java.util.List;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public interface PersistentState {

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
