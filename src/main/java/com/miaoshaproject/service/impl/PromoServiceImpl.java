package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.PromoDOMapper;
import com.miaoshaproject.dataobject.PromoDO;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.PromoModel;
import com.miaoshaproject.utils.ObjectConvertUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class PromoServiceImpl implements PromoService {

    @Autowired(required = false)
    PromoDOMapper promoDOMapper;
    @Autowired
    ObjectConvertUtil objectConvertUtil;
    @Autowired
    ItemService itemService;
    @Autowired
    RedisTemplate redisTemplate;

    /**
     * 根据商品id 获取即将进行或者正在进行的秒杀活动
     * @param itemId
     * @return
     */
    @Override
    public PromoModel getPromoByItemId(Integer itemId) {
        PromoDO promoDO = promoDOMapper.selectByItemId(itemId);

        // DO -> model
        PromoModel promoModel = objectConvertUtil.convertFromDataObject(promoDO);

        if (ObjectUtils.isEmpty(promoModel)) {
            return null;
        }
        // 判断当前时间是否秒杀活动即将开始或者正在进行
        if (promoModel.getStartTime().isAfterNow()) {
            // 活动还未开始
            promoModel.setStatus(1);
        } else if (promoModel.getEndTime().isBeforeNow()) {
            // 活动已结束
            promoModel.setStatus(3);
        } else {
            promoModel.setStatus(2);
        }



        return promoModel;
    }

    @Override
    public void publishPromo(Integer promoId) {
        PromoDO promoDO = promoDOMapper.selectByPrimaryKey(promoId);
        if (promoDO.getItemId() == null || promoDO.getItemId() == 0) {
            return;
        }
        ItemModel itemModel = itemService.getItemById(promoDO.getItemId());

        // 将库存同步到redis内
        redisTemplate.opsForValue().set("promo_item_stock_" + itemModel.getId(), itemModel.getStock());
    }

}
