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
     * εε»Ίεε
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

        // 1. ζ ‘ιͺε₯ε
        if (StringUtils.isEmpty(title) || ObjectUtils.isEmpty(price) || ObjectUtils.isEmpty(stock)
            || StringUtils.isEmpty(description) || StringUtils.isEmpty(imgUrl)) {

            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        // θ½¬εitemmodel -> dataobject
        ItemDO itemDO = objectConvertUtil.convertItemDOFromItemModel(itemModel);



        // 2. εε₯ζ°ζ?εΊ
           // itemDOε₯εΊ
        itemDOMapper.insertSelective(itemDO);
        itemModel.setId(itemDO.getId());

        ItemStockDO itemStockDO = objectConvertUtil.convertItemStockFromItemModel(itemModel);

          // itemStockDOε₯εΊ
        itemStockDOMapper.insertSelective(itemStockDO);

        // 3. θΏεεε»Ίε?ζηε―Ήθ±‘

        return this.getItemById(itemModel.getId());
    }

    /**
     * εεεθ‘¨ζ΅θ§
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
     * εεθ―¦ζζ΅θ§
     * @param id
     * @return
     */
    @Override
    public ItemModel getItemById(Integer id) {
        // 1. θ·εεε
        ItemDO itemDO = itemDOMapper.selectByPrimaryKey(id);
        if (ObjectUtils.isEmpty(itemDO)) {
            return null;
        }
        // 2. θ·εεΊε­ζ°ι
        ItemStockDO itemStockDO = itemStockDOMapper.selectByItemId(itemDO.getId());

        // 3. ε°dataobject - > itemModel
        ItemModel itemModel = objectConvertUtil.convertFromDataObject(itemDO, itemStockDO);

        // 4. θ·εζ΄»ε¨εεδΏ‘ζ―
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
     * εεΊε­
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
            // ζ΄ζ°εΊε­ζε, δΈι’ε¨ redisδΈ­εεΊε­οΌηΆειθΏζΆζ―ιε θ?©ζ°ζ?εΊεεΊε­
            return true;
        } else if (result == 0) {
            // ε δΈεεε?η½ηζ θ―
            redisTemplate.opsForValue().set("promo_item_stock_invalid_" + itemId, "true");

            // ζ΄ζ°εΊε­ζε
            return true;
        } else {
            // ζ΄ζ°εΊε­ε€±θ΄₯
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
     * εεε’ε ιι
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
     * εε§εεΊε­ζ΅ζ°΄
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
