package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class AppendEntriesResponse implements RpcMessage {

    private final long requestId;

    private final long term;

    private final boolean success;

    public AppendEntriesResponse(long requestId, long term, boolean success) {
        this.requestId = requestId;
        this.term = term;
        this.success = success;
    }

    @Override
    public long getTerm() {
        return term;
    }

    public boolean isSuccess() {
        return success;
    }

    public long getRequestId() {
        return requestId;
    }

    @Override
    public void visit(RpcMessageVisitor visitor) {
        visitor.onAppendEntriesResponse(this);
    }
}
