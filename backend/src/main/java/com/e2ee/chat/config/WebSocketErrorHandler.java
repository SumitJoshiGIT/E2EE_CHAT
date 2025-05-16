package com.e2ee.chat.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class WebSocketErrorHandler extends StompSubProtocolErrorHandler {
    
    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, Throwable ex) {
        log.error("Error while processing client WebSocket message", ex);
        
        // Extract information from the client message headers
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(clientMessage);
        StompCommand command = accessor.getCommand();
        String sessionId = accessor.getSessionId();
        
        // Create and log a detailed error description
        String detailedErrorMessage = String.format(
            "WebSocket error [sessionId: %s, command: %s]: %s",
            sessionId,
            command,
            ex.getMessage()
        );
        log.error(detailedErrorMessage);
        
        // Return graceful error message to client
        if (ex instanceof MessageDeliveryException) {
            return prepareErrorMessage(accessor, "Error delivering message: " + ex.getMessage());
        } else {
            return prepareErrorMessage(accessor, "Internal server error while processing message");
        }
    }
    
    private Message<byte[]> prepareErrorMessage(StompHeaderAccessor accessor, String errorMessage) {
        accessor.setLeaveMutable(true);
        accessor.setMessage("ERROR");
        accessor.setHeader("message", errorMessage);
        
        return MessageBuilder.createMessage(errorMessage.getBytes(), accessor.getMessageHeaders());
    }
}
