package com.meetmind.repository;

import com.meetmind.model.FollowUpPlan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface FollowUpPlanRepository extends MongoRepository<FollowUpPlan, String> {
    Optional<FollowUpPlan> findByMeetingId(String meetingId);
    void deleteByMeetingId(String meetingId);
}
