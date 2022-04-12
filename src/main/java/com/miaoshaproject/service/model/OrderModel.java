package com.miaoshaproject.service.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户下单的交易模型
 */
@Data
public class OrderModel {

    /**
     * 订单id,前面常以时间拼接，所以这里用String 。20201212xxxxxxx
     */
    private String id;

    /**
     * 购买者的id
     */
    private Integer userId;

    /**
     * 购买的商品id
     */
    private Integer itemId;

    /**
     * 购买商品的价格，若promoId非空，则表示秒杀商品价格
     */
    private BigDecimal itemPrice;

    /**
     * 购买数量
     */
    private Integer amount;

    /**
     * 购买的金额, 若promoId非空，则表示秒杀商品的订单金额
     */
    private BigDecimal orderPrice;

    /**
     * 若非空，则表示以秒杀商品方式下单
     */
    private Integer promoId;

}
