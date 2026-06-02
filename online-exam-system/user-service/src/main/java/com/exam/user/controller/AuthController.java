package com.exam.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.exam.common.entity.User;
import com.exam.common.utils.JwtUtil;
import com.exam.user.mapper.UserMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Api(tags = "用户认证管理模块")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @ApiOperation("注册接口")
    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody User user) {
        Map<String, Object> result = new HashMap<>();
        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, user.getUsername());
        if (userMapper.selectCount(query) > 0) {
            result.put("code", 400);
            result.put("message", "用户名已存在！");
            return result;
        }
        
        userMapper.insert(user);
        result.put("code", 200);
        result.put("message", "注册成功！");
        return result;
    }

    @ApiOperation("登录接口")
    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> loginParam) {
        Map<String, Object> result = new HashMap<>();
        String username = loginParam.get("username");
        String password = loginParam.get("password");

        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<>();
        query.eq(User::getUsername, username).eq(User::getPassword, password);
        User user = userMapper.selectOne(query);

        if (user == null) {
            result.put("code", 401);
            result.put("message", "用户名或密码错误！");
            return result;
        }

        // 生成 Token
        String token = JwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 使用 Redis 记录已登录状态
        redisTemplate.opsForValue().set("login_token:" + user.getId(), token, 1, TimeUnit.DAYS);

        result.put("code", 200);
        result.put("message", "登录成功！");
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    @ApiOperation("获取当前登录用户信息")
    @GetMapping("/me")
    public Map<String, Object> getMe(@RequestHeader("X-User-Id") Long userId) {
        Map<String, Object> result = new HashMap<>();
        User user = userMapper.selectById(userId);
        if (user == null) {
            result.put("code", 404);
            result.put("message", "用户不存在！");
        } else {
            result.put("code", 200);
            result.put("user", user);
        }
        return result;
    }
}
