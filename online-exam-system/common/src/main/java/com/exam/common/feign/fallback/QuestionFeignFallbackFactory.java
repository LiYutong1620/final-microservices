package com.exam.common.feign.fallback;

import com.exam.common.entity.Question;
import com.exam.common.feign.QuestionFeignClient;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
public class QuestionFeignFallbackFactory implements FallbackFactory<QuestionFeignClient> {
    @Override
    public QuestionFeignClient create(Throwable cause) {
        return new QuestionFeignClient() {
            @Override
            public List<Question> getQuestionsByIds(List<Long> ids) {
                // Sentinel 熔断降级逻辑
                System.out.println("熔断降级触发! 错误原因: " + cause.getMessage());
                // 返回空容器或带错误标识的退路答案，上层Score-service据此抛出 Sentinel 降级异常或默认处理
                throw new RuntimeException("阅卷服务繁忙，题库信息拉取失败，请稍后重试");
            }
        };
    }
}
