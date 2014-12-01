package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public interface RpcMessage extends Message {

    void visit(RpcMessageVisitor visitor);

}
