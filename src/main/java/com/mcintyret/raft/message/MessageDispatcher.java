package com.mcintyret.raft.message;

import com.mcintyret.raft.client.ClientMessage;
import com.mcintyret.raft.rpc.Message;
import com.mcintyret.raft.rpc.RaftRpcMessage;
import com.mcintyret.raft.rpc.RpcMessage;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public interface MessageDispatcher {

    // Todo: handle exceptions?
    // TODO: retry indefinitely (this is a key part of raft!)
    void sendMessage(int recipientId, Message message);

    void sendMessage(ClientMessage message);

}
