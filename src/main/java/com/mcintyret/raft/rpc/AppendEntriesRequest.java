package com.mcintyret.raft.rpc;

import com.mcintyret.raft.core.LogEntry;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class AppendEntriesRequest extends BaseRequest implements RaftRpcMessage {

    private final long term;

    private final int leaderId;

    private final int targetId;

    private final long prevLogIndex;

    private final long prevLogTerm;

    private final List<LogEntry> entries;

    private final long leaderCommit;

    public AppendEntriesRequest(long term, int leaderId, int targetId, long prevLogIndex, long prevLogTerm, List<LogEntry> entries, long leaderCommit) {
        this.term = term;
        this.leaderId = leaderId;
        this.targetId = targetId;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }

    public int getTargetId() {
        return targetId;
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
