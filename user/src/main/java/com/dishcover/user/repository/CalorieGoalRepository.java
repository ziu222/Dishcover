package com.dishcover.user.repository;

import com.dishcover.user.entity.CalorieGoal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository JPA cho entity {@link CalorieGoal}, thao tác trực tiếp bảng user_service.calorie_goals. */
public interface CalorieGoalRepository extends JpaRepository<CalorieGoal, Long> {

    /**
     * Lấy mục tiêu calo/macro hiện tại của một user, nếu có.
     *
     * @param userId id user cần lấy
     * @return mục tiêu hiện tại, rỗng nếu user chưa đặt mục tiêu
     */
    Optional<CalorieGoal> findByUserId(Long userId);
}
