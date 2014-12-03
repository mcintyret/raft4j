package com.mcintyret.raft.persist;

import com.mcintyret.raft.address.Peer;
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

    // null if none
    Peer getVotedFor();

    void setVotedFor(Peer votedFor);

    List<LogEntry> getAllLogEntries();

    List<LogEntry> getLogEntriesBetween(long fromIndex, long toIndex);

    /**
     * Gets the log entry at the 1-based index
     */
    LogEntry getLogEntry(long logIndex);

    LogEntry getLastLogEntry();

    /**
     * Simply appends a log entry to the end of the log.
     *
     * This is for Leaders appending entirely new entries received from clients
     */
    void appendLogEntry(LogEntry newEntry);

    /**
     * Appends the given log entries to this node's log.
     *
     * If there are any existing entries which conflict with the new ones they, and all entries after them, are deleted.
     *
     * This is for Followers appending entries sent from the Leader
     */
    void deleteConflictingAndAppend(List<LogEntry> entries);

}
