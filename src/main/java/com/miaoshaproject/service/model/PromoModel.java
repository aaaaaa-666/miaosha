package com.miaoshaproject.service.model;

import lombok.Data;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 秒杀活动模型
 */
@Data
public class PromoModel implements Serializable {

    private Integer id;

    /**
     * 商品秒杀的状态。 1:还未开始； 2：正在进行； 3:已结束
     */
    private Integer status;

    /**
     * 秒杀活动名称
     */
    private String promoName;

    /**
     * 秒杀活动的开始时间
     */
    private DateTime startTime;

    /**
     * 秒杀活动结束时间
     */
    private DateTime endTime;

    /**
     * 参与秒杀活动的商品
     */
    private Integer itemId;

    /**
     * 秒杀活动时，商品的价格
     */
    private BigDecimal promoItemPrice;
}
