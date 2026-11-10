package com.liveplatform.user.api;

import java.io.Serializable;

public class LoginResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String token;
    private long expiresInSeconds;
    private UserDTO user;

    public LoginResultDTO() { }
    public LoginResultDTO(String token, long expiresInSeconds, UserDTO user) {
        this.token = token;
        this.expiresInSeconds = expiresInSeconds;
        this.user = user;
    }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
    public void setExpiresInSeconds(long value) { expiresInSeconds = value; }
    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }
}
