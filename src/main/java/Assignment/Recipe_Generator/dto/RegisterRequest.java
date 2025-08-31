package Assignment.Recipe_Generator.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String email;
    private String name;
    private String password;
}
