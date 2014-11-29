package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public class RequestVoteRequest {

    private final long term;

    private final int candidateId;

    private final long lastLogIndex;

    private final long lastLogTerm;

    public RequestVoteRequest(long term, int candidateId, long lastLogIndex, long lastLogTerm) {
        this.term = term;
        this.candidateId = candidateId;
        this.lastLogIndex = lastLogIndex;
        this.lastLogTerm = lastLogTerm;
    }

    public long getTerm() {
        return term;
    }

    public int getCandidateId() {
        return candidateId;
    }

    public long getLastLogIndex() {
        return lastLogIndex;
    }

    public long getLastLogTerm() {
        return lastLogTerm;
    }
}
