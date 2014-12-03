package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class RequestVoteRequest extends BaseRequest implements RaftRpcMessage {

    private final long term;

    private final long lastLogIndex;

    private final long lastLogTerm;

    public RequestVoteRequest(Header header, long term, long lastLogIndex, long lastLogTerm) {
        super(header);
        this.term = term;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    @Override
    public long getTerm() {
        return term;
    }

    public long getLastLogIndex() {
        return lastLogIndex;
    }

    public long getLastLogTerm() {
        return lastLogTerm;
    }

    @Override
    public void visit(RpcMessageVisitor visitor) {
        visitor.onRequestVoteRequest(this);
    }
}
