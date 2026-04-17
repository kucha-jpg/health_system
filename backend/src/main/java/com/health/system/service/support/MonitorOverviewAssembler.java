package com.health.system.service.support;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.health.system.entity.DoctorGroup;
import com.health.system.entity.DoctorGroupMember;
import com.health.system.entity.HealthData;
import com.health.system.entity.User;
import com.health.system.mapper.UserMapper;

@Component
public class MonitorOverviewAssembler {

    private final UserMapper userMapper;

    public MonitorOverviewAssembler(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public Map<String, Object> assemble(long totalUsers,
                                        long totalHealthData,
                                        long openAlerts,
                                        List<HealthData> latestHealthData,
                                        List<HealthData> recentMonthData,
                                        Set<String> enabledRuleIndicators,
                                        List<DoctorGroup> groups,
                                        List<DoctorGroupMember> groupMembers) {
        Map<String, Object> result = new HashMap<>();
        result.put("totalUsers", totalUsers);
        result.put("totalHealthData", totalHealthData);
        result.put("openAlerts", openAlerts);
        result.put("latestHealthData", latestHealthData);
        result.put("indicatorDistribution", buildIndicatorDistribution(recentMonthData, enabledRuleIndicators));
        result.put("dailyReportTrend", buildDailyReportTrend(recentMonthData, 14));
        result.put("activeUserStats", buildActiveUserStats(recentMonthData));
        result.put("groupStats", buildGroupStats(groups, groupMembers));
        return result;
    }

    private List<Map<String, Object>> buildIndicatorDistribution(List<HealthData> dataList,
                                                                 Set<String> enabledRuleIndicators) {
        Set<String> allowedIndicators = enabledRuleIndicators == null ? Set.of() : new HashSet<>(enabledRuleIndicators);
        Map<String, Integer> counts = new HashMap<>();
        for (String indicator : allowedIndicators) {
            if (indicator != null && !indicator.isBlank()) {
                counts.put(indicator, 0);
            }
        }

        for (HealthData item : dataList) {
            String key = item.getIndicatorType() == null ? "未知" : item.getIndicatorType();
            if (!allowedIndicators.isEmpty() && !allowedIndicators.contains(key)) {
                continue;
            }
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(entry -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("indicatorType", entry.getKey());
                    row.put("count", entry.getValue());
                    return row;
                }).toList();
    }

    private List<Map<String, Object>> buildDailyReportTrend(List<HealthData> dataList, int days) {
        int safeDays = Math.max(days, 1);
        Map<LocalDate, Integer> bucket = new HashMap<>();
        for (HealthData item : dataList) {
            if (item.getReportTime() == null) {
                continue;
            }
            LocalDate day = item.getReportTime().toLocalDate();
            bucket.put(day, bucket.getOrDefault(day, 0) + 1);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = safeDays - 1; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            Map<String, Object> row = new HashMap<>();
            row.put("date", day.toString());
            row.put("count", bucket.getOrDefault(day, 0));
            result.add(row);
        }
        return result;
    }

    private List<Map<String, Object>> buildActiveUserStats(List<HealthData> dataList) {
        Map<Long, Integer> counts = new HashMap<>();
        for (HealthData item : dataList) {
            if (item.getUserId() == null) {
                continue;
            }
            counts.put(item.getUserId(), counts.getOrDefault(item.getUserId(), 0) + 1);
        }
        if (counts.isEmpty()) {
            return List.of();
        }

        List<Long> topIds = counts.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .limit(8)
                .map(Map.Entry::getKey)
                .toList();

        Map<Long, User> userMap = new HashMap<>();
        for (User user : userMapper.selectBatchIds(topIds)) {
            userMap.put(user.getId(), user);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Long userId : topIds) {
            User user = userMap.get(userId);
            Map<String, Object> row = new HashMap<>();
            row.put("userId", userId);
            row.put("username", user == null ? String.valueOf(userId) : user.getUsername());
            row.put("name", user == null ? "-" : user.getName());
            row.put("count", counts.getOrDefault(userId, 0));
            result.add(row);
        }
        result.sort(Comparator.comparing((Map<String, Object> m) -> (Integer) m.get("count")).reversed());
        return result;
    }

    private List<Map<String, Object>> buildGroupStats(List<DoctorGroup> groups, List<DoctorGroupMember> members) {
        if (groups.isEmpty()) {
            return List.of();
        }
        Map<Long, Integer> countByGroup = new HashMap<>();
        for (DoctorGroupMember item : members) {
            countByGroup.put(item.getGroupId(), countByGroup.getOrDefault(item.getGroupId(), 0) + 1);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (DoctorGroup group : groups) {
            Map<String, Object> row = new HashMap<>();
            row.put("groupId", group.getId());
            row.put("groupName", group.getGroupName());
            row.put("patientCount", countByGroup.getOrDefault(group.getId(), 0));
            result.add(row);
        }
        result.sort(Comparator.comparing((Map<String, Object> m) -> (Integer) m.get("patientCount")).reversed());
        return result.stream().limit(10).toList();
    }
}
