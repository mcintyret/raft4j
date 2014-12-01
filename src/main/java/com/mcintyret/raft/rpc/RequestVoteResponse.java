package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class RequestVoteResponse extends BaseResponse<RequestVoteRequest> implements RaftRpcMessage {

    private final int voterId;

    private final long term;

    private final boolean voteGranted;

    public RequestVoteResponse(String uuid, int voterId, long term, boolean voteGranted) {
        super(uuid);
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
