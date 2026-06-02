package com.bless.assess.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bless.assess.entity.QuestionBlacklistConf;
import org.apache.ibatis.annotations.Mapper;

import java.util.Set;

@Mapper
public interface QuestionBlacklistConfMapper extends BaseMapper<QuestionBlacklistConf> {
    
    /**
     * 获取所有黑名单题目ID集合
     */
    Set<Long> selectAllBlacklistedIds();
}
