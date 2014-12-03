package com.mcintyret.raft.rpc;

import com.mcintyret.raft.core.LogEntry;

import java.util.List;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class AppendEntriesRequest extends BaseRequest implements RaftRpcMessage {

    private final long term;

    private final long prevLogIndex;

    private final long prevLogTerm;

    private final List<LogEntry> entries;

    private final long leaderCommit;

    public AppendEntriesRequest(Header header, long term, long prevLogIndex, long prevLogTerm, List<LogEntry> entries, long leaderCommit) {
        super(header);
        this.term = term;
        this.prevLogIndex = prevLogIndex;
        this.prevLogTerm = prevLogTerm;
        this.entries = entries;
        this.leaderCommit = leaderCommit;
    }

    @Override
    public long getTerm() {
        return term;
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
