package com.alibaba.dubbo.rpc.protocol.springmvc.web;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.GenericTypeResolver;
import org.springframework.core.MethodParameter;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;

import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.springmvc.entity.RequestEntity;
import com.alibaba.dubbo.rpc.protocol.springmvc.entity.ResponseEntity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class SpringmvcHandlerInvoker {

	private Map<String, HandlerMethod> handlerMethods;

	private final String HESSIAN_TYPE = "application/hessain2";

	private final String JSON_TYPE = "application/json;charset=utf-8";

	@RequestMapping(value = { "/" }, consumes = { HESSIAN_TYPE }, produces = { HESSIAN_TYPE })
	@ResponseBody
	public ResponseEntity invokerHessain2(@RequestBody RequestEntity requestEntity) throws Exception {
		if(requestEntity.mappingUrl()==null){
			return new ResponseEntity().setResult(null).setStatus(500).setMsg("Missing parameter!!");
		}
		HandlerMethod handlerMethod = handlerMethods.get(requestEntity.mappingUrl());
		if (handlerMethod == null) {
			return new ResponseEntity().setResult(null).setStatus(404).setMsg("not find service!");
		}
		Object result = invokerHandler(handlerMethod, requestEntity.getArgs());
		return new ResponseEntity().setResult(result).setStatus(200).setMsg("success");
	}

	@RequestMapping(value = { "/" }, consumes = { JSON_TYPE }, produces = { JSON_TYPE })
	@ResponseBody
	public Object invokerJson(@RequestBody RequestEntity requestEntity) throws Exception {
		if(requestEntity.mappingUrl()==null){
			return new ResponseEntity().setResult(null).setStatus(500).setMsg("Missing parameter!!");
		}
		HandlerMethod handlerMethod = handlerMethods.get(requestEntity.mappingUrl());
		if (handlerMethod == null) {
			return new ResponseEntity().setResult(null).setStatus(404).setMsg("not find service!");
		}
		return invokerHandler(handlerMethod, handleJsonArgs(handlerMethod, requestEntity.getArgs()));
	}
	
	public SpringmvcHandlerInvoker(Map<String, HandlerMethod> handlerMethods) {
		super();
		this.handlerMethods = handlerMethods;
	}

	private Object invokerHandler(HandlerMethod handlerMethod, Object[] args) throws Exception {
		Method method = getHandlerMethodBridgeMethod(handlerMethod);
		return method.invoke(handlerMethod.getBean(), args);
	}

	private Method getHandlerMethodBridgeMethod(HandlerMethod handlerMethod) {
		Field bridgedMethodField = ReflectionUtils.findField(HandlerMethod.class, "bridgedMethod");
		bridgedMethodField.setAccessible(true);
		return (Method) ReflectionUtils.getField(bridgedMethodField, handlerMethod);
	}

	private Object[] handleJsonArgs(HandlerMethod handlerMethod, Object[] args) throws Exception {
		MethodParameter[] parameters = handlerMethod.getMethodParameters();
		if (args != null && args.length > 0) {
			for (int i = 0; i < parameters.length; i++) {
				if (args[i] == null) {
					continue;
				}
				Class<?> parameterType = GenericTypeResolver.resolveParameterType(parameters[i],
						handlerMethod.getBean().getClass());

				if (List.class.isAssignableFrom(parameterType) || Set.class.isAssignableFrom(parameterType)) {
					ParameterizedType genericParameterType = (ParameterizedType) parameters[i]
							.getGenericParameterType();
					Type type = genericParameterType.getActualTypeArguments()[0];
					try {
						args[i] = JSON.parseArray(JSONObject.toJSONString(args[i]), new Type[] { type });
					} catch (Exception e) {
						try {
							args[i] = JSONObject.parseArray(JSONObject.toJSONString(args[i]));
						} catch (Exception e2) {
							e2.printStackTrace();
							throw new RpcException("convert args error", e2);
						}
					}
				} else {
					args[i] = JSON.parseObject(JSONObject.toJSONString(args[i]), parameterType);
				}
			}
		}
		return args;
	}
}
