package com.emar.order_app.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    @Query(value = """
        select
          a.id,
          a.entity_type,
          a.entity_id,
          a.action,
          (case
             when pg_typeof(a.actor_username) = 'bytea'::regtype
               then convert_from(a.actor_username::bytea, 'UTF8')
             else a.actor_username::text
           end) as actor_username,
          a.message,
          a.metadata::text as metadata,
          a.created_at
        from audit_log a
        where (cast(:entityType as text) is null or a.entity_type = cast(:entityType as text))
          and (cast(:action as text) is null or a.action = cast(:action as text))
          and (
                cast(:actor as text) is null
                or lower(
                    (case
                       when pg_typeof(a.actor_username) = 'bytea'::regtype
                         then convert_from(a.actor_username::bytea, 'UTF8')
                       else a.actor_username::text
                     end)
                ) like lower('%' || cast(:actor as text) || '%')
              )
          and (cast(:dateFrom as timestamptz) is null or a.created_at >= cast(:dateFrom as timestamptz))
          and (cast(:dateTo as timestamptz) is null or a.created_at <= cast(:dateTo as timestamptz))
        order by a.created_at desc
        limit :limit offset :offset
        """, nativeQuery = true)
    List<Object[]> searchNative(
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("actor") String actor,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
        select count(*)
        from audit_log a
        where (cast(:entityType as text) is null or a.entity_type = cast(:entityType as text))
          and (cast(:action as text) is null or a.action = cast(:action as text))
          and (
                cast(:actor as text) is null
                or lower(
                    (case
                       when pg_typeof(a.actor_username) = 'bytea'::regtype
                         then convert_from(a.actor_username::bytea, 'UTF8')
                       else a.actor_username::text
                     end)
                ) like lower('%' || cast(:actor as text) || '%')
              )
          and (cast(:dateFrom as timestamptz) is null or a.created_at >= cast(:dateFrom as timestamptz))
          and (cast(:dateTo as timestamptz) is null or a.created_at <= cast(:dateTo as timestamptz))
        """, nativeQuery = true)
    long countNative(
            @Param("entityType") String entityType,
            @Param("action") String action,
            @Param("actor") String actor,
            @Param("dateFrom") Instant dateFrom,
            @Param("dateTo") Instant dateTo
    );
}
