package com.mcintyret.raft.rpc;

/**
 * User: tommcintyre
 * Date: 11/30/14
 */
public class BaseResponse<RequestType> extends BaseMessage {

    private RequestType request;

    public BaseResponse(Header header) {
        super(header);
    }

    public RequestType getRequest() {
        return request;
    }

    public void setRequest(RequestType request) {
        this.request = request;
    }

}
