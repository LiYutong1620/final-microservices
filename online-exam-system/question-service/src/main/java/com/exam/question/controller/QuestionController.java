package com.exam.question.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.entity.Question;
import com.exam.question.mapper.QuestionMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "题库管理服务")
@RestController
@RequestMapping("/api/question")
public class QuestionController {

    @Autowired
    private QuestionMapper questionMapper;

    @ApiOperation("添加新试题")
    @PostMapping("/add")
    public Map<String, Object> addQuestion(@RequestBody Question question) {
        Map<String, Object> result = new HashMap<>();
        questionMapper.insert(question);
        result.put("code", 200);
        result.put("message", "题目入库成功！ID: " + question.getId());
        return result;
    }

    @ApiOperation("查询整套题库题单（可提供按分类筛选）")
    @GetMapping("/list")
    public Map<String, Object> getList(@RequestParam(value = "category", required = false) String category) {
        Map<String, Object> result = new HashMap<>();
        LambdaQueryWrapper<Question> query = new LambdaQueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            query.eq(Question::getCategory, category);
        }
        List<Question> list = questionMapper.selectList(query);
        result.put("code", 200);
        result.put("questions", list);
        return result;
    }

    @ApiOperation("Feign远程客户端：依据Question IDs批量请求题目及答案详情")
    @GetMapping("/batch")
    public List<Question> getQuestionsByIds(@RequestParam("ids") List<Long> ids) {
        // 模拟 question-service 超时 (1.5s以上，配合熔断降级演示可取消/减弱其睡眠，也可以加5秒演示Sentinel超限)
        // try { Thread.sleep(200); } catch(Exception e) {}
        return questionMapper.selectBatchIds(ids);
    }
}
