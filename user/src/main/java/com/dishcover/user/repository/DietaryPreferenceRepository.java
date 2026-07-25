package com.dishcover.user.repository;

import com.dishcover.user.entity.DietaryPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DietaryPreferenceRepository extends JpaRepository<DietaryPreference, Long> {

    List<DietaryPreference> findByUserId(Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}
