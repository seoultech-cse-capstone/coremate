package com.coremate.backend.reward.repository;

import com.coremate.backend.user.entity.User;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

// user 패키지 파일을 수정하지 않고 XP/레벨만 갱신하기 위한 리워드 전용 포트.
public interface RewardUserProgressRepository extends Repository<User, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update User u
            set u.totalXp = :totalXp,
                u.level = :level
            where u.userId = :userId
            """)
    int updateLearningProgress(
            @Param("userId") Long userId,
            @Param("totalXp") int totalXp,
            @Param("level") int level
    );
}
