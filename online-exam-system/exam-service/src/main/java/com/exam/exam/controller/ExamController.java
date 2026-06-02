package com.exam.exam.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.entity.Exam;
import com.exam.common.dto.ExamDTO;
import com.exam.exam.mapper.ExamMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Api(tags = "考试管理模块")
@RefreshScope
@RestController
@RequestMapping("/api/exam")
public class ExamController {

    @Autowired
    private ExamMapper examMapper;

    // 从 Nacos 配置中心中动态读取最大参加限制人数
    @Value("${exam.max.students:50}")
    private int maxStudents;

    @ApiOperation("动态读取Nacos单场重分配最大考生配置")
    @GetMapping("/config/max-students")
    public Map<String, Object> getMaxStudentsConfig() {
        Map<String, Object> result = new HashMap<>();
        result.put("maxStudents", maxStudents);
        return result;
    }

    @ApiOperation("教师新建考试发布")
    @PostMapping("/create")
    public Map<String, Object> createExam(@RequestBody ExamDTO dto, @RequestHeader("X-User-Id") Long teacherId) {
        Map<String, Object> result = new HashMap<>();
        Exam exam = new Exam();
        exam.setTitle(dto.getTitle());
        exam.setStartTime(dto.getStartTime());
        exam.setEndTime(dto.getEndTime());
        exam.setTotalScore(dto.getTotalScore());
        exam.setCreatorId(teacherId);
        exam.setStatus("UPCOMING");
        
        // 拼接题目 ID 存入数据库
        String idsStr = dto.getQuestionIds().stream().map(String::valueOf).collect(Collectors.joining(","));
        exam.setQuestionIds(idsStr);

        examMapper.insert(exam);
        result.put("code", 200);
        result.put("message", "考试成功发布！限制同一时段参加的最大学生上限: " + maxStudents);
        return result;
    }

    @ApiOperation("获取公开考核状态考试列表")
    @GetMapping("/list")
    public Map<String, Object> getExamList() {
        Map<String, Object> result = new HashMap<>();
        List<Exam> list = examMapper.selectList(new LambdaQueryWrapper<Exam>().orderByDesc(Exam::getStartTime));
        
        // 动态计算更新考试状态
        long now = System.currentTimeMillis();
        for (Exam e : list) {
            if (e.getStartTime().getTime() > now) {
                e.setStatus("UPCOMING");
            } else if (e.getEndTime().getTime() < now) {
                e.setStatus("FINISHED");
            } else {
                e.setStatus("RUNNING");
            }
            examMapper.updateById(e);
        }

        result.put("code", 200);
        result.put("exams", list);
        return result;
    }

    @ApiOperation("获取具体考核基本项及包含主键")
    @GetMapping("/{id}")
    public Map<String, Object> getExamDetail(@PathVariable("id") Long id) {
        Map<String, Object> result = new HashMap<>();
        Exam exam = examMapper.selectById(id);
        if (exam == null) {
            result.put("code", 404);
            result.put("message", "考试不存在！");
        } else {
            result.put("code", 200);
            result.put("exam", exam);
        }
        return result;
    }
}
