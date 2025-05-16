package com.e2ee.chat.repository;

import com.e2ee.chat.model.UserProfile;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.Optional;

public interface UserProfileRepository extends MongoRepository<UserProfile, ObjectId> {
    Optional<UserProfile> findByUsername(String username);
    
    // Search for users by username or displayName containing the query string
    @Query("{ $or: [ { 'username': { $regex: ?0, $options: 'i' } }, { 'displayName': { $regex: ?0, $options: 'i' } } ] }")
    List<UserProfile> findByUsernameOrDisplayNameContainingIgnoreCase(String query);

    Optional<UserProfile> findById(ObjectId id);
}