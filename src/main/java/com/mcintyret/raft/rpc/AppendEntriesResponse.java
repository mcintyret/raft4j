package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class AppendEntriesResponse implements RpcMessage {

    private final long term;

    private final boolean success;

    public AppendEntriesResponse(long term, boolean success) {
        this.term = term;
        this.success = success;
    }

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
