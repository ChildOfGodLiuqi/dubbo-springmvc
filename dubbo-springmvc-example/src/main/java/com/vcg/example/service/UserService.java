package com.vcg.example.service;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import com.vcg.example.model.User;

/**
 * 
 * @author wuyu
 *
 */
public interface UserService {

	/**
	 * 以下是基于springmvc注解,实现rest
	 */

	public User findById(Integer id);

	public User register(User user);

	public String delete(HttpServletRequest request, HttpServletResponse response);

	public List<String> upload(MultipartFile file) throws IOException;

	/**
	 * 
	 * 
	 * 以下是自动生成url
	 */
	
	// http://localhost:8090/defaultGroup/0.0.0/userService/getById?id=1
	public User getById(Integer id);
	
	
	public void testRequest(HttpServletRequest request,HttpServletResponse response);
	
	//请求头为json,自动注入
	public List<User> testRequestBody(@RequestBody List<User> users);
	
	public List<String> testUpload(MultipartFile file) throws IOException;

	
	// http://localhost:8090/defaultGroup/0.0.0/userService/deleteById?id=1
	public void deleteById(Integer id);

	// http://localhost:8090/defaultGroup/0.0.0/userService/deleteById?id=1&username=wuyu&password=1
	public Integer insert(User user);

	
	// http://localhost:8090/defaultGroup/0.0.0/userService/testException
	public void testException(Integer id);

	
	// http://localhost:8090/defaultGroup/0.0.0/userService/testErrorMsgException
	public void testErrorMsgException(Integer id);

}
