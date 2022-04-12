package com.miaoshaproject.service.model;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserModel implements Serializable {

    private Integer id;
    private String name;
    private Byte gender;
    private Integer age;
    private String telphone;
    private String registerModel;
    private String thirdPartyId;

    private String encrptPassword;
}
