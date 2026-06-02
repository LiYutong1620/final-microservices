package com.exam.score.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.entity.Question;
import com.exam.common.entity.Score;
import com.exam.common.feign.QuestionFeignClient;
import com.exam.score.mapper.ScoreMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@Api(tags = "阅卷及成绩信息管理服务")
@RestController
@RequestMapping("/api/score")
public class ScoreController {

    @Autowired
    private ScoreMapper scoreMapper;

    @Autowired
    private QuestionFeignClient questionFeignClient;

    @ApiOperation("学生提交答卷（核心业务，含Feign调用+Sentinel限流保护）")
    @PostMapping("/submit")
    @SentinelResource(value = "submitScore", blockHandler = "submitBlockHandler")
    public Map<String, Object> submitExamAnswers(
            @RequestBody Map<String, Object> payload,
            @RequestHeader("X-User-Id") Long studentId,
            @RequestHeader("X-User-Name") String studentName) {

        Map<String, Object> result = new HashMap<>();
        Long examId = Long.valueOf(payload.get("examId").toString());
        String examTitle = payload.get("examTitle").toString();
        
        @SuppressWarnings("unchecked")
        Map<String, String> studentAnswers = (Map<String, String>) payload.get("answers");

        // 1. 获取题目ID列表以拉取正确解析
        List<Long> questionIds = new ArrayList<>();
        for (String qId : studentAnswers.keySet()) {
            questionIds.add(Long.valueOf(qId));
        }

        // 2. 核心远程调用：OpenFeign 提取题库标准参考答案 (若 question-service 挂掉，会触发 FallbackFactory 报错)
        List<Question> correctQuestions = questionFeignClient.getQuestionsByIds(questionIds);

        // 3. 校验并算分 (自动判选择题)
        int scoreValue = 0;
        for (Question q : correctQuestions) {
            String studentAns = studentAnswers.get(q.getId().toString());
            if (studentAns != null && studentAns.trim().equalsIgnoreCase(q.getAnswer().trim())) {
                scoreValue += q.getScore();
            }
        }

        // 4. 新生成学生成绩记录
        Score score = new Score();
        score.setExamId(examId);
        score.setExamTitle(examTitle);
        score.setStudentId(studentId);
        score.setStudentName(studentName);
        score.setFinalScore(scoreValue);
        
        // 序列化学生答卷
        score.setMyAnswers(studentAnswers.toString());
        score.setSubmitTime(new Date());

        scoreMapper.insert(score);

        result.put("code", 200);
        result.put("message", "试卷阅毕！答卷自动计算阅卷已归档。");
        result.put("score", score);
        return result;
    }

    // 限流熔断兜底方法
    public Map<String, Object> submitBlockHandler(
            Map<String, Object> payload, 
            Long studentId, 
            String studentName, 
            BlockException e) {
        Map<String, Object> result = new HashMap<>();
        result.put("code", 429);
        result.put("message", "交卷系统高炮超载(Sentinel限流)：Blocked by Sentinel - 阅卷服务暂时繁忙，请重新提交！");
        return result;
    }

    @ApiOperation("查询某位学生的考试记录")
    @GetMapping("/history")
    public Map<String, Object> getStudentHistory(@RequestHeader("X-User-Id") Long studentId) {
        Map<String, Object> result = new HashMap<>();
        LambdaQueryWrapper<Score> query = new LambdaQueryWrapper<>();
        query.eq(Score::getStudentId, studentId).orderByDesc(Score::getSubmitTime);
        result.put("code", 200);
        result.put("history", scoreMapper.selectList(query));
        return result;
    }

    @ApiOperation("教师获取某场考试的全部统成绩")
    @GetMapping("/exam/{examId}")
    public Map<String, Object> getScoresByExam(@PathVariable("examId") Long examId) {
        Map<String, Object> result = new HashMap<>();
        LambdaQueryWrapper<Score> query = new LambdaQueryWrapper<>();
        query.eq(Score::getExamId, examId).orderByDesc(Score::getFinalScore);
        result.put("code", 200);
        result.put("scores", scoreMapper.selectList(query));
        return result;
    }
}
