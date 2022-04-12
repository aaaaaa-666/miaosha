package com.miaoshaproject.controller;

import com.miaoshaproject.controller.viewobject.ItemVO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.CacheService;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.PromoService;
import com.miaoshaproject.service.model.ItemModel;
import com.miaoshaproject.utils.ObjectConvertUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/item")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class ItemController extends BaseController {

    @Autowired
    private ItemService itemService;
    @Autowired
    ObjectConvertUtil objectConvertUtil;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    CacheService cacheService;
    @Autowired
    PromoService promoService;

    /**
     * 创建商品接口
     * @param title
     * @param description
     * @param price
     * @param stock
     * @param imgUrl
     * @return
     */
    @RequestMapping(value = "/create", method = {RequestMethod.POST}, consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createItem(@RequestParam(name = "title") String title,
                                       @RequestParam(name = "description") String description,
                                       @RequestParam(name = "price")BigDecimal price,
                                       @RequestParam(name = "stock") Integer stock,
                                       @RequestParam(name = "imgUrl") String imgUrl) throws BusinessException {

        // 封装service请求用来创建商品
        ItemModel itemModel = new ItemModel();
        itemModel.setTitle(title);
        itemModel.setDescription(description);
        itemModel.setPrice(price);
        itemModel.setStock(stock);
        itemModel.setImgUrl(imgUrl);

        ItemModel itemModelForReturn = itemService.createItem(itemModel);
        ItemVO itemVO = objectConvertUtil.converVOFromModel(itemModelForReturn);

        return CommonReturnType.create(itemVO);
    }

    /**
     * 获取商品详情信息
     * @param id
     * @return
     */
    @RequestMapping(value = "/get", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType getItem(@RequestParam(name = "id") Integer id) {

        ItemModel itemModel = null;

        // 1. 先取本地缓存
        itemModel = (ItemModel) cacheService.getFromCommonCache("item_" + id);

        if (ObjectUtils.isEmpty(itemModel)) {
            // 2. 本地缓存没有的华，再到 redis缓存中 获取相应数据
            itemModel = (ItemModel) redisTemplate.opsForValue().get("item_" + id);

            // 3. 若redis缓存中不存在，则通过itemService去数据库获取
            if (ObjectUtils.isEmpty(itemModel)) {
                itemModel = itemService.getItemById(id);
                // 获取到数据，并存入 redis中, 过期时间为 10分钟
                redisTemplate.opsForValue().set("item_" + id, itemModel, 10, TimeUnit.MINUTES);
            }
            // 填充本地缓存
            cacheService.setCommonCache("item_" + id, itemModel);
        }

        ItemVO itemVO = objectConvertUtil.converVOFromModel(itemModel);

        return CommonReturnType.create(itemVO);
    }


    /**
     * 获取商品列表
     * @return
     */
    @RequestMapping(value = "/listItem", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType listItem() {
        List<ItemModel> itemModelList = itemService.listItem();
        List<ItemVO> itemVOList = itemModelList.stream().map(itemModel -> {
            ItemVO itemVO = objectConvertUtil.converVOFromModel(itemModel);
            return itemVO;
        }).collect(Collectors.toList());

        return CommonReturnType.create(itemVOList);
    }


    /**
     * 发布活动
     * @param id
     * @return
     */
    @RequestMapping(value = "/publishPromo", method = {RequestMethod.GET})
    @ResponseBody
    public CommonReturnType publishPromo(@RequestParam(name = "id") Integer id) {
        promoService.publishPromo(id);
        return CommonReturnType.create(null);
    }
}
