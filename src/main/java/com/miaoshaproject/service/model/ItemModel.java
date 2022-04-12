package com.miaoshaproject.service.model;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class ItemModel implements Serializable {
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
     * 适用聚合模型，如果promoModel不为空，则表示其拥有还未结束的秒杀活动
     */
    private PromoModel promoModel;
}
