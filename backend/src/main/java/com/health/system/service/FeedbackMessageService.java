package com.health.system.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.health.system.dto.AdminFeedbackReplyDTO;
import com.health.system.dto.FeedbackCreateDTO;
import com.health.system.dto.FeedbackQueryDTO;
import com.health.system.entity.FeedbackMessage;

public interface FeedbackMessageService {
    void createFeedback(String username, FeedbackCreateDTO dto);

    List<FeedbackMessage> listMine(String username);

    Map<String, Object> listMinePaged(String username, LocalDateTime startTime, LocalDateTime endTime, int pageNo, int pageSize);

    List<FeedbackMessage> listAll(FeedbackQueryDTO query);

    Map<String, Object> listAllPaged(FeedbackQueryDTO query);

    List<FeedbackMessage> listAllForExport(FeedbackQueryDTO query);

    Map<String, Object> getAdminStats();

    long countPending();

    long countUnreadReply(String username);

    void markMineReplyRead(String username);

    void updateStatus(Long id, Integer status);

    Map<String, Object> batchUpdateStatus(List<Long> ids, Integer status);

    Map<String, Object> batchUpdateStatusByFilter(FeedbackQueryDTO query, Integer targetStatus);

    void replyFeedback(AdminFeedbackReplyDTO dto);
}
