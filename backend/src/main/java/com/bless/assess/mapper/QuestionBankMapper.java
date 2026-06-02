package com.bless.assess.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bless.assess.entity.QuestionBank;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Set;

@Mapper
public interface QuestionBankMapper extends BaseMapper<QuestionBank> {
    
    /**
     * 按题型随机抽取指定数量的题目（排除黑名单）
     */
    @Select("SELECT * FROM question_bank WHERE question_type = #{questionType} AND enabled = 1 AND deleted = 0 " +
            "AND id NOT IN (SELECT question_id FROM question_blacklist_conf) " +
            "ORDER BY RAND() LIMIT #{limit}")
    List<QuestionBank> randomSelectByType(@Param("questionType") String questionType, 
                                          @Param("limit") int limit);
    
    /**
     * 按题型和难度随机抽取题目（排除黑名单）
     */
    @Select("SELECT * FROM question_bank WHERE question_type = #{questionType} AND difficulty = #{difficulty} " +
            "AND enabled = 1 AND deleted = 0 AND id NOT IN (SELECT question_id FROM question_blacklist_conf) " +
            "ORDER BY RAND() LIMIT #{limit}")
    List<QuestionBank> randomSelectByTypeAndDifficulty(@Param("questionType") String questionType,
                                                        @Param("difficulty") int difficulty,
                                                        @Param("limit") int limit);
    
    /**
     * 批量查询题目ID列表
     */
    List<QuestionBank> selectByIds(@Param("ids") Set<Long> ids);
}
