package com.dishcover.user.repository;

import com.dishcover.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Repository JPA cho entity {@link User}, thao tác trực tiếp bảng user_service.users. */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Tìm user theo email đăng nhập.
     *
     * @param email email cần tìm (đã chuẩn hóa lowercase ở tầng service)
     * @return user tương ứng, rỗng nếu không tồn tại
     */
    Optional<User> findByEmail(String email);

    /**
     * Kiểm tra email đã được đăng ký chưa.
     *
     * @param email email cần kiểm tra
     * @return true nếu email đã tồn tại trong hệ thống
     */
    boolean existsByEmail(String email);
}
