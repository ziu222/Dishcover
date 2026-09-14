package com.dishcover.user.repository;

import com.dishcover.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
     * Danh sách cho khu quản trị, lọc theo email gần đúng (không phân biệt hoa thường).
     *
     * @param email chuỗi con của email; rỗng nghĩa là lấy tất cả
     * @param pageable phân trang + sắp xếp
     * @return trang user khớp
     */
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);

    /**
     * Kiểm tra email đã được đăng ký chưa.
     *
     * @param email email cần kiểm tra
     * @return true nếu email đã tồn tại trong hệ thống
     */
    boolean existsByEmail(String email);
}
