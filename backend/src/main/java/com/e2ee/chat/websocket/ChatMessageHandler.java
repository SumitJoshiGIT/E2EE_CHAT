package com.e2ee.chat.websocket;

import com.e2ee.chat.model.Chat;
import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.model.Message;
import com.e2ee.chat.service.ChatService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatMessageHandler {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final WebSocketSessionTracker sessionTracker;
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();
    private final Map<String, String> userRooms = new ConcurrentHashMap<>(); // Map profileId to their personal room
    private final Map<String, Set<String>> roomSubscribers = new ConcurrentHashMap<>(); // Track which profiles are
                                                                                        // subscribed to which rooms

    /**
     * Called when a user connects to WebSocket
     * Creates a personal room for the user if not already present
     */

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        
        log.info("=== WEBSOCKET CONNECT EVENT DETAILS ===");
        log.info("Headers: {}", headerAccessor.toMap());
        
        // Handle potential null pointer in getUser() and getName()
        String userId = "anonymous";
        Principal user = headerAccessor.getUser();
        if (user != null) {
            String name = user.getName();
            if (name != null) {
                userId = name;
                log.info("Principal set with userId: {}", userId);
            } else {
                log.warn("Principal found but name is null");
            }
        } else {
            log.warn("No principal found in connect event");
        }
        
        String sessionId = headerAccessor.getSessionId();
        log.info("Session ID: {}", sessionId);

        if (userId != null && sessionId != null) {
            log.info("User connected: {} (session: {})", userId, sessionId);
            onlineUsers.add(userId);

            // Register session in session tracker
            sessionTracker.registerSession(sessionId, userId);

            // Create a personal room for this user if not exists
            if (!userRooms.containsKey(userId)) {
                String roomId = "room_" + UUID.randomUUID().toString();
                userRooms.put(userId, roomId);
                log.info("Created personal room for userId {}: {}", userId, roomId);
            }

            // Broadcast that user is online
            broadcastUserStatus();

            // Send chat list to the user
            sendChatListToUser(userId);
            log.info("=== WEBSOCKET CONNECT PROCESSING COMPLETE ===");
        } else {
            log.error("Invalid connection: userId or sessionId is null. userId: {}, sessionId: {}", userId, sessionId);
        }
    }

    /**
     * Called when a user disconnects from WebSocket
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        // First try to get username from session tracker
        String username = null;
        if (sessionId != null) {
            username = sessionTracker.removeSession(sessionId);
        }

        // If not found in tracker, try to get from the message
        if (username == null) {
            username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;
        }

        if (username != null) {
            log.info("User disconnected: {} (session: {})", username, sessionId);
            onlineUsers.remove(username);

            // Remove user from all room subscriptions
            String roomId = userRooms.get(username);
            if (roomId != null && roomSubscribers.containsKey(roomId)) {
                roomSubscribers.get(roomId).remove(username);
                if (roomSubscribers.get(roomId).isEmpty()) {
                    roomSubscribers.remove(roomId);
                }
            }

            // Notify all contacts of this user about their offline status
            notifyUserStatusToContacts(username, false);

            // Broadcast general status update
            broadcastUserStatus();
        }
    }

    /**
     * Broadcast current online users to everyone
     */
    private void broadcastUserStatus() {
        ChatMessage message = new ChatMessage();
        message.setType(ChatMessage.MessageType.USER_LIST);
        message.setContent(String.join(",", onlineUsers));
        message.setTimestamp(LocalDateTime.now());

        messagingTemplate.convertAndSend("/topic/public", message);
    }

    /**
     * Deliver a message to all participants in a chat
     * Currently not used after removal of redundant endpoint, but kept for potential future use
     * 
     * @param chatId  The ID of the chat
     * @param message The message to deliver
     */
    // Retain this helper method as it could be useful for future implementations
    private void deliverMessageToChat(String chatId, Object message) {
        Chat chat = chatService.getChatById(chatId);
        if (chat != null) {
            log.info("Delivering message to chat: {} with participants: {}", chatId, chat.getParticipants());
            for (String participantId : chat.getParticipants()) {
                // Only send to online users
                log.info(chatId + " - Sending message to participant: " + participantId + ", Online: " + onlineUsers.contains(participantId));
                if (onlineUsers.contains(participantId)) {
                    log.info("Sending message to queue /user/{}/queue/messages", participantId);
                    messagingTemplate.convertAndSendToUser(
                            participantId,
                            "/queue/messages",
                            message);
                }
                }
            }
        }
    

    /**
     * Check if a user is currently online
     * 
     * @param profileId The profile ID to check
     * @return true if the user is online, false otherwise
     */
    public boolean isUserOnline(String profileId) {
        return onlineUsers.contains(profileId);
    }

    /**
     * Get the room ID for a specific user
     * 
     * @param profileId The profile ID
     * @return The room ID or null if the user doesn't have a room
     */
    public String getUserRoomId(String profileId) {
        return userRooms.get(profileId);
    }

    /**
     * Handle WebSocket subscription events
     * This is called when a user subscribes to a topic/queue
     */
    @EventListener
    public void handleWebSocketSubscribeListener(SessionSubscribeEvent event) {
        SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String username = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;
        String destination = headerAccessor.getDestination();

        if (username != null && destination != null) {
            log.debug("User {} subscribed to {}", username, destination);

            // Check if this is a subscription to a room
            if (destination.startsWith("/user/") && destination.contains("/queue/messages")) {
                // User is subscribing to their message queue
                String roomId = userRooms.get(username);
                if (roomId != null) {
                    // Keep track of the subscription
                    roomSubscribers.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(username);
                    log.debug("User {} added to room subscribers for room {}", username, roomId);

                    // Notify other users in chats about this user's online status
                    notifyUserStatusToContacts(username, true);

                    // Send unread messages to the user
                    sendUnreadMessagesToUser(username);

                    // Send chat list to the user
                    sendChatListToUser(username);
                }
            }
        }
    }

    /**
     * Notify all contacts of a user about their online status
     * 
     * @param profileId   The user whose status changed
     * @param isOnline Whether the user is online or not
     */
    private void notifyUserStatusToContacts(String profileId, boolean isOnline) {
        // Find all chats where this user is a participant
        List<Chat> userChats = chatService.findChatsByParticipant(profileId);

        if (userChats.isEmpty()) {
            return;
        }

        // Create a status update message
        Map<String, Object> statusUpdate = new HashMap<>();
        statusUpdate.put("type", "USER_STATUS");
        statusUpdate.put("profileId", profileId);
        statusUpdate.put("online", isOnline);
        statusUpdate.put("timestamp", LocalDateTime.now());

        // Notify all participants in those chats
        for (Chat chat : userChats) {
            for (String participantId : chat.getParticipants()) {
                // Don't send the notification to the user themselves
                if (!participantId.equals(profileId) && onlineUsers.contains(participantId)) {
                    messagingTemplate.convertAndSendToUser(
                            participantId,
                            "/queue/messages",
                            statusUpdate);
                }
            }
        }
    }

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload byte[] payload, SimpMessageHeaderAccessor headerAccessor) {
        log.debug("[sendMessage] Received byte[] payload of length: {}", payload.length);
        String base64Payload = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
        log.debug("[sendMessage] Base64-encoded string from bytes: {}", base64Payload);
        
        Message message;
        try {
            // First decode the Base64 string to get the actual JSON
            String jsonPayload;
            // Check if the received string is a Base64-encoded JSON (it may have quotes around it)
            if (base64Payload.startsWith("\"") && base64Payload.endsWith("\"")) {
                // Remove the surrounding quotes
                base64Payload = base64Payload.substring(1, base64Payload.length() - 1);
            }
            
            // Decode Base64 to get the actual JSON string
            byte[] decodedBytes = java.util.Base64.getDecoder().decode(base64Payload);
            jsonPayload = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8);
            log.debug("[sendMessage] Decoded JSON string: {}", jsonPayload);
            
            // Parse the JSON string to Message object
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules(); // For handling Java 8 date/time
            message = objectMapper.readValue(jsonPayload, Message.class);
            log.debug("[sendMessage] Successfully parsed message: {}", message);
            log.info("[sendMessage] Parsed Message clientTempId: {}", message.getClientTempId());
        } catch (Exception e) {
            log.error("[sendMessage] Failed to parse JSON: {}", e.getMessage(), e);
            return;
        }
        
        // Simple validation
        String senderId = message.getSenderId();
        if (senderId == null || senderId.isEmpty()) {
            log.error("[sendMessage] Sender ID is missing in the message");
            return;
        }
        
        String chatId = message.getChatId();
        if (chatId == null) {
            log.error("[sendMessage] chatId is null. Cannot process message.");
            return;
        }
        Chat chat = chatService.getChatById(chatId);
        if (chat == null) {
            log.error("[sendMessage] Chat with ID {} not found.", chatId);
            return;
        }
        chatService.saveMessage(message);
        chatService.updateChatPreview(chatId, message.getContent());

        // Extra debugging
        log.info("[sendMessage] Message clientTempId before conversion: {}", message.getClientTempId());

        // Convert Message to ChatMessage before sending
        ChatMessage chatMessageToSend = new ChatMessage();
        chatMessageToSend.setId(message.getMessageId());
        chatMessageToSend.setChatId(message.getChatId());
        chatMessageToSend.setSenderId(message.getSenderId());
        chatMessageToSend.setContent(message.getContent());
        chatMessageToSend.setTimestamp(message.getTimestamp());
        chatMessageToSend.setClientTempId(message.getClientTempId()); // Copy clientTempId for message deduplication
        
        // Extra debugging
        log.info("[sendMessage] ChatMessage clientTempId after conversion: {}", chatMessageToSend.getClientTempId());

        // Determine ChatMessage.MessageType based on Message.messageType
        ChatMessage.MessageType targetType = ChatMessage.MessageType.MESSAGE; // Default
        if (message.getMessageType() != null) {
            switch (message.getMessageType().toUpperCase()) {
                case "TEXT":
                    targetType = ChatMessage.MessageType.MESSAGE;
                    break;
                case "ENCRYPTED_CHAT":
                    targetType = ChatMessage.MessageType.ENCRYPTED_CHAT;
                    break;
                default:
                    log.warn("[sendMessage] Unhandled Message.messageType '{}' during conversion. Defaulting to MESSAGE.", message.getMessageType());
                    targetType = ChatMessage.MessageType.MESSAGE;
                    break;
            }
        }
        chatMessageToSend.setType(targetType);

        // Deliver to all participants in the chat
        for (String participantId : chat.getParticipants()) {
            String participantRoom = userRooms.get(participantId);
            if (participantRoom != null) {
                log.debug("[sendMessage] Sending ChatMessage to participant {} in room {}", participantId, participantRoom);
                messagingTemplate.convertAndSendToUser(participantId, "/queue/messages", chatMessageToSend); // Send ChatMessage
            }
        }
    }

    @MessageMapping("/chat.keyExchange")
    public void handleKeyExchange(@Payload ChatMessage chatMessage) {
        log.debug("Received key exchange from {} to {}",
                chatMessage.getSenderId(),
                chatMessage.getChatId());

        String senderId = chatMessage.getSenderId();
        String recipientId = chatMessage.getChatId();

        if (senderId == null || recipientId == null) {
            log.error("Sender ID or Recipient ID is null. Cannot process key exchange.");
            return;
        }

        List<Chat> existingChats = chatService.findChatsBetweenUsers(senderId, recipientId);
        Chat chat = existingChats.isEmpty() ? chatService.createChatWithUser(senderId, recipientId) : existingChats.get(0);

        chatMessage.setChatId(chat.getChatId());
        messagingTemplate.convertAndSendToUser(recipientId, "/queue/keyExchange", chatMessage);
    }

    @MessageMapping("/chat.createChat")
    public void createChat(@Payload Map<String, Object> payload) {
        String ownerId = (String) payload.get("ownerId");
        String targetUserId = (String) payload.get("targetProfileId");

        log.info("Creating chat between {} and {}", ownerId, targetUserId);

        try {
            // Check if chat already exists between these users
            List<Chat> existingChats = chatService.findChatsBetweenUsers(ownerId, targetUserId);
            Chat chat;

            if (existingChats.isEmpty()) {
                // Create new chat
                chat = chatService.createChatWithUser(ownerId, targetUserId);
                log.info("Created new chat with ID: {} between {} and {}", chat.getChatId(), ownerId, targetUserId);
            } else {
                // Use existing chat
                chat = existingChats.get(0);
                log.info("Using existing chat with ID: {} between {} and {}", chat.getChatId(), ownerId, targetUserId);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("type", "CHAT_CREATED");
            response.put("chatId", chat.getChatId());
            response.put("participants", chat.getParticipants());
            response.put("ownerId", ownerId);
            response.put("targetUserId", targetUserId);

            log.info("Sending chat creation notification to participants: {}", chat.getParticipants());
            
            // Send individual messages to each participant directly
            for (String participantId : chat.getParticipants()) {
                if (onlineUsers.contains(participantId)) {
                    log.info("Sending direct chat creation notification to: {}", participantId);
                    messagingTemplate.convertAndSendToUser(
                        participantId,
                        "/queue/messages",
                        response
                    );
                    
                    // Also send to alternative queue for redundancy
                    messagingTemplate.convertAndSendToUser(
                        participantId,
                        "/queue/chat.updates",
                        response
                    );
                }
            }
            
            // Also publish to topic to ensure delivery
            messagingTemplate.convertAndSend("/topic/chat.events", response);
            
        } catch (Exception e) {
            log.error("Error creating chat", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "ERROR");
            errorResponse.put("error", "Failed to create chat: " + e.getMessage());

            messagingTemplate.convertAndSendToUser(
                    ownerId,
                    "/queue/messages",
                    errorResponse);
        }
    }

    // @MessageMapping("/chat.sendMessage") endpoint removed as it was redundant with "/chat.send"
    // The frontend only uses the "/chat.send" endpoint

    /**
     * Check if a user is actively subscribed to a specific chat room
     * 
     * @param profileId The profile ID to check
     * @param chatId The chat ID to check
     * @return true if the user is subscribed to the chat room, false otherwise
     */
    public boolean isUserSubscribedToChat(String profileId, String chatId) {
        // First check if user is online
        if (!onlineUsers.contains(profileId)) {
            return false;
        }

        // Check if user has a room
        String roomId = userRooms.get(profileId);
        if (roomId == null) {
            return false;
        }

        // Check if user is subscribed to their room
        Set<String> subscribers = roomSubscribers.get(roomId);
        if (subscribers == null || !subscribers.contains(profileId)) {
            return false;
        }

        // Check if user is participant in the chat
        Chat chat = chatService.getChatById(chatId);
        if (chat == null) {
            return false;
        }

        return chat.getParticipants().contains(profileId);
    }

    /**
     * Get all actively subscribed participants for a chat
     * 
     * @param chatId The chat ID
     * @return A list of user IDs who are actively subscribed to the chat
     */
    public Set<String> getActiveParticipants(String chatId) {
        Set<String> activeParticipants = new HashSet<>();
        Chat chat = chatService.getChatById(chatId);

        if (chat != null) {
            for (String participantId : chat.getParticipants()) {
                if (isUserSubscribedToChat(participantId, chatId)) {
                    activeParticipants.add(participantId);
                }
            }
        }

        return activeParticipants;
    }

    /**
     * Send unread chat messages to a user upon connection
     * This method is called when a user subscribes to their message queue
     * 
     * @param profileId The profile ID to send unread messages to
     */
    private void sendUnreadMessagesToUser(String profileId) {
        // Get all chats for this user
        List<Chat> userChats = chatService.findChatsByParticipant(profileId);

        for (Chat chat : userChats) {
            // For each chat, get messages that were sent while the user was offline
            // The logic for unread messages would need to be implemented in the service
            // layer
            List<Message> unreadMessages = chatService.getUnreadMessagesForUser(chat.getChatId(), profileId);

            if (unreadMessages != null && !unreadMessages.isEmpty()) {
                log.debug("Sending {} unread messages to user {} for chat {}",
                        unreadMessages.size(), profileId, chat.getChatId());

                for (Message message : unreadMessages) {
                    // Convert Message to ChatMessage
                    ChatMessage chatMessageToSend = new ChatMessage();
                    chatMessageToSend.setId(message.getMessageId());
                    chatMessageToSend.setChatId(message.getChatId());
                    chatMessageToSend.setSenderId(message.getSenderId());
                    chatMessageToSend.setContent(message.getContent());
                    chatMessageToSend.setTimestamp(message.getTimestamp());

                    ChatMessage.MessageType targetType = ChatMessage.MessageType.MESSAGE; // Default
                    if (message.getMessageType() != null) {
                        switch (message.getMessageType().toUpperCase()) {
                            case "TEXT":
                                targetType = ChatMessage.MessageType.MESSAGE;
                                break;
                            case "ENCRYPTED_CHAT":
                                targetType = ChatMessage.MessageType.ENCRYPTED_CHAT;
                                break;
                            default:
                                log.warn("[sendUnreadMessagesToUser] Unhandled Message.messageType '{}' during conversion. Defaulting to MESSAGE.", message.getMessageType());
                                targetType = ChatMessage.MessageType.MESSAGE;
                                break;
                        }
                    }
                    chatMessageToSend.setType(targetType);

                    Map<String, Object> response = new HashMap<>();
                    response.put("type", "MESSAGE"); // This outer "type" is for client-side routing
                    response.put("message", chatMessageToSend); // Embed ChatMessage object
                    response.put("chatId", chat.getChatId());
                    response.put("unread", true);

                    messagingTemplate.convertAndSendToUser(
                            profileId,
                            "/queue/messages",
                            response);
                }
            }
        }
    }

    /**
     * Send the list of chats to a user
     * This method is called when a user connects to get their chat list
     * 
     * @param profileId The profile ID to send chat list to
     */
    private void sendChatListToUser(String profileId) {
        List<Chat> userChats = chatService.findChatsByParticipant(profileId);
        if (!userChats.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("type", "CHAT_LIST");
            response.put("chats", userChats);

            // Send to regular message queue for backward compatibility
            messagingTemplate.convertAndSendToUser(
                    profileId,
                    "/queue/messages",
                    response);
                    
            // Also send to specific chat.list queue that frontend is expecting
            messagingTemplate.convertAndSendToUser(
                    profileId,
                    "/queue/chat.list",
                    response);
                    
            log.info("Sent chat list with {} chats to user {} on both message queues", userChats.size(), profileId);
        } else {
            log.info("No chats found for user {}", profileId);
        }
    }
    
    /**
     * Handle incoming chat list request from String payload
     * This method is called by ChatRequestEndpoint to handle serialized requests
     * 
     * @param jsonPayload The JSON payload as a String
     */
    public void handleChatListRequest(String jsonPayload) {
        try {
            // Parse the JSON payload manually
            ObjectMapper objectMapper = new ObjectMapper();
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = objectMapper.readValue(jsonPayload, Map.class);
            
            // Extract username
            String Id = (String) payload.get("profileId");
            log.info("Processing chat list request for user: {}", Id);
            
            if (Id != null) {
                // Call the existing method to send the chat list
                sendChatListToUser(Id);
            } else {
                log.error("Chat list request received with no username");
            }
        } catch (Exception e) {
            log.error("Error handling chat list request", e);
        }
    }

    /**
     * Get message history for a specific chat
     * This is used when the client requests message history for a chat
     */
    @MessageMapping("/chat.getHistory")
    public void getChatHistory(@Payload Map<String, Object> payload) {
        String chatId = (String) payload.get("chatId");
        String requesterId = (String) payload.get("requesterId");
        Integer limit = payload.get("limit") instanceof Number ? ((Number) payload.get("limit")).intValue() : 50;
        Integer offset = payload.get("offset") instanceof Number ? ((Number) payload.get("offset")).intValue() : 0;

        log.debug("User {} requested history for chat {}, limit: {}, offset: {}",
                requesterId, chatId, limit, offset);

        try {
            // Check if user is authorized to get this chat's messages
            Chat chat = chatService.getChatById(chatId);
            if (chat == null || !chat.getParticipants().contains(requesterId)) {
                throw new RuntimeException("Unauthorized access to chat history");
            }

            // Get messages for this chat
            List<Message> messages = chatService.getMessagesByChatId(chatId);

            // Sort messages by timestamp (newest first) and apply pagination
            messages.sort((m1, m2) -> m2.getTimestamp().compareTo(m1.getTimestamp()));

            // Apply pagination
            int fromIndex = Math.min(offset, messages.size());
            int toIndex = Math.min(offset + limit, messages.size());
            List<Message> paginatedMessages = fromIndex < toIndex ? messages.subList(fromIndex, toIndex)
                    : new ArrayList<>();

            // Convert List<Message> to List<ChatMessage>
            List<ChatMessage> chatMessagesToSend = new ArrayList<>();
            for (Message msg : paginatedMessages) {
                ChatMessage chatMsg = new ChatMessage();
                chatMsg.setId(msg.getMessageId());
                chatMsg.setChatId(msg.getChatId());
                chatMsg.setSenderId(msg.getSenderId());
                chatMsg.setContent(msg.getContent());
                chatMsg.setTimestamp(msg.getTimestamp());

                ChatMessage.MessageType targetType = ChatMessage.MessageType.MESSAGE; // Default
                if (msg.getMessageType() != null) {
                    switch (msg.getMessageType().toUpperCase()) {
                        case "TEXT":
                            targetType = ChatMessage.MessageType.MESSAGE;
                            break;
                        case "ENCRYPTED_CHAT":
                            targetType = ChatMessage.MessageType.ENCRYPTED_CHAT;
                            break;
                        default:
                            log.warn("[getChatHistory] Unhandled Message.messageType '{}' during conversion. Defaulting to MESSAGE.", msg.getMessageType());
                            targetType = ChatMessage.MessageType.MESSAGE;
                            break;
                    }
                }
                chatMsg.setType(targetType);
                chatMessagesToSend.add(chatMsg);
            }

            // Send back to requester
            Map<String, Object> response = new HashMap<>();
            response.put("type", "CHAT_HISTORY");
            response.put("chatId", chatId);
            response.put("messages", chatMessagesToSend); // Send List<ChatMessage>
            response.put("offset", offset);
            response.put("limit", limit);
            response.put("total", messages.size());

            messagingTemplate.convertAndSendToUser(
                    requesterId,
                    "/queue/messages",
                    response);

            log.debug("Sent {} messages to user {} for chat {}",
                    paginatedMessages.size(), requesterId, chatId);

        } catch (Exception e) {
            log.error("Error retrieving chat history", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "ERROR");
            errorResponse.put("error", "Failed to get chat history: " + e.getMessage());

            messagingTemplate.convertAndSendToUser(
                    requesterId,
                    "/queue/messages",
                    errorResponse);
        }
    }

    @MessageMapping("/chat.markRead")
    public void markMessagesAsRead(@Payload Map<String, Object> payload) {
        String chatId = (String) payload.get("chatId");
        String profileId = (String) payload.get("profileId");
        @SuppressWarnings("unchecked")
        List<String> messageIds = (List<String>) payload.get("messageIds");

        log.debug("Marking messages as read for user {} in chat {}: {}", profileId, chatId, messageIds);

        if (chatId == null || profileId == null || messageIds == null || messageIds.isEmpty()) {
            log.warn("Missing required parameters for marking messages as read");
            return;
        }

        try {
            chatService.markMessagesAsRead(chatId, profileId, messageIds);

            // Notify other chat participants about read status
            Chat chat = chatService.getChatById(chatId);
            if (chat != null) {
                Map<String, Object> response = new HashMap<>();
                response.put("type", "MESSAGES_READ");
                response.put("chatId", chatId);
                response.put("profileId", profileId);
                response.put("messageIds", messageIds);

                for (String participantId : chat.getParticipants()) {
                    // Don't send notification back to the same user who marked messages as read
                    if (!participantId.equals(profileId) && onlineUsers.contains(participantId)) {
                        messagingTemplate.convertAndSendToUser(
                                participantId,
                                "/queue/messages",
                                response);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error marking messages as read", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("type", "ERROR");
            errorResponse.put("error", "Failed to mark messages as read: " + e.getMessage());

            messagingTemplate.convertAndSendToUser(
                    profileId,
                    "/queue/messages",
                    errorResponse);
        }
    }
}
