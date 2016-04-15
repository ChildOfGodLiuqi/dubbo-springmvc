package com.alibaba.dubbo.rpc.protocol.springmvc.entity;

import java.io.Serializable;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class RequestEntity implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String group;

	private String version;

	private Object[] args;

	private String method;

	private String service;

	private String contextPath;

	private Object result;

	public RequestEntity(JSONObject jsonObject, String contextPath) {
		if (jsonObject != null) {
			this.group = jsonObject.getString("group");
			this.version = jsonObject.getString("version");
			this.method = jsonObject.getString("method");
			this.service = jsonObject.getString("service");
			this.contextPath = jsonObject.getString("contextPath");
			if (contextPath == null) {
				this.contextPath = contextPath;
			}
			JSONArray jsonArray = jsonObject.getJSONArray("args");
			if (jsonArray != null) {
				this.args = jsonArray.toArray();
			}

		}
	}

	public RequestEntity() {
		super();
		// TODO Auto-generated constructor stub
	}

	public RequestEntity(String group, String version, String service, String method, Object[] args,
			String contextPath) {
		super();
		this.group = group;
		this.version = version;
		this.args = args;
		this.method = method;
		this.service = service;
		this.contextPath = contextPath;
	}

	public String mappingUrl() {
		StringBuffer sb = new StringBuffer("/");
		
		if (StringUtils.isBlank(group)) {
			sb.append("defaultGroup");
		} else {
			sb.append(this.group);
		}

		sb.append("/");

		if (StringUtils.isBlank(version)) {
			sb.append("0.0.0");
		} else {
			sb.append(this.version);
		}

		sb.append("/");

		String[] split = this.service.split("[.]");

		sb.append(firstLow(split[split.length - 1]));
		sb.append("/");

		sb.append(method);

		return sb.toString();
	}

	public String firstLow(String str) {
		return str.substring(0, 1).toLowerCase() + str.substring(1);
	}

	public String getGroup() {
		return group;
	}

	public void setGroup(String group) {
		this.group = group;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getMethod() {
		return method;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public Object[] getArgs() {
		return args;
	}

	public void setArgs(Object[] args) {
		this.args = args;
	}

	public String getContextPath() {
		return contextPath;
	}

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

	public String getService() {
		return service;
	}

	public void setService(String service) {
		this.service = service;
	}

	public Object getResult() {
		return result;
	}

	public void setResult(Object result) {
		this.result = result;
	}

}
