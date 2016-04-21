package com.vcg.example.model;

import java.util.Date;

import com.alibaba.fastjson.JSON;

public class User {

	private Integer id;

	private String username;

	private String password;

	private Date registerDate;

	public User(Integer id, String username, String password, Date registerDate) {
		super();
		this.id = id;
		this.username = username;
		this.password = password;
		this.registerDate = registerDate;
	}

	public User() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Integer getId() {
		return id;
	}

	public User setId(Integer id) {
		this.id = id;
		return this;
	}

	public String getUsername() {
		return username;
	}

	public User setUsername(String username) {
		this.username = username;
		return this;
	}

	public String getPassword() {
		return password;
	}

	public User setPassword(String password) {
		this.password = password;
		return this;
	}

	public Date getRegisterDate() {
		return registerDate;
	}

	public User setRegisterDate(Date registerDate) {
		this.registerDate = registerDate;
		return this;
	}

	@Override
	public String toString() {
		return JSON.toJSONString(this);
	}
	
	public User build(){
		return new User(id, username, password, registerDate);
	}

}
