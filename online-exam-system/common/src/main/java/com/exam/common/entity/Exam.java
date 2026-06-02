package com.exam.common.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
@TableName("t_exam")
public class Exam implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private Date startTime;
    private Date endTime;
    private String questionIds; // 题目ID，逗号拼接：1,2,3...
    private Integer totalScore;
    private Long creatorId;
    private String status; // UPCOMING, RUNNING, FINISHED
}
