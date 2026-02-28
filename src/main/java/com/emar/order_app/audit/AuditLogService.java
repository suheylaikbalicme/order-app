package com.emar.order_app.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AuditLogService {

    private final AuditLogRepository repo;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository repo, ObjectMapper objectMapper) {
        this.repo = repo;
        this.objectMapper = objectMapper;
    }


    public void safeLog(AuditEntityType entityType,
                        Long entityId,
                        AuditAction action,
                        String actorUsername,
                        String message,
                        Map<String, Object> metadata) {
        try {
            AuditLogEntity e = new AuditLogEntity();
            e.setEntityType(entityType);
            e.setEntityId(entityId);
            e.setAction(action);
            e.setActorUsername(actorUsername);
            e.setMessage(message);
            e.setCreatedAt(Instant.now());
            if (metadata != null && !metadata.isEmpty()) {
                e.setMetadata(objectMapper.writeValueAsString(metadata));
            }
            repo.save(e);
        } catch (Exception ignore) {
            // intentionally swallow
        }
    }


    public void log(String actorUsername,
                    AuditEntityType entityType,
                    Long entityId,
                    AuditAction action,
                    String message,
                    Map<String, Object> metadata) {
        safeLog(entityType, entityId, action, actorUsername, message, metadata);
    }

    public Page<AuditLogEntity> search(AuditEntityType entityType,
                                       AuditAction action,
                                       String actor,
                                       Instant dateFrom,
                                       Instant dateTo,
                                       Pageable pageable) {
        int limit = pageable.getPageSize();
        int offset = (int) pageable.getOffset();

        String et = (entityType == null ? null : entityType.name());
        String ac = (action == null ? null : action.name());

        long total = repo.countNative(et, ac, actor, dateFrom, dateTo);
        List<Object[]> rows = repo.searchNative(et, ac, actor, dateFrom, dateTo, limit, offset);

        List<AuditLogEntity> content = new ArrayList<>();
        for (Object[] r : rows) {
            AuditLogEntity e = new AuditLogEntity();
            if (r[0] != null) e.setId(((Number) r[0]).longValue());
            if (r[1] != null) e.setEntityType(AuditEntityType.valueOf((String) r[1]));
            if (r[2] != null) e.setEntityId(((Number) r[2]).longValue());
            if (r[3] != null) e.setAction(AuditAction.valueOf((String) r[3]));
            e.setActorUsername((String) r[4]);
            e.setMessage((String) r[5]);
            e.setMetadata((String) r[6]);
            if (r[7] != null) {
                if (r[7] instanceof Instant i) {
                    e.setCreatedAt(i);
                } else if (r[7] instanceof Timestamp ts) {
                    e.setCreatedAt(ts.toInstant());
                } else if (r[7] instanceof java.util.Date d) {
                    e.setCreatedAt(d.toInstant());
                }
            }
            content.add(e);
        }

        return new PageImpl<>(content, pageable, total);
    }

    public static Map<String, Object> meta(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (kv == null) return m;
        for (int i = 0; i + 1 < kv.length; i += 2) {
            Object k = kv[i];
            Object v = kv[i + 1];
            if (k == null) continue;
            m.put(String.valueOf(k), v);
        }
        return m;
    }
}
