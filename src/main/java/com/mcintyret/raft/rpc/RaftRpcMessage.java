package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/30/14
 */
public interface RaftRpcMessage extends RpcMessage {

    long getTerm();

}
