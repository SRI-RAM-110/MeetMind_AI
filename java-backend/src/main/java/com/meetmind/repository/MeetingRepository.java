package com.meetmind.repository;

import com.meetmind.model.Meeting;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingRepository extends MongoRepository<Meeting, String> {
    List<Meeting> findAllByOrderByCreatedAtDesc();
    Optional<Meeting> findByNotesHash(String notesHash);
}
