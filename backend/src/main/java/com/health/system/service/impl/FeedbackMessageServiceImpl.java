package com.health.system.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.common.CacheNames;
import com.health.system.dto.AdminFeedbackReplyDTO;
import com.health.system.dto.FeedbackCreateDTO;
import com.health.system.dto.FeedbackQueryDTO;
import com.health.system.entity.FeedbackMessage;
import com.health.system.entity.User;
import com.health.system.mapper.FeedbackMessageMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.FeedbackMessageService;

@Service
public class FeedbackMessageServiceImpl implements FeedbackMessageService {

    private final FeedbackMessageMapper feedbackMessageMapper;
    private final UserMapper userMapper;

    public FeedbackMessageServiceImpl(FeedbackMessageMapper feedbackMessageMapper, UserMapper userMapper) {
        this.feedbackMessageMapper = feedbackMessageMapper;
        this.userMapper = userMapper;
    }

    @Override
    @CacheEvict(cacheNames = CacheNames.ADMIN_FEEDBACK_STATS, allEntries = true)
    public void createFeedback(String username, FeedbackCreateDTO dto) {
        User user = requireNonAdminUser(username, "管理员账号无需反馈");

        FeedbackMessage feedback = new FeedbackMessage();
        feedback.setSenderUserId(user.getId());
        feedback.setSenderUsername(user.getUsername());
        feedback.setSenderRoleType(user.getRoleType());
        feedback.setContent(dto.getContent().trim());
        feedback.setStatus(0);
        feedbackMessageMapper.insert(feedback);
    }

    @Override
    public List<FeedbackMessage> listMine(String username) {
        User user = requireNonAdminUser(username, "管理员账号无需查看该通道");

        return feedbackMessageMapper.selectList(new LambdaQueryWrapper<FeedbackMessage>()
                .eq(FeedbackMessage::getSenderUserId, user.getId())
                .orderByDesc(FeedbackMessage::getCreateTime));
    }

    @Override
    public Map<String, Object> listMinePaged(String username, LocalDateTime startTime, LocalDateTime endTime, int pageNo, int pageSize) {
        User user = requireNonAdminUser(username, "管理员账号无需查看该通道");

        int safePageNo = Math.min(Math.max(pageNo, 1), 1000);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePageNo - 1) * safePageSize;

        LambdaQueryWrapper<FeedbackMessage> countWrapper = new LambdaQueryWrapper<FeedbackMessage>()
                .eq(FeedbackMessage::getSenderUserId, user.getId());
        applyDateRange(countWrapper, startTime, endTime);
        long total = feedbackMessageMapper.selectCount(countWrapper);

        LambdaQueryWrapper<FeedbackMessage> pageWrapper = new LambdaQueryWrapper<FeedbackMessage>()
                .eq(FeedbackMessage::getSenderUserId, user.getId())
                .orderByDesc(FeedbackMessage::getCreateTime)
                .last("limit " + offset + "," + safePageSize);
        applyDateRange(pageWrapper, startTime, endTime);

        List<FeedbackMessage> records = feedbackMessageMapper.selectList(pageWrapper);
        return pagedResult(records, total, safePageNo, safePageSize);
    }

    @Override
    public List<FeedbackMessage> listAll(FeedbackQueryDTO query) {
        LambdaQueryWrapper<FeedbackMessage> wrapper = new LambdaQueryWrapper<FeedbackMessage>()
                .orderByDesc(FeedbackMessage::getCreateTime);
        applyAdminFilters(wrapper, query);
        return feedbackMessageMapper.selectList(wrapper);
    }

    @Override
    public Map<String, Object> listAllPaged(FeedbackQueryDTO query) {
        Integer pageNoObj = query == null ? null : query.getPageNo();
        Integer pageSizeObj = query == null ? null : query.getPageSize();
        int pageNo = pageNoObj == null ? 1 : pageNoObj;
        int pageSize = pageSizeObj == null ? 10 : pageSizeObj;
        int safePageNo = Math.min(Math.max(pageNo, 1), 1000);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        int offset = (safePageNo - 1) * safePageSize;

        LambdaQueryWrapper<FeedbackMessage> countWrapper = new LambdaQueryWrapper<>();
        applyAdminFilters(countWrapper, query);
        long total = feedbackMessageMapper.selectCount(countWrapper);

        LambdaQueryWrapper<FeedbackMessage> pageWrapper = new LambdaQueryWrapper<>();
        applyAdminFilters(pageWrapper, query);
        pageWrapper.orderByAsc(FeedbackMessage::getStatus)
                .orderByDesc(FeedbackMessage::getCreateTime)
                .last("limit " + offset + "," + safePageSize);

        List<FeedbackMessage> records = feedbackMessageMapper.selectList(pageWrapper);
        return pagedResult(records, total, safePageNo, safePageSize);
    }

    @Override
    public List<FeedbackMessage> listAllForExport(FeedbackQueryDTO query) {
        LambdaQueryWrapper<FeedbackMessage> wrapper = new LambdaQueryWrapper<>();
        applyAdminFilters(wrapper, query);
        wrapper.orderByAsc(FeedbackMessage::getStatus)
                .orderByDesc(FeedbackMessage::getCreateTime);
        return feedbackMessageMapper.selectList(wrapper);
    }

    @Override
    @Cacheable(cacheNames = CacheNames.ADMIN_FEEDBACK_STATS, key = "'dashboard'")
    public Map<String, Object> getAdminStats() {
        long totalCount = feedbackMessageMapper.selectCount(new LambdaQueryWrapper<>());
        long pendingCount = feedbackMessageMapper.selectCount(new LambdaQueryWrapper<FeedbackMessage>()
                .eq(FeedbackMessage::getStatus, 0));

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        long todayNewCount = feedbackMessageMapper.selectCount(new LambdaQueryWrapper<FeedbackMessage>()
                .ge(FeedbackMessage::getCreateTime, todayStart)
                .lt(FeedbackMessage::getCreateTime, tomorrowStart));

        long patientCount = feedbackMessageMapper.selectCount(new LambdaQueryWrapper<FeedbackMessage>()
                .eq(FeedbackMessage::getSenderRoleType, "PATIENT"));
        long doctorCount = feedbackMessageMapper.selectCount(new LambdaQueryWrapper<FeedbackMessage>()
                .eq(FeedbackMessage::getSenderRoleType, "DOCTOR"));

        Map<String, Object> roleDistribution = new HashMap<>();
        roleDistribution.put("PATIENT", patientCount);
        roleDistribution.put("DOCTOR", doctorCount);

        List<Map<String, Object>> recent7Days = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime dayStart = day.atStartOfDay();
            LocalDateTime nextDayStart = day.plusDays(1).atStartOfDay();
            long dayCount = feedbackMessageMapper.selectCount(new LambdaQueryWrapper<FeedbackMessage>()
                    .ge(FeedbackMessage::getCreateTime, dayStart)
                    .lt(FeedbackMessage::getCreateTime, nextDayStart));

            Map<String, Object> point = new HashMap<>();
            point.put("date", day.toString());
            point.put("count", dayCount);
            recent7Days.add(point);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", totalCount);
        result.put("pendingCount", pendingCount);
        result.put("todayNewCount", todayNewCount);
        result.put("roleDistribution", roleDistribution);
        result.put("recent7Days", recent7Days);
        return result;
    }

    @Override
    public long countPending() {
        return feedbackMessageMapper.selectCount(new LambdaQueryWrapper<FeedbackMessage>()
                .eq(FeedbackMessage::getStatus, 0));
    }

    @Override
    public long countUnreadReply(String username) {
        User user = requireUser(username);
        if ("ADMIN".equals(user.getRoleType())) {
            return 0L;
        }

        return feedbackMessageMapper.selectCount(new LambdaQueryWrapper<FeedbackMessage>()
                .eq(FeedbackMessage::getSenderUserId, user.getId())
                .eq(FeedbackMessage::getReplyRead, 0)
                .eq(FeedbackMessage::getStatus, 1)
                .isNotNull(FeedbackMessage::getReplyContent));
    }

    @Override
    public void markMineReplyRead(String username) {
        User user = requireUser(username);
        if ("ADMIN".equals(user.getRoleType())) {
            return;
        }

        List<FeedbackMessage> unreadList = feedbackMessageMapper.selectList(new LambdaQueryWrapper<FeedbackMessage>()
                .eq(FeedbackMessage::getSenderUserId, user.getId())
                .eq(FeedbackMessage::getReplyRead, 0)
                .eq(FeedbackMessage::getStatus, 1)
                .isNotNull(FeedbackMessage::getReplyContent));

        LocalDateTime now = LocalDateTime.now();
        for (FeedbackMessage item : unreadList) {
            item.setReplyRead(1);
            item.setReplyReadTime(now);
            feedbackMessageMapper.updateById(item);
        }
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ADMIN_FEEDBACK_STATS, allEntries = true)
        })
    public void updateStatus(Long id, Integer status) {
        FeedbackMessage feedback = feedbackMessageMapper.selectById(id);
        if (feedback == null) {
            throw BusinessException.notFound("反馈不存在");
        }
        validateStatus(status);
        feedback.setStatus(status);
        feedbackMessageMapper.updateById(feedback);
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ADMIN_FEEDBACK_STATS, allEntries = true)
        })
    public Map<String, Object> batchUpdateStatus(List<Long> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            throw BusinessException.badRequest("反馈ID列表不能为空");
        }
        validateStatus(status);

        int successCount = 0;
        int skippedCount = 0;
        List<Long> failedIds = new ArrayList<>();
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            FeedbackMessage feedback = feedbackMessageMapper.selectById(id);
            if (feedback == null) {
                failedIds.add(id);
                continue;
            }
            if (feedback.getStatus() != null && feedback.getStatus().equals(status)) {
                skippedCount++;
                continue;
            }
            if (updateFeedbackStatus(feedback, status)) {
                successCount++;
            } else {
                failedIds.add(id);
            }
        }

        return batchResult(ids.size(), successCount, skippedCount, failedIds);
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ADMIN_FEEDBACK_STATS, allEntries = true)
        })
    public Map<String, Object> batchUpdateStatusByFilter(FeedbackQueryDTO query, Integer targetStatus) {
        validateStatus(targetStatus);

        List<FeedbackMessage> records = listAllForExport(query);
        int requestedCount = records.size();
        int successCount = 0;
        int skippedCount = 0;
        List<Long> failedIds = new ArrayList<>();

        for (FeedbackMessage item : records) {
            if (item.getStatus() != null && item.getStatus().equals(targetStatus)) {
                skippedCount++;
                continue;
            }
            if (updateFeedbackStatus(item, targetStatus)) {
                successCount++;
            } else {
                failedIds.add(item.getId());
            }
        }

        return batchResult(requestedCount, successCount, skippedCount, failedIds);
    }

    @Override
        @Caching(evict = {
            @CacheEvict(cacheNames = CacheNames.ADMIN_FEEDBACK_STATS, allEntries = true)
        })
    public void replyFeedback(AdminFeedbackReplyDTO dto) {
        FeedbackMessage feedback = feedbackMessageMapper.selectById(dto.getId());
        if (feedback == null) {
            throw BusinessException.notFound("反馈不存在");
        }
        validateStatus(dto.getStatus());

        feedback.setReplyContent(dto.getReplyContent().trim());
        feedback.setRepliedTime(LocalDateTime.now());
        feedback.setStatus(dto.getStatus());
        if (dto.getStatus() == 1) {
            feedback.setReplyRead(0);
            feedback.setReplyReadTime(null);
        }
        feedbackMessageMapper.updateById(feedback);
    }

    private void applyAdminFilters(LambdaQueryWrapper<FeedbackMessage> wrapper, FeedbackQueryDTO query) {
        String keyword = query == null ? null : query.getKeyword();
        String roleType = query == null ? null : query.getRoleType();
        Integer status = query == null ? null : query.getStatus();
        Integer replyStatus = query == null ? null : query.getReplyStatus();
        LocalDateTime startTime = query == null ? null : query.getStartTime();
        LocalDateTime endTime = query == null ? null : query.getEndTime();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(FeedbackMessage::getSenderUsername, keyword)
                    .or().like(FeedbackMessage::getContent, keyword));
        }
        if (StringUtils.hasText(roleType)) {
            wrapper.eq(FeedbackMessage::getSenderRoleType, roleType);
        }
        if (status != null) {
            wrapper.eq(FeedbackMessage::getStatus, status);
        }
        if (replyStatus != null) {
            if (replyStatus == 1) {
                wrapper.isNotNull(FeedbackMessage::getReplyContent);
            } else if (replyStatus == 0) {
                wrapper.isNull(FeedbackMessage::getReplyContent);
            }
        }
        applyDateRange(wrapper, startTime, endTime);
    }

    private void applyDateRange(LambdaQueryWrapper<FeedbackMessage> wrapper,
                                LocalDateTime startTime,
                                LocalDateTime endTime) {
        if (startTime != null) {
            wrapper.ge(FeedbackMessage::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(FeedbackMessage::getCreateTime, endTime);
        }
    }

    private Map<String, Object> pagedResult(List<FeedbackMessage> records, long total, int pageNo, int pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("records", records);
        result.put("total", total);
        result.put("pageNo", pageNo);
        result.put("pageSize", pageSize);
        return result;
    }

    private Map<String, Object> batchResult(int requestedCount,
                                            int successCount,
                                            int skippedCount,
                                            List<Long> failedIds) {
        Map<String, Object> result = new HashMap<>();
        result.put("requestedCount", requestedCount);
        result.put("successCount", successCount);
        result.put("skippedCount", skippedCount);
        result.put("failedIds", failedIds);
        return result;
    }

    private boolean updateFeedbackStatus(FeedbackMessage feedback, int targetStatus) {
        feedback.setStatus(targetStatus);
        return feedbackMessageMapper.updateById(feedback) > 0;
    }

    private User requireUser(String username) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    private User requireNonAdminUser(String username, String adminErrorMessage) {
        User user = requireUser(username);
        if ("ADMIN".equals(user.getRoleType())) {
            throw BusinessException.forbidden(adminErrorMessage);
        }
        return user;
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw BusinessException.badRequest("状态非法");
        }
    }
}
