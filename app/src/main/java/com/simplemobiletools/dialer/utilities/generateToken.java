package com.simplemobiletools.dialer.utilities;

import android.content.Context;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

public class generateToken {
     static String signature = "Keo6oAZ+9cseqG7uazWzOVS9jAPNyycxOk1uMHaB/Hs=";

    public static String getToken(String meetingID, Context passedcontext) throws UnsupportedEncodingException {
        String name, id;
        PreferenceManager preferenceManager = new PreferenceManager(passedcontext);
        Map<String, Object> payload = new HashMap<>();
        Map<String, String> user = new HashMap<>();
        Map<String, Object> context = new HashMap<>();
        if(preferenceManager.getString(Constants.KEY_FIRST_NAME)==null){
            name = preferenceManager.getString(Constants.KEY_MOBILE);
            id = preferenceManager.getString(Constants.KEY_MOBILE);
        }else{
            name =  preferenceManager.getString(Constants.KEY_FIRST_NAME);
            id   = preferenceManager.getString(Constants.KEY_FIRST_NAME);
        }
        user.put("avatar", "https://media-exp1.licdn.com/dms/image/C4E03AQE0XUdjM1yBGQ/profile-displayphoto-shrink_200_200/0/1517053167748?e=1652918400&v=beta&t=5HhvYLej__wzd0BKFq5wbPjJ-_m-Qmf4f3Y6btj1Uwg");
        user.put("name", name);
        user.put("id",  id);
        Map<String, String> group = new HashMap<>();
        context.put("user", user);
        context.put("group", "meet.onccxn.info");
        payload.put("context", context);

        JwtBuilder Jwt = Jwts.builder()
            .setHeaderParam("typ", "JWT")
            .setHeaderParam("alg", "HS256")
            .addClaims(payload)
            .setAudience("onecxninfra")
            .setIssuer("onecxninfra")
            .setSubject("jitsimeet.onecxn.info")
            .claim("room", meetingID)
            .signWith(SignatureAlgorithm.HS256, signature.getBytes("UTF-8"));

        /* return Jwt.compact(); */
        return Jwt.compact()+"#config.startSilent=true&config.startWithAudioMuted=true&config.disableInitialGUM=true";
    }
}
