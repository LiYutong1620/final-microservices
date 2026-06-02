package com.exam.common.feign;

import com.exam.common.entity.Question;
import com.exam.common.feign.fallback.QuestionFeignFallbackFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@FeignClient(name = "question-service", fallbackFactory = QuestionFeignFallbackFactory.class)
public interface QuestionFeignClient {
    
    @GetMapping("/api/question/batch")
    List<Question> getQuestionsByIds(@RequestParam("ids") List<Long> ids);
}
