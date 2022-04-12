package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.UserDOMapper;
import com.miaoshaproject.dao.UserPasswordDOMapper;
import com.miaoshaproject.dataobject.UserDO;
import com.miaoshaproject.dataobject.UserPasswordDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.UserModel;
import com.miaoshaproject.utils.ObjectConvertUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired(required = false)
    private UserDOMapper userDOMapper;
    @Autowired(required = false)
    private UserPasswordDOMapper userPasswordDOMapper;
    @Autowired
    private ObjectConvertUtil objectConvertUtil;
    @Autowired
    RedisTemplate redisTemplate;

    /**
     * 根据用户id 查询用户的信息
     * @param id
     * @return 用于各层的user对象
     */
    @Override
    public UserModel getUserById(Integer id) {
        UserDO userDO = userDOMapper.selectByPrimaryKey(id);

        if (ObjectUtils.isEmpty(userDO)) {
            return null;
        }
        // 通过用户id 获取对应的用户加密的密码信息
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(id);

        return objectConvertUtil.convertFromDataObject(userDO, userPasswordDO);
    }


    /**
     * 注册
     * @param userModel
     * @throws BusinessException
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserModel userModel) throws BusinessException {
        if (ObjectUtils.isEmpty(userModel)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }
        if (StringUtils.isEmpty(userModel.getName())
                || userModel.getGender() == null
                || userModel.getAge() == null
                || StringUtils.isEmpty(userModel.getTelphone())) {

            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        // 将model -> userDO用于存放到数据库
        UserDO userDO = objectConvertUtil.userDOconvertFromModel(userModel);
        try{
            userDOMapper.insertSelective(userDO);
        }catch (DuplicateKeyException e) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "手机号重复注册!");
        }


        // 因为前端并没有传id, 所以此时userModel中没id,需要设置一下,才能将此传给userPasswordDO中作为其 user_id
        // 因为 userDO中id是自增的所以可以直接用
        UserDO newUserDO = userDOMapper.selectByTelphone(userDO.getTelphone());
        userModel.setId(newUserDO.getId());

        // 将model -> userPasswordDO用于存放到数据库
        UserPasswordDO userPasswordDO = objectConvertUtil.userPasswordDOconvertFromModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);

        return;


    }
    /**
     * 登录　
     * @param telphone
     * @param encrptPassword
     */
    @Override
    public UserModel login(String telphone, String encrptPassword) throws BusinessException {
        // 通过用户的手机，获取用户的信息
        UserDO userDO = userDOMapper.selectByTelphone(telphone);
        if (org.apache.commons.lang3.ObjectUtils.isEmpty(userDO)) {
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }

        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        UserModel userModel = objectConvertUtil.convertFromDataObject(userDO, userPasswordDO);

        // 对比用户信息内加密的密码是否和传输的密码相匹配
        if (!StringUtils.equals(encrptPassword, userModel.getEncrptPassword())) {
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }

        return userModel;
    }

    @Override
    public UserModel getUserByIdInCache(Integer id) {
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get("user_" + id);
        if (ObjectUtils.isEmpty(userModel)) {
            userModel = this.getUserById(id);
            redisTemplate.opsForValue().set("user_" + id, userModel, 10, TimeUnit.MINUTES);
        }
        return userModel;
    }


}
