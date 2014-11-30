package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class RequestVoteResponse implements RpcMessage {

    private final int voterId;

    private final long term;

    private final boolean voteGranted;

    public RequestVoteResponse(int voterId, long term, boolean voteGranted) {
        this.voterId = voterId;
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public int getVoterId() {
        return voterId;
    }

    @Override
    public long getTerm() {
        return term;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }

    @Override
    public void visit(RpcMessageVisitor visitor) {
        visitor.onRequestVoteResponse(this);
    }
}
