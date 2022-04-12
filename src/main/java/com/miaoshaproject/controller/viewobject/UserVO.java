package com.miaoshaproject.controller.viewobject;

import lombok.Data;

/*
*  为了安全,该对象用于返回给前端的
* */
@Data
public class UserVO {

    private Integer id;
    private String name;
    private Byte gender;
    private Integer age;
    private String telphone;

}
