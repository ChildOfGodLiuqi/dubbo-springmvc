package com.alibaba.dubbo.rpc.protocol.springmvc.entity;

import java.io.Serializable;

import org.springframework.util.ClassUtils;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.fastjson.JSONObject;

public class RequestEntity implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String group;

	private String version;

	private Object[] args;

	private Object argsType[];

	private String method;

	private Class service;

	private String contextPath;

	public RequestEntity(JSONObject jsonObject) {
		if (jsonObject != null) {
			this.group = jsonObject.getString("group");
			this.version = jsonObject.getString("version");
			this.args = jsonObject.getJSONArray("args").toArray();
			this.setArgsType(jsonObject.getJSONArray("argsType").toArray());
			this.method = jsonObject.getString("method");
			String serviceClassName = jsonObject.getString("service");
			this.contextPath = jsonObject.getString("contextPath");
			try {
				this.service = ClassUtils.forName(serviceClassName, ClassUtils.getDefaultClassLoader());
			} catch (ClassNotFoundException e) {
				throw new RpcException(e);
			} catch (LinkageError e) {
				throw new RpcException(e);
			}
		}
	}

	public String getMappingUrl() {
		StringBuffer sb = new StringBuffer();
		if (!contextPath.equals("") || !contextPath.equals("/")) {
			sb.append(this.contextPath);
			sb.append("/");
		}

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

		sb.append(firstLow(service.getSimpleName()));
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

	public Class getService() {
		return service;
	}

	public void setService(Class service) {
		this.service = service;
	}

	public Object[] getArgsType() {
		return argsType;
	}

	public void setArgsType(Object argsType[]) {
		this.argsType = argsType;
	}

}
