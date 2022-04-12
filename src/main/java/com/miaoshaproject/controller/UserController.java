package com.miaoshaproject.controller;

import com.miaoshaproject.controller.viewobject.UserVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import com.miaoshaproject.utils.ObjectConvertUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.security.MD5Encoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class UserController extends BaseController {

    @Autowired
    private UserService userService;
    @Autowired
    private ObjectConvertUtil objectConvertUtil;
    @Autowired(required = false)
    private HttpServletRequest httpServletRequest;
    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据用户id 获取用户对象
     * @param id
     * @return
     * @throws BusinessException
     */
    @GetMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name = "id") Integer id) throws BusinessException {
        // 调用service服务获取对应的用户对象并返回给前端
        UserModel userModel = userService.getUserById(id);

        // 若获取的对应用户信息不存在
        if (ObjectUtils.isEmpty(userModel)) {
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }

        // 将核心领域模型用户对象转换为可供UI使用的viewobject
        UserVO userVO = objectConvertUtil.convertFromModel(userModel);

        return CommonReturnType.create(userVO);
    }

    /**
     * 用户获取otp短信接口
     * @param telphone
     * @return
     */
    @PostMapping(value = "/getotp", consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name = "telphone") String telphone) {
        // 按照一定规则生成OTP验证码
        Random random = new Random();
        int randomInt = random.nextInt(99999);
        randomInt += 10000;
        String otpCode = String.valueOf(randomInt);

        // 将OTP验证码与用户的telphone关联, 使用httpsession的方式绑定他的手机号与optCode
        httpServletRequest.getSession().setAttribute(telphone, otpCode);

        // 将OTP验证码通过短信通道发送给用户,这里暂时省略
        System.out.println("telphone=" + telphone + " & otpCode=" + otpCode);

        return CommonReturnType.create(null);
    }


    /**
     * 用户注册接口
     * @param telphone
     * @param otpCode
     * @param name
     * @param gender
     * @param age
     * @param password
     * @return
     * @throws BusinessException
     * @throws UnsupportedEncodingException
     * @throws NoSuchAlgorithmException
     */
    @PostMapping(value = "/register", consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(@RequestParam(name = "telphone") String telphone,
                                     @RequestParam(name = "otpCode") String otpCode,
                                     @RequestParam(name = "name") String name,
                                     @RequestParam(name = "gender") Integer gender,
                                     @RequestParam(name = "age") Integer age,
                                     @RequestParam(name = "password") String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {

        // 验证手机号与 对应的 otpCode相符合
        String inSessionOtpCode = (String) this.httpServletRequest.getSession().getAttribute(telphone);
        if (!otpCode.equals(inSessionOtpCode)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "短信验证码不正确");
        }

        // 用户注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(gender.byteValue());
        userModel.setAge(age);
        userModel.setTelphone(telphone);
        userModel.setRegisterModel("byphone");
        userModel.setEncrptPassword(objectConvertUtil.EncodeByMd5(password));

        userService.register(userModel);
        return CommonReturnType.create(null);

    }

    @RequestMapping(value = "/login", method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(@RequestParam(name = "telphone") String telphone,
                                  @RequestParam(name = "password") String password) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {

        // 入参校验
        if (StringUtils.isEmpty(telphone) || StringUtils.isEmpty(password)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        // 用户登录服务,校验用户登录是否合法
        UserModel userModel = userService.login(telphone, objectConvertUtil.EncodeByMd5(password));

        // 用户登录成功后，将其对应的登录信息和登录凭证一起存入redis中
        // 生成登录凭证即 token, 为了保证其唯一性，采用uuid生成
        String uuidToken = UUID.randomUUID().toString();
        uuidToken = uuidToken.replace("-", "");

        // 建立token和用户之间的联系, 过期时间1小时
        redisTemplate.opsForValue().set(uuidToken, userModel, 1, TimeUnit.HOURS);

        // 将登录凭证加入到用户登录成功的session内
//        this.httpServletRequest.getSession().setAttribute("IS_LOGIN", true);
//        this.httpServletRequest.getSession().setAttribute("LOGIN＿USER", userModel);

        return CommonReturnType.create(uuidToken);

    }


}
