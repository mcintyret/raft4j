package com.mcintyret.raft.message;

import com.mcintyret.raft.rpc.BaseRequest;
import com.mcintyret.raft.rpc.BaseResponse;

import java.util.HashMap;
import java.util.Map;

/**
 * User: tommcintyre
 * Date: 11/30/14
 */

// TODO: better name
public class MessageHandler {

    private final Map<String, BaseRequest> map = new HashMap<>();

    public <T extends BaseRequest> T register(T message) {
        map.put(message.getRequestUid(), message);
        return message;
    }

    public <U extends BaseResponse<T>, T extends BaseRequest> U decorate(U response) {
        T request = (T) map.remove(response.getRequestUid()); // TODO: what if it's null?
        response.setRequest(request);
        return response;
    }

    // TODO: work out a smart time to call this.

    /*
    Potential rule:
    - clear on every state change
    - if a response arrives and it's request cannot be found, it means it was intended for a previous state, so it can be safely dropped (?)
    - - possibly after the term check, which seems super important!
     */
    public void clear() {
        map.clear();
    }

}
