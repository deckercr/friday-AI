package com.friday.config;

import com.friday.auth.JwtService;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
public class StompJwtChannelInterceptor implements ChannelInterceptor {

    private static final Logger log = LoggerFactory.getLogger(StompJwtChannelInterceptor.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    public StompJwtChannelInterceptor(JwtService jwtService, UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
            MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("STOMP CONNECT rejected: missing or malformed Authorization header");
            throw new MessageDeliveryException(message, "Missing or invalid Authorization header");
        }

        String token = authHeader.substring(7);
        try {
            String username = jwtService.extractUsername(token);
            var userDetails = userDetailsService.loadUserByUsername(username);
            if (!jwtService.isValid(token, username)) {
                log.warn("STOMP CONNECT rejected: expired or invalid token for '{}'", username);
                throw new MessageDeliveryException(message, "Invalid JWT token");
            }
            var auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
            accessor.setUser(auth);
        } catch (JwtException e) {
            log.warn("STOMP CONNECT rejected: JWT parse failure — {}", e.getMessage());
            throw new MessageDeliveryException(message, "Invalid JWT token");
        }

        return message;
    }
}
