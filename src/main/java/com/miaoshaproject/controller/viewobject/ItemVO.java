package com.miaoshaproject.controller.viewobject;

import lombok.Data;
import org.joda.time.DateTime;

import java.math.BigDecimal;

@Data
public class ItemVO {
    private Integer id;

    /**
     * 商品名称
     */
    private String title;

    /**
     * 商品价格
     */
    private BigDecimal price;

    /**
     * 商品库存
     */
    private Integer stock;

    /**
     * 商品的描述
     */
    private String description;

    /**
     * 商品的销量
     */
    private Integer sales;

    /**
     * 商品描述图片
     */
    private String imgUrl;

    /**
     * 记录商品是否在秒杀活动中，以及对应的状态0：没有秒杀活动； 1：秒杀活动待开始；2：秒杀活动进行中
     */
    private Integer promoStatus;

    /**
     * 秒杀价格
     */
    private BigDecimal promoPrice;

    /**
     * 秒杀活动id
     */
    private Integer promoId;

    /**
     * 秒杀活动开始时间
     */
    private String promoStartTime;
}
