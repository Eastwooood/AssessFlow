package com.bless.assess.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bless.assess.entity.SessionStep;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface SessionStepMapper extends BaseMapper<SessionStep> {
    
    /**
     * 查询会话的所有步骤（按索引排序）
     */
    List<SessionStep> selectBySessionIdOrderByIndex(Long sessionId);
}
