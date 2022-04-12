package com.miaoshaproject.controller;

import com.miaoshaproject.dataobject.UserDO;
import com.miaoshaproject.error.BusinessException;
import com.miaoshaproject.error.EmBusinessError;
import com.miaoshaproject.mq.MqProducer;
import com.miaoshaproject.response.CommonReturnType;
import com.miaoshaproject.service.ItemService;
import com.miaoshaproject.service.OrderService;
import com.miaoshaproject.service.model.OrderModel;
import com.miaoshaproject.service.model.UserModel;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/order")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class OrderController extends BaseController{

    @Autowired(required = false)
    OrderService orderService;
    @Autowired(required = false)
    HttpServletRequest httpServletRequest;
    @Autowired
    RedisTemplate redisTemplate;
    @Autowired
    MqProducer mqProducer;
    @Autowired
    ItemService itemService;

    /**
     * 创建订单接口
     * @param itemId
     * @param amount
     * @return
     */
    @RequestMapping(value = "/createOrder", method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType createOrder(@RequestParam(name = "itemId") Integer itemId,
                                        @RequestParam(name = "amount") Integer amount,
                                        @RequestParam(name = "promoId", required = false) Integer promoId) throws BusinessException {

        // 下面这种获取前端传过来的token的方式，可以换为，在参数中加RequestParam来获取
        String token = httpServletRequest.getParameterMap().get("token")[0];
        if (StringUtils.isEmpty(token)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN);
        }
        // 根据前端传过来的 token 去 redis中取相应的user对象（如果出来的token失效或者是假的，则取不到）
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get(token);

        if (ObjectUtils.isEmpty(userModel)) {
            throw new BusinessException(EmBusinessError.USER_NOT_LOGIN, "用户未登录，暂不能下单");
        }

//        OrderModel orderModel = orderService.createOrder(userModel.getId(), itemId, promoId, amount);

        // 判断商品是否售罄
        if (redisTemplate.hasKey("promo_item_stock_invalid_" + itemId)) {
            throw new BusinessException(EmBusinessError.STOCK_NOT_ENOUGH);
        }

        // 加入库存流水init状态，一遍后续对订单情况可进行追踪
        String stockLogId = itemService.initStockLog(itemId, amount);

        if (!mqProducer.transactionAsyncReduceStock(userModel.getId(), itemId, promoId, amount, stockLogId)) {
            throw new BusinessException(EmBusinessError.UNKNOWN_ERROR, "下单失败");
        };
        return CommonReturnType.create(null);
    }
}
