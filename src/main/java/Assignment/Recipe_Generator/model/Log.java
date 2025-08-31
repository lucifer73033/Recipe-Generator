package Assignment.Recipe_Generator.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "logs")
public class Log {
    
    @Id
    private String id;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @NotNull
    private String event;
    
    private String userId; // optional, for user-specific events
    
    private Map<String, Object> metadata; // flexible structure for event details
    
    private LogLevel level;
    
    public enum LogLevel {
        INFO, WARN, ERROR, DEBUG
    }
}



