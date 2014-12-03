package com.mcintyret.raft.message;

import com.mcintyret.raft.rpc.Message;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public interface MessageDispatcher {

    void sendMessage(Message message);

}
