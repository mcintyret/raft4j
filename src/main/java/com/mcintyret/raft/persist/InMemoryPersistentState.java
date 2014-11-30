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
    public List<LogEntry> getAllLogEntries() {
        return Collections.unmodifiableList(logEntries);
    }

    @Override
    public List<LogEntry> getLogEntriesBetween(long fromIndex, long toIndex) {
        return Collections.unmodifiableList(new ArrayList<>(logEntries.subList((int) fromIndex, (int) toIndex)));
    }

    @Override
    public LogEntry getLogEntry(long logIndex) {
        return logEntries.get((int) logIndex);
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
            if (entries.size() <= listIndex) {
                entries.add(entry);
            } else {
                entries.set(listIndex, entry);
            }
        }

        for (int i = listIndex; i < logEntries.size(); i++) {
            logEntries.remove(i);
        }
    }

    @Override
    public void appendLogEntry(LogEntry newEntry) {
        logEntries.add(newEntry);
    }

}
