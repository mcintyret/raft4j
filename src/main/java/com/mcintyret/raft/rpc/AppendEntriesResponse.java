package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class AppendEntriesResponse implements RpcMessage {

    private final int responderId;

    private final long term;

    private final boolean success;

    public AppendEntriesResponse(int responderId, long term, boolean success) {
        this.responderId = responderId;
        this.term = term;
        this.success = success;
    }

    public int getResponderId() {
        return responderId;
    }

    @Override
    public long getTerm() {
        return term;
    }

    public boolean isSuccess() {
        return success;
    }

    @Override
    public void visit(RpcMessageVisitor visitor) {
        visitor.onAppendEntriesResponse(this);
    }
}
