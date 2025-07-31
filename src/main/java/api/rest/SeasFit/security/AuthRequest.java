package api.rest.SeasFit.security;

import lombok.Getter;

@Getter
public class AuthRequest {
    private String userName;
    private String password;

    public void setUsername(String username) {
        this.userName = userName;
    }

}
