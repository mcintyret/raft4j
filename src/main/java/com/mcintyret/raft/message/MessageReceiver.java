package com.mcintyret.raft.message;

import com.mcintyret.raft.rpc.Message;

/**
 * User: tommcintyre
 * Date: 12/3/14
 */
public interface MessageReceiver<M extends Message> {

    void messageReceived(M message);
}
