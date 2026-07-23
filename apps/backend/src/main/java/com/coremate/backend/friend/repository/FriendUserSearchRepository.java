package com.coremate.backend.friend.repository;

import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendUserSearchRepository extends JpaRepository<User, Long> {

    @Query("""
            select u
            from User u
            where u.status = :status
              and u.userId <> :currentUserId
              and (
                    lower(u.nickname) like lower(concat('%', :keyword, '%'))
                 or lower(u.email) like lower(concat('%', :keyword, '%'))
              )
            order by u.nickname asc
            """)
    List<User> searchActiveUsers(
            @Param("currentUserId") Long currentUserId,
            @Param("keyword") String keyword,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}
