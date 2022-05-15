package com.example.kursov.models;

public class User {

    private String email;
    private String password;
    private String nickname;
    private int score;

    public User() {

    }

    public User(String email, String password, String nickname, int score) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.score = score;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
