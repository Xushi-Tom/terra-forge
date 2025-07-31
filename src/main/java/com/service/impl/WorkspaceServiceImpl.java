package com.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.config.LocalCacheService;
import com.mapper.WorkspaceMapper;
import com.po.WorkspacePo;
import com.service.IWorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class WorkspaceServiceImpl extends ServiceImpl<WorkspaceMapper, WorkspacePo> implements IWorkspaceService {

    @Autowired
    private LocalCacheService localCacheService;

    @Override
    public String updateStatusWorkspace(String workspaceGroup, String workspace, String status) {
        // 存入redis
        // 存入本地缓存（缓存30分钟）
        String cacheKey = workspaceGroup + "_" + workspace;
        localCacheService.setValue(cacheKey, status, 1800);
        return "修改成功";
    }
}
