package com.alibaba.dubbo.rpc.protocol.springmvc.entity;

import java.io.Serializable;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class ResponseEntity implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Object result;

	public Object getResult() {
		return result;
	}

	public ResponseEntity setResult(Object result) {
		this.result = result;
		return this;
	}

}
