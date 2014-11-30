package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public interface RpcMessage {

    void visit(RpcMessageVisitor visitor);

}
