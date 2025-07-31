package com.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mapper.MapRecordMapper;
import com.po.MapRecordPo;
import com.service.IMapRecordService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class MapRecordServiceImpl extends ServiceImpl<MapRecordMapper, MapRecordPo> implements IMapRecordService {

}
