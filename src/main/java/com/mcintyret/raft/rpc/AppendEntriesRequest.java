package com.mcintyret.raft.rpc;

import com.mcintyret.raft.core.LogEntry;

import java.util.List;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class AppendEntriesRequest implements RpcMessage {

    private final long term;

    private final int leaderId;

    private final long prevLogIndex;

    private final long prevLogTerm;

    private final List<LogEntry> entries;

    private final long leaderCommit;

    public AppendEntriesRequest(long term, int leaderId, long prevLogIndex, long prevLogTerm, List<LogEntry> entries, long leaderCommit) {
        this.term = term;
        this.leaderId = leaderId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }

    @Override
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

    public List<LogEntry> getEntries() {
        return entries;
    }

    public long getLeaderCommit() {
        return leaderCommit;
    }

    @Override
    public void visit(RpcMessageVisitor visitor) {
        visitor.onAppendEntriesRequest(this);
    }
}
