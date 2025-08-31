package Assignment.Recipe_Generator.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OpenRouterRequest {
    
    private String model;
    private List<Message> messages;
    
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    private Double temperature;
    
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;
    
    @Data
    @Builder
    public static class Message {
        private String role;
        private Object content; // Can be String or List<Content> for multimodal
    }
    
    @Data
    @Builder
    public static class Content {
        private String type; // "text" or "image_url"
        private String text;
        
        @JsonProperty("image_url")
        private ImageUrl imageUrl;
    }
    
    @Data
    @Builder
    public static class ImageUrl {
        private String url;
        private String detail; // "low", "high", or "auto"
    }
    
    @Data
    @Builder
    public static class ResponseFormat {
        private String type; // "json_object" or "text"
    }
}



