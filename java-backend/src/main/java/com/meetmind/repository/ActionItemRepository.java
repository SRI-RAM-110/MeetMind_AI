package com.meetmind.repository;

import com.meetmind.model.ActionItem;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActionItemRepository extends MongoRepository<ActionItem, String> {
    List<ActionItem> findByMeetingId(String meetingId);
    void deleteByMeetingId(String meetingId);
}
