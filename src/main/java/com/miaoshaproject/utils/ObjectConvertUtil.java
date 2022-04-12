package com.miaoshaproject.utils;

import com.miaoshaproject.controller.viewobject.ItemVO;
import com.miaoshaproject.controller.viewobject.UserVO;
import com.miaoshaproject.dataobject.*;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.PromoModel;
import com.miaoshaproject.service.model.UserModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import sun.misc.BASE64Encoder;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class ObjectConvertUtil {


    /**
     * 将userDO 和 userPasswordDO转换成,用于在各层之间交互的 userModel
     * @param userDO
     * @param userPasswordDO
     * @return
     */
    public UserModel convertFromDataObject(UserDO userDO, UserPasswordDO userPasswordDO) {
        if (ObjectUtils.isEmpty(userDO)) {
            return null;
        }
        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDO, userModel);
        if (!ObjectUtils.isEmpty(userPasswordDO)) {
            userModel.setEncrptPassword(userPasswordDO.getEncrptPassword());
        }

        return userModel;
    }


    /**
     * 将 userModel 对象 转换成 可以返回给前端的 userVO 对象
     * @param userModel
     * @return
     */
    public UserVO convertFromModel(UserModel userModel) {
        if (ObjectUtils.isEmpty(userModel)) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return userVO;
    }


    /**
     * 将 userModel 对象 转换成 存放到数据库中 userDO 对象
     * @param userModel
     * @return
     */
    public UserDO userDOconvertFromModel(UserModel userModel) {
        if (ObjectUtils.isEmpty(userModel)) {
            return null;
        }
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);
        return userDO;
    }



    /**
     * 将 userModel 对象 转换成 存放到数据库中 userpasswordDO 对象
     * @param userModel
     * @return
     */
    public UserPasswordDO userPasswordDOconvertFromModel(UserModel userModel) {
        if (ObjectUtils.isEmpty(userModel)) {
            return null;
        }
        UserPasswordDO userPasswordDO = new UserPasswordDO();
        userPasswordDO.setEncrptPassword(userModel.getEncrptPassword());
        userPasswordDO.setUserId(userModel.getId());
        return userPasswordDO;
    }


    /**
     * 使用md5加密
     * @param str
     * @return
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public String EncodeByMd5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        // 确定计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        BASE64Encoder base64Encoder = new BASE64Encoder();
        // 加密字符串
        String newStr = base64Encoder.encode(md5.digest(str.getBytes("utf-8")));
        return newStr;
    }

    /**
     * 将itemModel --> 数据库可存储的itemDO
     * @param itemModel
     * @return
     */
    public ItemDO convertItemDOFromItemModel(ItemModel itemModel) {
        if (ObjectUtils.isEmpty(itemModel)) {
            return null;
        }
        ItemDO itemDO = new ItemDO();
        BeanUtils.copyProperties(itemModel, itemDO);
        // 因为copyProperties是不会复制类型不一样的属性,所以这里需要额外设置一下
        itemDO.setPrice(itemModel.getPrice().doubleValue());

        return itemDO;
    }

    /**
     * 将 itemModel -> itemStockDO
     * @param itemModel
     * @return
     */
    public ItemStockDO convertItemStockFromItemModel(ItemModel itemModel) {
        if (ObjectUtils.isEmpty(itemModel)) {
            return null;
        }
        ItemStockDO itemStockDO = new ItemStockDO();
        itemStockDO.setItemId(itemModel.getId());
        itemStockDO.setStock(itemModel.getStock());

        return itemStockDO;
    }

    /**
     *
     * @param itemDO
     * @param itemStockDO
     * @return
     */
    public ItemModel convertFromDataObject(ItemDO itemDO, ItemStockDO itemStockDO) {
        if (ObjectUtils.isEmpty(itemDO) || ObjectUtils.isEmpty(itemStockDO)) {
            return null;
        }
        ItemModel itemModel = new ItemModel();
        BeanUtils.copyProperties(itemDO, itemModel);
        // 注意：因为价格数据类型问题，要重新设置一下
        itemModel.setPrice(new BigDecimal(itemDO.getPrice()));
        itemModel.setStock(itemStockDO.getStock());

        return itemModel;
    }


    /**
     *
     * @param itemModel
     * @return
     */
    public ItemVO converVOFromModel(ItemModel itemModel) {
        if (ObjectUtils.isEmpty(itemModel)) {
            return null;
        }
        ItemVO itemVO = new ItemVO();
        BeanUtils.copyProperties(itemModel, itemVO);
        if (!ObjectUtils.isEmpty(itemModel.getPromoModel())) {
            itemVO.setPromoStatus(itemModel.getPromoModel().getStatus());
            itemVO.setPromoId(itemModel.getPromoModel().getId());
            itemVO.setPromoStartTime(itemModel.getPromoModel().getStartTime().toString(DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")));
            itemVO.setPromoPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            itemVO.setPromoStatus(0);
        }

        return itemVO;
    }

    /**
     * 订单 model -> 订单DO 用于存放数据库
     * @param orderModel
     * @return
     */
    public OrderDO convertDOFromModel(OrderModel orderModel) {
        if (ObjectUtils.isEmpty(orderModel)) {
            return null;
        }
        OrderDO orderDO = new OrderDO();
        BeanUtils.copyProperties(orderModel, orderDO);
        orderDO.setItemPrice(orderModel.getItemPrice().doubleValue());
        orderDO.setOrderPrice(orderModel.getOrderPrice().doubleValue());

        return orderDO;
    }

    public PromoModel convertFromDataObject(PromoDO promoDO) {
        if (ObjectUtils.isEmpty(promoDO)) {
            return null;
        }
        PromoModel promoModel = new PromoModel();
        BeanUtils.copyProperties(promoDO, promoModel);
        promoModel.setPromoItemPrice(new BigDecimal(promoDO.getPromoItemPrice()));
        promoModel.setStartTime(new DateTime(promoDO.getStartTime()));
        promoModel.setEndTime(new DateTime(promoDO.getEndTime()));

        return promoModel;
    }
}
