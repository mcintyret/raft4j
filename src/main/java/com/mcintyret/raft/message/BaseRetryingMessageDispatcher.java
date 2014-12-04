package com.mcintyret.raft.message;

import com.mcintyret.raft.rpc.BaseRequest;
import com.mcintyret.raft.rpc.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * User: tommcintyre
 * Date: 12/3/14
 */
public abstract class BaseRetryingMessageDispatcher implements MessageDispatcher {

    private static final Logger LOG = LoggerFactory.getLogger(BaseRetryingMessageDispatcher.class);

    private static final long MESSAGE_RETRY_MILLIS = 1000; // retry every 1s

    private final ScheduledExecutorService messageExec = Executors.newSingleThreadScheduledExecutor();

    private final Messages messages;

    public BaseRetryingMessageDispatcher(Messages messages) {
        this.messages = messages;
        messageExec.scheduleAtFixedRate(() -> {
                messages.getAllOutstandingRequests().forEach(req -> {
                    if (req.getAttempts() > 0) {
                        Header h = req.getRequest().getHeader();
                        sendRequestInternal(req.getRequest());
                        LOG.info("Resending message from {} to {}, ruuid={}", h.getSource(), h.getDestination(), h.getRuuid());
                    }
                    req.incrementAttempts();
                });
            }, MESSAGE_RETRY_MILLIS, MESSAGE_RETRY_MILLIS, TimeUnit.MILLISECONDS);
    }


    public void sendRequest(BaseRequest message) {
        messages.register(message);
        sendRequestInternal(message);
    }

    protected abstract void sendRequestInternal(BaseRequest message);

    public void close() throws Exception {
        messageExec.shutdown();
        messageExec.awaitTermination(5, TimeUnit.SECONDS);
    }

}
