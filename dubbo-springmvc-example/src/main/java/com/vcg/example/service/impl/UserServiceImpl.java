package com.vcg.example.service.impl;

import java.util.Date;

import javax.management.RuntimeErrorException;

import com.alibaba.dubbo.rpc.protocol.springmvc.ErrorMsg;
import com.vcg.example.model.User;
import com.vcg.example.service.UserService;

/**
 * 
 * @author wuyu
 *
 */
public class UserServiceImpl implements UserService {

	@Override
	// http://localhost:8090/defaultGroup/0.0.0/userService/getById?id=1
	public User getById(Integer id) {
		return new User(id, "test1", "123456", new Date());
	}

	@Override
	// http://localhost:8090/defaultGroup/0.0.0/userService/deleteById?id=1
	public void deleteById(Integer id) {
		System.out.println("删除用户 :" + id);
	}

	@Override
	// http://localhost:8090/defaultGroup/0.0.0/userService/deleteById?id=1&username=wuyu&password=1
	public Integer insert(User user) {
		System.out.println("插入用户" + user.toString());
		user.setId(1);
		return user.getId();
	}

	@Override
	//http://localhost:8090/defaultGroup/0.0.0/userService/testException
	public void testException(Integer id) {
		if (id == null || id.equals("testException")) {
			throw new RuntimeException("未找到相关用户");
		}
	}

	@Override
	//http://localhost:8090/defaultGroup/0.0.0/userService/testErrorMsgException
	@ErrorMsg(msg = "测试自定义测试信息", responseType = "text/xml;charset=utf-8")
	public void testErrorMsgException(Integer id) {
		if (id == null || id.equals("testException")) {
			throw new RuntimeException();
		}
	}

}
