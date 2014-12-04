package com.mcintyret.raft.message;

import com.mcintyret.raft.rpc.BaseRequest;
import com.mcintyret.raft.rpc.BaseResponse;

/**
 * User: tommcintyre
 * Date: 11/29/14
 */
public interface MessageDispatcher {

    void sendRequest(BaseRequest request);

    void sendResponse(BaseResponse response);

}
