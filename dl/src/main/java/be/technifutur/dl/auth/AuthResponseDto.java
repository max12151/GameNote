package be.technifutur.dl.auth;

import be.technifutur.dl.user.UserDto;

public class AuthResponseDto {

    private String token;
    private UserDto user;

    public AuthResponseDto() {
    }

    public AuthResponseDto(String token, UserDto user) {
        this.token = token;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public UserDto getUser() {
        return user;
    }

    public void setUser(UserDto user) {
        this.user = user;
    }
}