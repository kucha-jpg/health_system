package com.health.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.health.system.dto.SystemNoticeDTO;
import com.health.system.entity.SystemNotice;
import com.health.system.mapper.SystemNoticeMapper;
import com.health.system.service.SystemNoticeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SystemNoticeServiceImpl implements SystemNoticeService {

    private final SystemNoticeMapper systemNoticeMapper;

    public SystemNoticeServiceImpl(SystemNoticeMapper systemNoticeMapper) {
        this.systemNoticeMapper = systemNoticeMapper;
    }

    @Override
    public List<SystemNotice> listNotices(boolean includeOffline, String keyword, Integer status) {
        LambdaQueryWrapper<SystemNotice> wrapper = new LambdaQueryWrapper<SystemNotice>()
                .orderByDesc(SystemNotice::getCreateTime);

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(SystemNotice::getTitle, keyword)
                    .or().like(SystemNotice::getContent, keyword));
        }
        if (status != null) {
            wrapper.eq(SystemNotice::getStatus, status);
        }
        if (!includeOffline) {
            wrapper.eq(SystemNotice::getStatus, 1);
        }
        return systemNoticeMapper.selectList(wrapper);
    }

    @Override
    public void createNotice(SystemNoticeDTO dto) {
        SystemNotice notice = new SystemNotice();
        notice.setTitle(dto.getTitle());
        notice.setContent(dto.getContent());
        notice.setStatus(dto.getStatus());
        systemNoticeMapper.insert(notice);
    }

    @Override
    public void updateNotice(SystemNoticeDTO dto) {
        SystemNotice exists = systemNoticeMapper.selectById(dto.getId());
        if (exists == null) {
            throw new RuntimeException("公告不存在");
        }
        exists.setTitle(dto.getTitle());
        exists.setContent(dto.getContent());
        exists.setStatus(dto.getStatus());
        systemNoticeMapper.updateById(exists);
    }

    @Override
    public void deleteNotice(Long id) {
        systemNoticeMapper.deleteById(id);
    }
}
