package com.mcintyret.raft.message;

import com.mcintyret.raft.rpc.RpcMessage;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public interface MessageDispatcher {

    // Todo: handle exceptions?
    void sendMessage(int recipientId, RpcMessage message);

}
