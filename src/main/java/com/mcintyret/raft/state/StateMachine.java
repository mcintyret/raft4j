package com.mcintyret.raft.state;

/**
 * User: tommcintyre
 * Date: 11/30/14
 */
public interface StateMachine {

    void apply(long index, byte[] data);

    long getLastApplied();

    void setLastApplied(long lastApplied);

}
