package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/30/14
 */
public class BaseResponse<RequestType> implements Message {

    private final String uuid;

    private RequestType request;

    public BaseResponse(String uuid) {
        this.uuid = uuid;
    }

    public RequestType getRequest() {
        return request;
    }

    public void setRequest(RequestType request) {
        this.request = request;
    }

    @Override
    public String getRequestUid() {
        return uuid;
    }
}
