package com.alibaba.dubbo.rpc.protocol.springmvc;

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
import org.springframework.web.method.HandlerMethod;

import com.alibaba.dubbo.rpc.protocol.springmvc.entity.RequestEntity;
import com.alibaba.dubbo.rpc.protocol.springmvc.entity.ResponseEntity;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

class SpringmvcHandlerInvoker {

	private Map<String, HandlerMethod> handlerMethods;

	private final String HESSIAN_TYPE = "application/hessain2";

	@RequestMapping(value = { "/" }, consumes = { HESSIAN_TYPE }, produces = { HESSIAN_TYPE })
	public ResponseEntity invoker(@RequestBody RequestEntity requestEntity) throws Exception {
		HandlerMethod handlerMethod = handlerMethods.get(requestEntity.mappingUrl());
		Object result = invokerHandler(handlerMethod, requestEntity.getArgs());
		return new ResponseEntity().setResult(result);
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
					args[i] = JSON.parseArray(JSONObject.toJSONString(args[i]), new Type[] { type });
				} else {
					args[i] = JSON.parseObject(JSONObject.toJSONString(args[i]), parameterType);
				}
			}
		}
		return args;
	}
}
