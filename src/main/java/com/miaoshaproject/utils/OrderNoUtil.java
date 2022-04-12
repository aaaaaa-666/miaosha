package com.miaoshaproject.utils;

import com.miaoshaproject.dao.SequenceDOMapper;
import com.miaoshaproject.dataobject.SequenceDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class OrderNoUtil {

    @Autowired(required = false)
    SequenceDOMapper sequenceDOMapper;

    /**
     * 生成订单号： 16位
     * 前 8 位 ：年月日
     * 中间 6 位：自增序列
     * 最后两位：分库分表位
     * @return
     */
    /**
     *propagation = Propagation.REQUIRES_NEW, 不管当前是否存在事务，它都开启一个新事务运行
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String gererateOrderNo() {
        // 订单号16位
        StringBuilder stringBuilder = new StringBuilder();
        // 前 8 位 年月日
        LocalDateTime now = LocalDateTime.now();
        String nowDate = now.format(DateTimeFormatter.ISO_DATE).replace("-", "");
        stringBuilder.append(nowDate);

        // 中间6位
        int sequence = 0;
        SequenceDO sequenceDO = sequenceDOMapper.getSequenceByName("order_info");
        sequence = sequenceDO.getCurrentValue();
        sequenceDO.setCurrentValue(sequenceDO.getCurrentValue() + sequenceDO.getStep());
        sequenceDOMapper.updateByPrimaryKeySelective(sequenceDO);
        String sequenceStr= String.valueOf(sequence);
        for (int i = 0; i < 6 - sequenceStr.length(); i++) {
            stringBuilder.append(0);
        }
        stringBuilder.append(sequenceStr);

        // 最后2位，分库分表位。 暂时先写死
        stringBuilder.append("00");

        return stringBuilder.toString();
    }
}
