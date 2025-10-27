package pwr.zpi.hotspotter.repositoryanalysis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class RepositoryAnalysisConfig {

    @Bean(name = "analysisExecutor", destroyMethod = "shutdown")
    public ExecutorService analysisExecutor() {
        int poolSize = Runtime.getRuntime().availableProcessors();
        return Executors.newFixedThreadPool(poolSize);
    }

}
