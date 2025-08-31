package Assignment.Recipe_Generator.repository;

import Assignment.Recipe_Generator.model.Log;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogRepository extends MongoRepository<Log, String> {
    
    // Find logs by event type
    List<Log> findByEvent(String event);
    
    // Find logs by user
    List<Log> findByUserId(String userId);
    
    // Find logs by time range
    List<Log> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    // Find recent logs with pagination
    Page<Log> findByOrderByTimestampDesc(Pageable pageable);
    
    // Find logs by level
    List<Log> findByLevel(Log.LogLevel level);
    
    // Find recent logs for streaming (last 100)
    @Query(value = "{}", sort = "{ 'timestamp': -1 }")
    List<Log> findRecentLogs(Pageable pageable);
}



