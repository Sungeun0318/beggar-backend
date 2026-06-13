package com.beggar.api.repository.admin;

import com.beggar.api.entity.AdminActionLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminActionLogRepository extends JpaRepository<AdminActionLog, Long> {

    @Query("""
            SELECT l
              FROM AdminActionLog l
             WHERE (:adminUsername = '' OR LOWER(l.adminUsername) LIKE LOWER(CONCAT('%', :adminUsername, '%')))
               AND (:action = '' OR l.action = :action)
               AND (:targetType = '' OR l.targetType = :targetType)
               AND (:keyword = ''
                    OR LOWER(COALESCE(l.targetId, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(COALESCE(l.message, '')) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    Page<AdminActionLog> searchLogs(
            @Param("adminUsername") String adminUsername,
            @Param("action") String action,
            @Param("targetType") String targetType,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
