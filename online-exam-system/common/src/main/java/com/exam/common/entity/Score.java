package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_score")
public class Score implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long examId;
    private String examTitle;
    private Long studentId;
    private String studentName;
    private Integer finalScore;
    private String myAnswers; // 学生提交答案，JSON字符串: {"1":"A","2":"C"}
    private Date submitTime;
}
