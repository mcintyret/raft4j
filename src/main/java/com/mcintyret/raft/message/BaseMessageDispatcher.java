package com.mcintyret.raft.message;

import com.mcintyret.raft.rpc.BaseRequest;

/**
 * User: tommcintyre
 * Date: 12/3/14
 */
public abstract class BaseMessageDispatcher implements MessageDispatcher {

    private final Messages messages;

    public BaseMessageDispatcher(Messages messages) {
        this.messages = messages;
    }

    public void sendRequest(BaseRequest message) {
        messages.register(message);
        sendRequestInternal(message);
    }

    protected abstract void sendRequestInternal(BaseRequest message);

}
