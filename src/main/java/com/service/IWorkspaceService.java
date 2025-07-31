package com.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.po.WorkspacePo;

public interface IWorkspaceService extends IService<WorkspacePo> {

    String updateStatusWorkspace(String workspaceGroup, String workspace, String status);

}
