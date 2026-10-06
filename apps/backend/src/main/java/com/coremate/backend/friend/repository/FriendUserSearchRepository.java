package com.coremate.backend.friend.repository;

import com.coremate.backend.user.entity.User;
import com.coremate.backend.user.entity.UserStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FriendUserSearchRepository extends JpaRepository<User, Long> {

    // 닉네임으로만 찾는다. 이메일까지 부분 일치로 찾으면 이메일 일부만 입력해 남의 닉네임과
    // 가입 여부를 알아낼 수 있다. 닉네임은 유일해서 이것만으로 특정인을 찾을 수 있다.
    @Query("""
            select u
            from User u
            where u.status = :status
              and u.userId <> :currentUserId
              and lower(u.nickname) like lower(concat('%', :keyword, '%'))
            order by u.nickname asc
            """)
    List<User> searchActiveUsers(
            @Param("currentUserId") Long currentUserId,
            @Param("keyword") String keyword,
            @Param("status") UserStatus status,
            Pageable pageable
    );
}
