package com.miaoshaproject.service;

import com.miaoshaproject.service.model.PromoModel;

public interface PromoService {

    /**
     * 根据商品id 获取即将进行或者正在进行的秒杀活动
     * @param itemId
     * @return
     */
    PromoModel getPromoByItemId(Integer itemId);

    /**
     * 发布活动
     * @param promoId
     */
    void publishPromo(Integer promoId);
}
