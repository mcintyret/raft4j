package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class AppendEntriesResponse extends BaseResponse<AppendEntriesRequest> implements RaftRpcMessage {

    private final long term;

    private final boolean success;

    public AppendEntriesResponse(Header header, long term, boolean success) {
        super(header);
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

    @Override
    public void visit(RpcMessageVisitor visitor) {
        visitor.onAppendEntriesResponse(this);
    }
}
