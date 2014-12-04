package com.mcintyret.raft.message;

import com.mcintyret.raft.rpc.BaseRequest;
import com.mcintyret.raft.rpc.BaseResponse;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: tommcintyre
 * Date: 12/3/14
 */
public class Messages {

    private final Map<String, RequestWrapper<BaseRequest>> requests = new ConcurrentHashMap<>();

    public void register(BaseRequest request) {
        String ruuid = request.getHeader().getRuuid();
        if (requests.put(ruuid, new RequestWrapper<>(request)) != null) {
            throw new AssertionError("Seen this request id before!: " + ruuid);
        }
    }

    public void decorate(BaseResponse response) {
        RequestWrapper<BaseRequest> request = requests.remove(response.getHeader().getRuuid());
        response.setRequest(request.value);
    }

    public Collection<RequestWrapper<BaseRequest>> getAllOutstandingRequests() {
        return Collections.unmodifiableCollection(requests.values());
    }

    public static class RequestWrapper<T> {
        private int attempts;

        private final T value;

        private RequestWrapper(T value) {
            this.value = value;
        }

        public int getAttempts() {
            return attempts;
        }

        public T getRequest() {
            return value;
        }

        public void incrementAttempts() {
            attempts++;
        }
    }

}
