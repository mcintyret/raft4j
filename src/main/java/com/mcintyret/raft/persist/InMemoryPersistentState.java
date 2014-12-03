package com.mcintyret.raft.persist;

import com.mcintyret.raft.address.Peer;
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

    private Peer votedFor = null;

    @Override
    public long getCurrentTerm() {
        return currentTerm;
    }

    @Override
    public void setCurrentTerm(long currentTerm) {
        // TODO: this logic applies to all PersistentState implementations: refactor into base class?
        if (currentTerm < this.currentTerm) {
            throw new IllegalStateException("Cannot decrease current term");
        }
        if (currentTerm > this.currentTerm) {
            this.votedFor = null;
        }
        this.currentTerm = currentTerm;
    }

    @Override
    public Peer getVotedFor() {
        return votedFor;
    }

    @Override
    public void setVotedFor(Peer votedFor) {
        this.votedFor = votedFor;
    }

    @Override
    public List<LogEntry> getAllLogEntries() {
        return Collections.unmodifiableList(logEntries);
    }

    @Override
    public List<LogEntry> getLogEntriesBetween(long fromIndex, long toIndex) {
        return Collections.unmodifiableList(new ArrayList<>(logEntries.subList((int) --fromIndex, (int) --toIndex)));
    }

    @Override
    public LogEntry getLogEntry(long logIndex) {
        return logIndex == 0 ? EMPTY_LOG : logEntries.get((int) (logIndex - 1));
    }

    @Override
    public LogEntry getLastLogEntry() {
        return logEntries.isEmpty() ? EMPTY_LOG : logEntries.get(logEntries.size() - 1);
    }

    @Override
    public void deleteConflictingAndAppend(List<LogEntry> entries) {
        int listIndex = -1;
        for (LogEntry entry : entries) {
            listIndex = (int) (entry.getIndex() - 1);
            if (logEntries.size() <= listIndex) {
                logEntries.add(entry);
            } else {
                // Overwrite previous entries
                logEntries.set(listIndex, entry);
            }
        }

        // Delete all future entries
        for (int i = listIndex + 1; i < logEntries.size(); i++) {
            logEntries.remove(i);
        }
    }

    @Override
    public void appendLogEntry(LogEntry newEntry) {
        logEntries.add(newEntry);
    }

}
