package Assignment.Recipe_Generator.dto;

import Assignment.Recipe_Generator.model.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private boolean success;
    private String message;
    private String token; // Base64 encoded username:password
    private User user;
}
