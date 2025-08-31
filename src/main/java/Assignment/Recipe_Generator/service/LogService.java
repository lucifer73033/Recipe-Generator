package Assignment.Recipe_Generator.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Slf4j
public class LogService {

    public void logUserActivity(String event, String userId, Map<String, Object> metadata) {
        log.info("User Activity - Event: {}, User: {}, Metadata: {}", event, userId, metadata);
    }

    public void logSystemEvent(String event, Map<String, Object> metadata, String level) {
        log.info("System Event - Event: {}, Level: {}, Metadata: {}", event, level, metadata);
    }

    public void logLLMCall(String operation, String userId, Map<String, Object> metadata) {
        Map<String, Object> enrichedMetadata = Map.of(
            "operation", operation,
            "timestamp", LocalDateTime.now(),
            "metadata", metadata != null ? metadata : Map.of()
        );
        
        log.info("LLM Call - Operation: {}, User: {}, Metadata: {}", operation, userId, enrichedMetadata);
    }

    public void logRecipeGeneration(String userId, String strategy, int resultCount, Map<String, Object> criteria) {
        Map<String, Object> metadata = Map.of(
            "strategy", strategy,
            "resultCount", resultCount,
            "criteria", criteria
        );
        
        log.info("Recipe Generation - User: {}, Strategy: {}, Results: {}, Criteria: {}", 
                userId, strategy, resultCount, criteria);
    }
}



