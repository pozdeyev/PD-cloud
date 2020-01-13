package com.geekbrains.cloud.common;

//Сообщение с информацией об авторизации
public class AuthMessage extends AbstractMessage {

    private String name;
    private String pass;
    private boolean auth = false;

    public AuthMessage(String name, String pass, boolean auth) {
        this.name = name;
        this.pass = pass;
        this.auth = auth;
        }

    public String getName() {
        return name;
    }
    public String getPass() {
        return pass;
    }
    public boolean getAuth() { return auth; }

   // Сеттер для отправки флага авторизации
    public void setAuth(boolean auth) { this.auth = auth; }
}
