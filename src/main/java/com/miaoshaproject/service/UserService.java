package com.miaoshaproject.service;

import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.service.model.UserModel;

public interface UserService {

    /**
     * 根据用户id 查询用户的信息
     * @param id
     * @return 用于各层的user对象
     */
    UserModel getUserById(Integer id);

    /**
     * 注册
     * @param userModel
     * @throws BusinessException
     */
    void register(UserModel userModel) throws BusinessException;

    /**
     * 登录
     * @param telphone
     * @param encrptPassword 用户加密后的密码
     */
    UserModel login(String telphone, String encrptPassword) throws BusinessException;

    /**
     * 在缓存中获取用户
     * @param id
     * @return
     */
    UserModel getUserByIdInCache(Integer id);
}
