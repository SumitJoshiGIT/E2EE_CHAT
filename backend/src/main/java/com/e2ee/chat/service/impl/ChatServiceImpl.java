package com.e2ee.chat.service.impl;

import com.e2ee.chat.model.ChatMessage;
import com.e2ee.chat.model.User;
import com.e2ee.chat.model.UserProfile;
import com.e2ee.chat.repository.ChatMessageRepository;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MessageRepository messageRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserRepository userRepository;

    @Override
    public ChatMessage processMessage(ChatMessage message) {
        // Encrypt the message content (E2EE encryption logic can be added here)
        message.setContent(encryptMessage(message.getContent()));
        return message;
    }

    @Override
    public void saveMessage(ChatMessage message) {
        // Save WebSocket ChatMessage to Mongo
        messageRepository.save(message);
    }

    @Override
    public List<Chat> getChatsByOwner(String ownerId) {
        return chatRepository.findByOwnerId(ownerId);
    }

    @Override
    public List<Message> getMessagesByChatId(String chatId) {
        return chatMessageRepository.findByChatId(chatId);
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
        // Check if a chat already exists between these two users
        List<Chat> existingChats = chatRepository.findByParticipantsContainingBoth(ownerId, targetUserId);
        if (!existingChats.isEmpty()) {
            // Return the existing chat
            return existingChats.get(0);
        }
        
        // Create a new chat with the target user
        Chat chat = new Chat();
        List<String> participants = new ArrayList<>();
        participants.add(ownerId);
        participants.add(targetUserId);
        
        chat.setParticipants(participants);
        chat.setOwnerId(ownerId);
        chat.setTargetUserId(targetUserId);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());
        
        // Get the target user profile to get public key
        Optional<UserProfile> targetProfile = userProfileRepository.findByUsername(targetUserId);
        if (targetProfile.isPresent()) {
            chat.setTargetPublicKey(targetProfile.get().getPublicKey());
        }
        
        Chat savedChat = chatRepository.save(chat);
        
        // Update user's chat list
        Optional<User> ownerOptional = userRepository.findByUsername(ownerId);
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
        
        // Update target user's chat list
        Optional<User> targetOptional = userRepository.findByUsername(targetUserId);
        if (targetOptional.isPresent()) {
            User target = targetOptional.get();
            List<String> chats = target.getChats();
            if (chats == null) {
                chats = new ArrayList<>();
            }
            chats.add(savedChat.getChatId());
            target.setChats(chats);
            userRepository.save(target);
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
        
        Message savedMessage = chatMessageRepository.save(message);
        
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
        // Placeholder for encryption logic
        // Replace this with actual encryption implementation
        return "[Encrypted] " + content;
    }
}