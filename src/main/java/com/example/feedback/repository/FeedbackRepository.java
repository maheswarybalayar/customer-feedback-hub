package com.example.feedback.repository;

import com.example.feedback.entity.FeedbackEntity;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, String> {
	
	List<FeedbackEntity> findByProductIgnoreCase(String product);

}