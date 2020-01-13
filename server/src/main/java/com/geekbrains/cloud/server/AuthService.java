package com.geekbrains.cloud.server;

public interface AuthService {

    boolean authUser(String username, String password);
}
