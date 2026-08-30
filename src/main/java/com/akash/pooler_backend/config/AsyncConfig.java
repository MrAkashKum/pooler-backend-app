package com.akash.pooler_backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async configuration for background tasks (e.g. mail dispatch).
 * Uses bounded pools so background work cannot create unbounded threads or queues.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    private final AppProperties appProperties;

    @Override
    public Executor getAsyncExecutor() {
        return defaultExecutor();
    }

    @Override
    public org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) ->
                log.error("Async task failed: {}.{} type={}",
                        method.getDeclaringClass().getSimpleName(),
                        method.getName(),
                        throwable.getClass().getSimpleName());
    }

    @Bean(name = {"taskExecutor", "defaultExecutor"})
    @Primary
    public ThreadPoolTaskExecutor defaultExecutor() {
        return buildExecutor("async-", appProperties.getAsync().getDefaults());
    }

    @Bean("mailExecutor")
    public Executor mailExecutor() {
        return buildExecutor("mail-", appProperties.getAsync().getMail());
    }

    @Bean("auditExecutor")
    public Executor auditExecutor() {
        return buildExecutor("audit-", appProperties.getAsync().getAudit());
    }

    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        return buildExecutor("notification-", appProperties.getAsync().getNotification());
    }

    private ThreadPoolTaskExecutor buildExecutor(String threadNamePrefix, AppProperties.Async.ExecutorPool pool) {
        int corePoolSize = Math.max(1, pool.getCorePoolSize());
        int maxPoolSize = Math.max(corePoolSize, pool.getMaxPoolSize());
        int queueCapacity = Math.max(0, pool.getQueueCapacity());
        int keepAliveSeconds = Math.max(10, pool.getKeepAliveSeconds());

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setKeepAliveSeconds(keepAliveSeconds);
        executor.setAllowCoreThreadTimeOut(true);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setTaskDecorator(task -> {
            Map<String, String> parentContext = MDC.getCopyOfContextMap();
            return () -> {
                Map<String, String> workerContext = MDC.getCopyOfContextMap();
                try {
                    if (parentContext == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(parentContext);
                    }
                    task.run();
                } finally {
                    if (workerContext == null) {
                        MDC.clear();
                    } else {
                        MDC.setContextMap(workerContext);
                    }
                }
            };
        });
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(Math.max(1, appProperties.getAsync().getShutdownAwaitSeconds()));
        executor.setRejectedExecutionHandler((task, threadPoolExecutor) -> {
            log.warn("asyncExecutorSaturated threadNamePrefix={} activeCount={} poolSize={} queueSize={}",
                    threadNamePrefix,
                    threadPoolExecutor.getActiveCount(),
                    threadPoolExecutor.getPoolSize(),
                    threadPoolExecutor.getQueue().size());
            new ThreadPoolExecutor.CallerRunsPolicy().rejectedExecution(task, threadPoolExecutor);
        });
        executor.initialize();
        return executor;
    }
}
