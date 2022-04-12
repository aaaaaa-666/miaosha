package com.miaoshaproject.service.impl;

import com.miaoshaproject.dao.ItemDOMapper;
import com.miaoshaproject.dao.ItemStockDOMapper;
import com.miaoshaproject.dao.StockLogDOMapper;
import com.miaoshaproject.dataobject.ItemDO;
import com.miaoshaproject.dataobject.ItemStockDO;
import com.miaoshaproject.dataobject.StockLogDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.mq.MqProducer;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.service.model.PromoModel;
import com.miaoshaproject.utils.ObjectConvertUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ItemServiceImpl implements ItemService {


    @Autowired(required = false)
    ItemDOMapper itemDOMapper;
    @Autowired
    ObjectConvertUtil objectConvertUtil;
    @Autowired(required = false)
    ItemStockDOMapper itemStockDOMapper;
    @Autowired
    PromoService promoService;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    MqProducer producer;
    @Autowired
    StockLogDOMapper stockLogDOMapper;


    /**
     * 创建商品
     * @param itemModel
     * @return
     */
    @Override
    @Transactional
    public ItemModel createItem(ItemModel itemModel) throws BusinessException {
        String title = itemModel.getTitle();
        Double price = itemModel.getPrice().doubleValue();
        Integer stock = itemModel.getStock();
        String description = itemModel.getDescription();
        String imgUrl = itemModel.getImgUrl();

        // 1. 校验入参
        if (StringUtils.isEmpty(title) || ObjectUtils.isEmpty(price) || ObjectUtils.isEmpty(stock)
            || StringUtils.isEmpty(description) || StringUtils.isEmpty(imgUrl)) {

            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        // 转化itemmodel -> dataobject
        ItemDO itemDO = objectConvertUtil.convertItemDOFromItemModel(itemModel);



        // 2. 写入数据库
           // itemDO入库
        itemDOMapper.insertSelective(itemDO);
        itemModel.setId(itemDO.getId());

        ItemStockDO itemStockDO = objectConvertUtil.convertItemStockFromItemModel(itemModel);

          // itemStockDO入库
        itemStockDOMapper.insertSelective(itemStockDO);

        // 3. 返回创建完成的对象

        return this.getItemById(itemModel.getId());
    }

    /**
     * 商品列表浏览
     * @return
     */
    @Override
    public List<ItemModel> listItem() {
        List<ItemDO> itemDOList = itemDOMapper.listItem();
        List<ItemModel> itemModelList = itemDOList.stream().map(itemDO -> {
            ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());
            ItemModel itemModel = objectConvertUtil.convertFromDataObject(itemDO, itemStockDO);
            return itemModel;
        }).collect(Collectors.toList());

        return itemModelList;
    }

    /**
     * 商品详情浏览
     * @param id
     * @return
     */
    @Override
    public ItemModel getItemById(Integer id) {
        // 1. 获取商品
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        if (ObjectUtils.isEmpty(itemDO)) {
            return null;
        }
        // 2. 获取库存数量
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());

        // 3. 将dataobject - > itemModel
        ItemModel itemModel = objectConvertUtil.convertFromDataObject(itemDO, itemStockDO);

        // 4. 获取活动商品信息
        PromoModel promoModel = promoService.getPromoByItemId(itemModel.getId());
        if (!ObjectUtils.isEmpty(promoModel) && promoModel.getStatus() != 3) {
            itemModel.setPromoModel(promoModel);
        }

        return itemModel;
    }

    @Override
    public ItemModel getItemByIdInCache(Integer id) {
        ItemModel itemModel = (ItemModel) redisTemplate.opsForValue().get("item_" + id);
        if (ObjectUtils.isEmpty(itemModel)) {
            itemModel = this.getItemById(id);
            redisTemplate.opsForValue().set("item_" + id, itemModel, 10, TimeUnit.MINUTES);
        }
        return itemModel;
    }


    /**
     * 减库存
     * @param itemId
     * @param amount
     * @return
     * @throws BusinessException
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean decreaseStock(Integer itemId, Integer amount) throws BusinessException {
        //int affectedRow = itemStockDOMapper.decreaseStock(itemId, amount);
        Long result = redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount * -1);
        if (result > 0) {
            // 更新库存成功, 上面在 redis中减库存，然后通过消息队列 让数据库减库存
            return true;
        } else if (result == 0) {
            // 加上商品售罄的标识
            redisTemplate.opsForValue().set("promo_item_stock_invalid_" + itemId, "true");

            // 更新库存成功
            return true;
        } else {
            // 更新库存失败
            increaseStock(itemId, amount);
            return false;
        }

    }

    @Override
    public boolean increaseStock(Integer itemId, Integer amount) throws BusinessException {
        redisTemplate.opsForValue().increment("promo_item_stock_" + itemId, amount);
        return true;
    }

    /**
     * 商品增加销量
     * @param itemId
     * @param amount
     * @throws BusinessException
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void increaseSales(Integer itemId, Integer amount) throws BusinessException {
        itemDOMapper.increaseSales(itemId, amount);
    }

    @Override
    public boolean asyncDecreaseStock(Integer itemId, Integer amount) {
        boolean mqResult = producer.asyncReduceStock(itemId, amount);
        return mqResult;
    }


    /**
     * 初始化库存流水
     * @param itemId
     * @param amount
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String initStockLog(Integer itemId, Integer amount) {
        StockLogDO stockLogDO = new StockLogDO();
        stockLogDO.setItemId(itemId);
        stockLogDO.setAmount(amount);
        stockLogDO.setStockLogId(UUID.randomUUID().toString().replace("-", ""));
        stockLogDO.setStatus(1);

        stockLogDOMapper.insertSelective(stockLogDO);

        return stockLogDO.getStockLogId();

    }
}
