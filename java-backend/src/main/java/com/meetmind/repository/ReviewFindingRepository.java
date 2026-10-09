package com.meetmind.repository;

import com.meetmind.model.ReviewFinding;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReviewFindingRepository extends MongoRepository<ReviewFinding, String> {
    List<ReviewFinding> findByMeetingId(String meetingId);
    void deleteByMeetingId(String meetingId);
}
