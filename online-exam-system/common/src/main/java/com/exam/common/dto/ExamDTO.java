package com.exam.common.dto;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class ExamDTO {
    private String title;
    private Date startTime;
    private Date endTime;
    private List<Long> questionIds;
    private Integer totalScore;
}
