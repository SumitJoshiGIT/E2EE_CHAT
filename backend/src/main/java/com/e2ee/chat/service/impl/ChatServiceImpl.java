package com.e2ee.chat.service.impl;

import com.e2ee.chat.model.User;
import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.repository.ChatRepository;
import com.e2ee.chat.repository.MessageRepository;
import com.e2ee.chat.repository.UserProfileRepository;
import com.e2ee.chat.repository.UserRepository;
import com.e2ee.chat.service.ChatService;
import com.e2ee.chat.model.Chat;
import com.e2ee.chat.model.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    @Override
    public Message processMessage(Message message) {
        // Encrypt the message content (E2EE encryption logic can be added here)
        message.setContent(encryptMessage(message.getContent()));
        return message;
    }

    @Override
    public void saveMessage(Message message) {
        if (message.getChatId() == null) {
            System.err.println("[saveMessage] chatId is null. Cannot save message.");
            return;
        }
        messageRepository.save(message);
        if ("TEXT".equals(message.getMessageType()) || "ENCRYPTED_CHAT".equals(message.getMessageType())) {
            if (message.getChatId() != null) {
                updateChatPreview(message.getChatId(), message.getContent());
            }
        }
    }

    @Override
    public List<Chat> getChatsByOwner(String ownerId) {
        return chatRepository.findByOwnerId(ownerId);
    }

    @Override
    public List<Message> getMessagesByChatId(String chatId) {
        return messageRepository.findByChatIdOrderByTimestampAsc(chatId);
    }

    @Override
    public Chat createChat(String ownerId, String targetUserId, String targetPublicKey) {
        Chat chat = new Chat();
        chat.setOwnerId(ownerId);
        chat.setTargetUserId(targetUserId);
        chat.setTargetPublicKey(targetPublicKey);
        return chatRepository.save(chat);
    }

    // New methods implementation

    @Override
    public List<UserProfile> searchUsers(String query, int limit) {
        // Use the repository method to perform a more efficient search
        List<UserProfile> results = userProfileRepository.findByUsernameOrDisplayNameContainingIgnoreCase(query);
        
        // Limit the results
        if (results.size() > limit) {
            return results.subList(0, limit);
        }
        return results;
    }

    @Override
    @Transactional
    public Chat createChatWithUser(String ownerId, String targetUserId) {
        // Check if a chat already exists between these two user IDs
        List<Chat> existingChats = chatRepository.findByParticipantsContainingBoth(ownerId, targetUserId);
        if (!existingChats.isEmpty()) {
            return existingChats.get(0);
        }

        // Create a new chat with the target user ID
        Chat chat = new Chat();
        List<String> participants = new ArrayList<>();
        participants.add(ownerId);
        participants.add(targetUserId);

        chat.setParticipants(participants);
        chat.setOwnerId(ownerId);
        chat.setTargetUserId(targetUserId);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());

        // Save the chat
        Chat savedChat = chatRepository.save(chat);

        // Update owner's chat list
        Optional<User> ownerOptional = userRepository.findById(ownerId);
        if (ownerOptional.isPresent()) {
            User owner = ownerOptional.get();
            List<String> chats = owner.getChats();
            if (chats == null) {
                chats = new ArrayList<>();
            }
            chats.add(savedChat.getChatId());
            owner.setChats(chats);
            userRepository.save(owner);
        }

        return savedChat;
    }

    @Override
    @Transactional
    public Message sendMessageToChat(String chatId, String senderId, String content, String messageType) {
        Chat chat = getChatById(chatId);
        if (chat == null) {
            throw new RuntimeException("Chat not found");
        }
        
        // Create and save the message
        Message message = new Message();
        message.setMessageId(UUID.randomUUID().toString());
        message.setChatId(chatId);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setMessageType(messageType);
        message.setTimestamp(LocalDateTime.now());
        
        Message savedMessage = messageRepository.save(message);
        
        // Update chat with message ID and last message preview
        List<String> messageIds = chat.getMessageIds();
        if (messageIds == null) {
            messageIds = new ArrayList<>();
        }
        messageIds.add(savedMessage.getMessageId());
        chat.setMessageIds(messageIds);
        
        // Update preview and timestamp
        String preview = content;
        if ("FILE".equals(messageType)) {
            preview = "[File] " + (message.getFileName() != null ? message.getFileName() : "Attachment");
        } else if ("IMAGE".equals(messageType)) {
            preview = "[Image]";
        }
        
        chat.setLastMessagePreview(preview.length() > 50 ? preview.substring(0, 47) + "..." : preview);
        chat.setUpdatedAt(LocalDateTime.now());
        chatRepository.save(chat);
        
        return savedMessage;
    }

    @Override
    public Chat getChatById(String chatId) {
        return chatRepository.findById(chatId).orElse(null);
    }

    @Override
    public void updateChatPreview(String chatId, String previewText) {
        Optional<Chat> chatOptional = chatRepository.findById(chatId);
        if (chatOptional.isPresent()) {
            Chat chat = chatOptional.get();
            chat.setLastMessagePreview(previewText.length() > 50 ? previewText.substring(0, 47) + "..." : previewText);
            chat.setUpdatedAt(LocalDateTime.now());
            chatRepository.save(chat);
        }
    }

    private String encryptMessage(String content) {
        // This method is just a placeholder. In a true E2EE system:
        // 1. Message is encrypted on the client side using recipient's public key
        // 2. Server never has access to the decryption keys
        // 3. Server only stores and forwards the encrypted messages
        
        // For this sample implementation, we're just marking the messages
        // as if they were encrypted. Real encryption is done on client.
        if (content != null && !content.startsWith("[Encrypted]")) {
            return "[Encrypted] " + content;
        }
        return content;
    }
    
    @Override
    public List<Chat> findChatsBetweenUsers(String user1, String user2) {
        // Search for chats where both users are participants
        return chatRepository.findByParticipantsContainingBoth(user1, user2);
    }
    
    @Override
    public List<Chat> findChatsByParticipant(String userId) {
        // Search for chats where user is a participant
        return chatRepository.findByParticipantsContaining(userId);
    }
    
    @Override
    public List<Message> getUnreadMessagesForUser(String chatId, String userId) {
        List<Message> allMessages = messageRepository.findByChatIdOrderByTimestampAsc(chatId);
        List<Message> unreadMessages = allMessages.stream()
            .filter(message -> !message.getReadBy().contains(userId))
            .collect(Collectors.toList());
        unreadMessages.sort(Comparator.comparing(Message::getTimestamp));
        return unreadMessages;
    }

    @Override
    @Transactional
    public void markMessagesAsRead(String chatId, String userId, List<String> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return;
        }
        
        // Verify that the user is actually a participant in the chat
        Chat chat = getChatById(chatId);
        if (chat == null) {
            throw new IllegalArgumentException("Chat not found with ID: " + chatId);
        }
        
        if (!chat.getParticipants().contains(userId)) {
            throw new IllegalArgumentException("User " + userId + " is not a participant in chat " + chatId);
        }
        
        try {
            for (String messageId : messageIds) {
                Optional<Message> messageOpt = messageRepository.findById(messageId);
                if (messageOpt.isPresent()) {
                    Message message = messageOpt.get();
                    // Validate that the message belongs to this chat
                    if (!message.getChatId().equals(chatId)) {
                        continue; // Skip messages that don't belong to this chat
                    }
                    
                    if (!message.getReadBy().contains(userId)) {
                        message.getReadBy().add(userId);
                        message.setStatus("READ");
                        messageRepository.save(message);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to mark messages as read", e);
        }
    }
}