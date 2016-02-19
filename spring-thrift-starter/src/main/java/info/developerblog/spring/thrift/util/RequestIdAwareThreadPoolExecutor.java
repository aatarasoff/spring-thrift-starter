package info.developerblog.spring.thrift.util;

import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import static org.slf4j.MDC.get;
import static org.slf4j.MDC.put;
import static ru.trylogic.spring.boot.thrift.beans.RequestIdLogger.getMDCKey;

public class RequestIdAwareThreadPoolExecutor extends ThreadPoolTaskExecutor {

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return super.submit(new RequestIdAwareCallable<>(task, get(getMDCKey())));
    }

    @Override
    public <T> ListenableFuture<T> submitListenable(Callable<T> task) {
        return super.submitListenable(new RequestIdAwareCallable<>(task, get(getMDCKey())));
    }

    private static class RequestIdAwareCallable<T> implements Callable<T> {
        private Callable<T> task;
        private String requestId;

        public RequestIdAwareCallable(Callable<T> task, String requestId) {
            this.task = task;
            this.requestId = requestId;
        }

        @Override
        public T call() throws Exception {
            if (requestId != null) {
                put(getMDCKey(), requestId);
            }
            return task.call();
        }
    }
}
