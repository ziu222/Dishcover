package com.dishcover.payment.repository;

import com.dishcover.payment.entity.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Truy cập bảng giá gói. */
public interface PlanRepository extends JpaRepository<Plan, String> {

    /** Bảng giá hiển thị cho người dùng — chỉ gói còn bán. */
    List<Plan> findByActiveTrue();
}
