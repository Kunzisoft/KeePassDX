package com.patarapolw.password_generator;

import java.security.SecureRandom;

public class PasswordGenerator {
    private SecureRandom random = new SecureRandom();

    public String generatePin(int length){
        StringBuilder pinBuilder = new StringBuilder();
        for(int i=0; i<length; i++){
            pinBuilder.append(random.nextInt(10));
        }

        return pinBuilder.toString();
    }
}
