package com.vcg.example.service.impl;

import java.io.IOException;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import com.vcg.example.model.User;
import com.vcg.example.service.UserService;

/**
 * 
 * @author wuyu
 *
 */

// springmvc注解
@RequestMapping("/user")
public class UserServiceImpl implements UserService {

	/**
	 * 以下是基于springmvc注解,实现rest
	 */

	// 可以不指定produce 默认会自动序列化成json
	@RequestMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public User findById(@PathVariable("id") Integer id) {
		return new User()
				.setId(id)
				.setPassword("123456")
				.setRegisterDate(new Date())
				.setUsername("test")
				.build();
	}

	// 只接受 请求头为application/json
	@RequestMapping(value = "register", consumes = MediaType.APPLICATION_JSON_VALUE)
	// 只做简单返回
	public User register(@RequestBody User user) {
		return user;
	}

	@RequestMapping(value = "delete")
	public String delete(HttpServletRequest request, HttpServletResponse response) {
		String id = request.getParameter("id");
		return id;
	}

	@RequestMapping(value = "upload")
	public List<String> upload(MultipartFile file) throws IOException {
		List<String> readLines = IOUtils.readLines(file.getInputStream());
		return readLines;
	}

	/**
	 * 
	 * 
	 * 以下是自动生成url
	 */
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
	// http://localhost:8090/defaultGroup/0.0.0/userService/testException
	public void testException(Integer id) {
		if (id == null || id.equals("testException")) {
			throw new RuntimeException("未找到相关用户");
		}
	}

	@Override
	// http://localhost:8090/defaultGroup/0.0.0/userService/testErrorMsgException
	public void testErrorMsgException(Integer id) {
		if (id == null || id.equals("testException")) {
			throw new RuntimeException();
		}
	}

}
