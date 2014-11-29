package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public interface RpcMessageVisitor {

    void onAppendEntriesRequest(AppendEntriesRequest aeReq);

    void onAppendEntriesResponse(AppendEntriesResponse aeResp);

    void onRequestVoteRequest(RequestVoteRequest rvReq);

    void onRequestVoteResponse(RequestVoteResponse rvResp);

    void onNewEntryRequest(NewEntryRequest neReq);

}
