package com.miaoshaproject;

import com.miaoshaproject.dao.UserDOMapper;
import com.miaoshaproject.dataobject.UserDO;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@MapperScan("com.miaoshaproject.dao")
@RestController
public class MiaoshaApplication {

	@Autowired
	private UserDOMapper userDOMapper;

	public static void main(String[] args) {

		System.out.println("hello");
		SpringApplication.run(MiaoshaApplication.class, args);
	}

	@RequestMapping("/")
	public String home() {
		UserDO userDO = userDOMapper.selectByPrimaryKey(1);
		if (userDO == null) {
			return "用户不存在";
		} else {
			return userDO.getName();
		}
	}

}
