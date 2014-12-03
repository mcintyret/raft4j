package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class RequestVoteResponse extends BaseResponse<RequestVoteRequest> implements RaftRpcMessage {

    private final long term;

    private final boolean voteGranted;

    public RequestVoteResponse(Header header, long term, boolean voteGranted) {
        super(header);
        this.term = term;
        this.voteGranted = voteGranted;
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
