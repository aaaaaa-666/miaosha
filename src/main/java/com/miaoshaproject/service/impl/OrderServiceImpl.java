package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.OrderDOMapper;
import com.miaoshaproject.dao.StockLogDOMapper;
import com.miaoshaproject.dataobject.OrderDO;
import com.miaoshaproject.dataobject.StockLogDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.UserService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import com.miaoshaproject.utils.ObjectConvertUtil;
import com.miaoshaproject.utils.OrderNoUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    ItemService itemService;
    @Autowired
    UserService userService;
    @Autowired
    OrderDOMapper orderDOMapper;
    @Autowired
    ObjectConvertUtil objectConvertUtil;
    @Autowired
    OrderNoUtil orderNoUtil;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    StockLogDOMapper stockLogDOMapper;


    /**
     * 创建订单
     * @param userId
     * @param itemId
     * @param promoId
     * @param amount
     * @return
     * @throws BusinessException
     */
    @Override
    @Transactional
    public OrderModel createOrder(Integer userId, Integer itemId, Integer promoId, Integer amount, String stockLogId) throws BusinessException {

        // 1. 校验下单状态，下单的商品是否存在，用户是否合法，购买数量是否正确
        // ItemModel itemModel = itemService.getItemById(itemId);
        ItemModel itemModel = itemService.getItemByIdInCache(itemId);
        if (ObjectUtils.isEmpty(itemModel)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "商品信息不存在");
        }
        //这是优化前的代码，直接去数据库中取UserModel userModel = userService.getUserById(userId);
        // 下面是优化后的，先去缓存中取，取不到的话，再去数据库中取
        UserModel userModel = userService.getUserByIdInCache(userId);
        if (ObjectUtils.isEmpty(userModel)) {
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }
        if (amount <= 0 || amount > 99) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "购买数量不合理");
        }

        // 活动信息校验
        if(!ObjectUtils.isEmpty(promoId)) {
            // (1)校验对应活动是否存在这个适用商品
            if (!itemModel.getPromoModel().getId().equals(promoId)) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动信息不正确");
            // (2)校验活动是否正在进行中
            } else if (itemModel.getPromoModel().getStatus() != 2) {
                throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "活动未开始");
            }
        }

        // 2. 下单减库存
        boolean result = itemService.decreaseStock(itemId, amount);
        if (!result) {
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        // 3. 订单入库
        OrderModel orderModel = new OrderModel();
        orderModel.setItemId(itemId);
        orderModel.setUserId(userId);
        // 根据promoId, 判断价格为正常价格，还是秒杀价格
        if (promoId != null) {
            orderModel.setItemPrice(itemModel.getPromoModel().getPromoItemPrice());
        } else {
            orderModel.setItemPrice(itemModel.getPrice());
        }
        orderModel.setAmount(amount);
        orderModel.setPromoId(promoId);
        orderModel.setOrderPrice(orderModel.getItemPrice().multiply(new BigDecimal(amount)));

        // 生成交易流水号，订单号
        orderModel.setId(orderNoUtil.gererateOrderNo());
        OrderDO orderDO = objectConvertUtil.convertDOFromModel(orderModel);

        // 订单信息保存至数据库
        orderDOMapper.insertSelective(orderDO);
        // 加上商品的销量
        itemService.increaseSales(itemId, amount);

        // 设置库存流水状态为成功
        StockLogDO stockLogDO = stockLogDOMapper.selectByPrimaryKey(stockLogId);
        if (ObjectUtils.isEmpty(stockLogDO)) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR);
        }
        // 2表示下单扣库存成功
        stockLogDO.setStatus(2);
        stockLogDOMapper.updateByPrimaryKeySelective(stockLogDO);


//        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
//            @Override
//            public void afterCommit() {
//                // 异步更新库存
//                boolean mqResult = itemService.asyncDecreaseStock(itemId, amount);
////                if (!mqResult) {
////                    itemService.increaseStock(itemId, amount);
////                    throw new BusinessException(EmBusinessError.MQ_SEND_FAIL);
////                }
//            }
//        });

        // 4. 返回前端
        return orderModel;
    }
}
