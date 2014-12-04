package com.mcintyret.raft.message;

import com.mcintyret.raft.rpc.BaseResponse;
import com.mcintyret.raft.rpc.Message;

/**
 * User: tommcintyre
 * Date: 12/3/14
 */
public class DecoratingMessageReceiver<M extends Message> implements MessageReceiver<M> {

    private final Messages messages;

    private final MessageReceiver<M> delegate;

    public DecoratingMessageReceiver(Messages messages, MessageReceiver<M> delegate) {
        this.messages = messages;
        this.delegate = delegate;
    }

    @Override
    public void messageReceived(M message) {
        if (message instanceof BaseResponse) {
            BaseResponse response = (BaseResponse) message;
            messages.decorate(response);

            if (response.getRequest() == null) {
                return; // This has been received in error or dealt with before
            }
        }
        delegate.messageReceived(message);
    }
}
