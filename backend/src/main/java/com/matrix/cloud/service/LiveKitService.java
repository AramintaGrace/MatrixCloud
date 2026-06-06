package com.matrix.cloud.service;

import io.livekit.server.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LiveKitService {

    @Value("${livekit.api.key}")
    private String apiKey;

    @Value("${livekit.api.secret}")
    private String apiSecret;

    @Value("${livekit.url}")
    private String livekitUrl;

    public String createToken(String roomName, String identity, String name) {
        AccessToken token = new AccessToken(apiKey, apiSecret);
        token.setIdentity(identity);
        token.setName(name);
        token.addGrants(
            new RoomJoin(true),
            new RoomName(roomName),
            new CanPublish(true),
            new CanSubscribe(true)
        );
        return token.toJwt();
    }

    public String getLivekitUrl() {
        return livekitUrl;
    }
}
