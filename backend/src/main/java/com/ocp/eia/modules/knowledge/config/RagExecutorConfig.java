package com.ocp.eia.modules.knowledge.config;

import com.ocp.eia.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@ConditionalOnProperty(name = "app.knowledge.enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class RagExecutorConfig {

    private final AppProperties appProperties;

    @Bean("ragExecutor")
    public Executor ragExecutor() {
        AppProperties.Ai.Rag.Performance perf = appProperties.getAi().getRag().getPerformance();
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(perf.getThreadPoolCoreSize());
        executor.setMaxPoolSize(perf.getThreadPoolMaxSize());
        executor.setQueueCapacity(perf.getThreadPoolQueueCapacity());
        executor.setThreadNamePrefix("rag-exec-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        
        // Politique de rejet : enregistrer et exécuter dans le thread courant
        executor.setRejectedExecutionHandler(new RagRejectedExecutionHandler());
        
        executor.initialize();
        
        log.info("Pool d'exécution RAG configuré: core={}, max={}, queue={}", 
                perf.getThreadPoolCoreSize(), perf.getThreadPoolMaxSize(), perf.getThreadPoolQueueCapacity());
        
        return executor;
    }
    
    /**
     * Gestionnaire de rejet personnalisé pour les tâches RAG
     */
    private static class RagRejectedExecutionHandler implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("Tâche RAG rejetée par le pool d'exécution, exécution dans le thread courant. " +
                    "Active threads: {}, Queue size: {}", executor.getActiveCount(), executor.getQueue().size());
            
            // Exécuter dans le thread courant (CallerRunsPolicy)
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }
}