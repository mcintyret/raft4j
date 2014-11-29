package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class RequestVotesResponse {

    private final long term;

    private final boolean voteGranted;

    public RequestVotesResponse(long term, boolean voteGranted) {
        this.term = term;
        this.voteGranted = voteGranted;
    }

    public long getTerm() {
        return term;
    }

    public boolean isVoteGranted() {
        return voteGranted;
    }
}
