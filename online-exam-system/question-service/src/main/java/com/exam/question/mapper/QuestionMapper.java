package com.exam.question.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.exam.common.entity.Question;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QuestionMapper extends BaseMapper<Question> {
}
