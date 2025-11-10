package pwr.zpi.hotspotter.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class SharedAsyncConfiguration {
    private static final int CORE_POOL_SIZE = Math.min(Runtime.getRuntime().availableProcessors(), 4);
    private static final int MAX_POOL_SIZE = 5;
    private static final int QUEUE_CAPACITY = 100;

    @Bean(name = "repoAnalysisExecutor")
    public Executor repoAnalysisExecutor() {
        return createExecutor("RepoAnalysis-");
    }

    @Bean(name = "sonarExecutor")
    public Executor sonarExecutor() {
        return createExecutor("SonarAnalysis-");
    }

    private Executor createExecutor(String threadNamePrefix) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setThreadNamePrefix(threadNamePrefix);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
