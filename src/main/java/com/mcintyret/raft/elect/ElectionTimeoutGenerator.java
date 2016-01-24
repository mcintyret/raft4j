package com.mcintyret.raft.elect;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public interface ElectionTimeoutGenerator {

    long nextElectionTimeout();

    long minimumElectionTimeout();

}
