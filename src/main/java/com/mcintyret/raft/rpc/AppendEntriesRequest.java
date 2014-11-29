package com.mcintyret.raft.rpc;

import com.mcintyret.raft.core.LogEntry;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class AppendEntriesRequest {

    private final long term;

    private final int leaderId;

    private final long prevLogIndex;

    private final long prevLogTerm;

    private final LogEntry[] entries;

    private final long leaderCommit;

    public AppendEntriesRequest(long term, int leaderId, long prevLogIndex, long prevLogTerm, LogEntry[] entries, long leaderCommit) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }

    public long getTerm() {
        return term;
    }

    public int getLeaderId() {
        return leaderId;
    }

    public long getPrevLogIndex() {
        return prevLogIndex;
    }

    public long getPrevLogTerm() {
        return prevLogTerm;
    }

    public LogEntry[] getEntries() {
        return entries;
    }

    public long getLeaderCommit() {
        return leaderCommit;
    }
}
