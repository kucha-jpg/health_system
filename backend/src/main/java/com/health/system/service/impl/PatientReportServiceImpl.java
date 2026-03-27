package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.common.BusinessException;
import com.health.system.common.CacheNames;
import com.health.system.entity.HealthAlert;
import com.health.system.entity.HealthData;
import com.health.system.entity.User;
import com.health.system.mapper.HealthAlertMapper;
import com.health.system.mapper.HealthDataMapper;
import com.health.system.mapper.UserMapper;
import com.health.system.service.PatientReportService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

@Service
public class PatientReportServiceImpl implements PatientReportService {

    private final UserMapper userMapper;
    private final HealthDataMapper healthDataMapper;
    private final HealthAlertMapper healthAlertMapper;
    private final Executor reportSummaryExecutor;

    private final ConcurrentHashMap<String, SummaryTask> tasks = new ConcurrentHashMap<>();

    public PatientReportServiceImpl(UserMapper userMapper,
                                    HealthDataMapper healthDataMapper,
                                    HealthAlertMapper healthAlertMapper,
                                    @Qualifier("reportSummaryExecutor") Executor reportSummaryExecutor) {
        this.userMapper = userMapper;
        this.healthDataMapper = healthDataMapper;
        this.healthAlertMapper = healthAlertMapper;
        this.reportSummaryExecutor = reportSummaryExecutor;
    }

    @Override
    @Cacheable(cacheNames = CacheNames.PATIENT_REPORT_SUMMARY, key = "#username + '::' + (#range == null ? 'week' : #range.toLowerCase())")
    public Map<String, Object> summary(String username, String range) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username));
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        LocalDateTime start = "month".equalsIgnoreCase(range) ? LocalDateTime.now().minusMonths(1) : LocalDateTime.now().minusWeeks(1);

        List<HealthData> dataList = healthDataMapper.selectList(new LambdaQueryWrapper<HealthData>()
                .eq(HealthData::getUserId, user.getId())
                .ge(HealthData::getReportTime, start)
                .orderByDesc(HealthData::getReportTime));

        long alertCount = healthAlertMapper.selectCount(new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getUserId, user.getId())
                .ge(HealthAlert::getCreateTime, start));

        List<HealthAlert> alerts = healthAlertMapper.selectList(new LambdaQueryWrapper<HealthAlert>()
                .eq(HealthAlert::getUserId, user.getId())
                .ge(HealthAlert::getCreateTime, start)
                .orderByAsc(HealthAlert::getCreateTime));

        Map<String, Integer> byType = new HashMap<>();
        for (HealthData data : dataList) {
            byType.put(data.getIndicatorType(), byType.getOrDefault(data.getIndicatorType(), 0) + 1);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("range", "month".equalsIgnoreCase(range) ? "month" : "week");
        result.put("reportCount", dataList.size());
        result.put("alertCount", alertCount);
        result.put("byType", byType);
        result.put("latestData", dataList.stream().limit(10).toList());
        result.put("riskTrend", buildRiskTrend(alerts));
        return result;
    }

    @Override
    public Map<String, Object> submitSummaryTask(String username, String range) {
        String normalizedRange = "month".equalsIgnoreCase(range) ? "month" : "week";
        String taskId = UUID.randomUUID().toString().replace("-", "");
        SummaryTask task = SummaryTask.running(taskId, username, normalizedRange);
        tasks.put(taskId, task);

        CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> report = summary(username, normalizedRange);
                task.setSuccess(report);
            } catch (Exception ex) {
                task.setFailed(ex.getMessage() == null ? "报告生成失败" : ex.getMessage());
            }
        }, reportSummaryExecutor);

        Map<String, Object> result = new HashMap<>();
        result.put("taskId", taskId);
        result.put("status", task.getStatus());
        result.put("range", normalizedRange);
        return result;
    }

    @Override
    public Map<String, Object> summaryTaskResult(String username, String taskId) {
        SummaryTask task = tasks.get(taskId);
        if (task == null || !username.equals(task.getUsername())) {
            throw BusinessException.notFound("任务不存在");
        }
        Map<String, Object> result = new HashMap<>();
        result.put("taskId", task.getTaskId());
        result.put("status", task.getStatus());
        result.put("range", task.getRange());
        result.put("createdAt", task.getCreatedAt());
        if (task.getResult() != null) {
            result.put("result", task.getResult());
        }
        if (task.getErrorMessage() != null) {
            result.put("error", task.getErrorMessage());
        }
        return result;
    }

    private List<Map<String, Object>> buildRiskTrend(List<HealthAlert> alerts) {
        Map<String, int[]> agg = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        for (HealthAlert alert : alerts) {
            if (alert.getCreateTime() == null) {
                continue;
            }
            Integer scoreObj = alert.getRiskScore();
            int score = scoreObj == null ? 60 : scoreObj;
            String day = alert.getCreateTime().format(fmt);
            int[] stat = agg.computeIfAbsent(day, k -> new int[]{0, 0});
            stat[0] += score;
            stat[1] += 1;
        }

        return agg.entrySet().stream().map(entry -> {
            int total = entry.getValue()[0];
            int count = entry.getValue()[1];
            int avg = count == 0 ? 0 : (int) Math.round((double) total / count);
            Map<String, Object> row = new HashMap<>();
            row.put("date", entry.getKey());
            row.put("avgRiskScore", avg);
            row.put("alertCount", count);
            return row;
        }).toList();
    }

    private static class SummaryTask {
        private final String taskId;
        private final String username;
        private final String range;
        private final LocalDateTime createdAt;
        private volatile String status;
        private volatile Map<String, Object> result;
        private volatile String errorMessage;

        private SummaryTask(String taskId, String username, String range, String status) {
            this.taskId = taskId;
            this.username = username;
            this.range = range;
            this.status = status;
            this.createdAt = LocalDateTime.now();
        }

        static SummaryTask running(String taskId, String username, String range) {
            return new SummaryTask(taskId, username, range, "RUNNING");
        }

        void setSuccess(Map<String, Object> result) {
            this.result = result;
            this.status = "SUCCESS";
            this.errorMessage = null;
        }

        void setFailed(String message) {
            this.result = null;
            this.status = "FAILED";
            this.errorMessage = message;
        }

        public String getTaskId() {
            return taskId;
        }

        public String getUsername() {
            return username;
        }

        public String getRange() {
            return range;
        }

        public LocalDateTime getCreatedAt() {
            return createdAt;
        }

        public String getStatus() {
            return status;
        }

        public Map<String, Object> getResult() {
            return result;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
