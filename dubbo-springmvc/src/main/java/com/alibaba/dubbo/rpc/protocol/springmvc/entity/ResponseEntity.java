package com.alibaba.dubbo.rpc.protocol.springmvc.entity;

import java.io.Serializable;

public class ResponseEntity implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Object result;

	private int status;

	private String msg;

	public Object getResult() {
		return result;
	}

	public ResponseEntity setResult(Object result) {
		this.result = result;
		return this;
	}

	public int getStatus() {
		return status;

	}

	public ResponseEntity setStatus(int status) {
		this.status = status;
		return this;
	}

	public String getMsg() {
		return msg;
	}

	public ResponseEntity setMsg(String msg) {
		this.msg = msg;
		return this;
	}

}
