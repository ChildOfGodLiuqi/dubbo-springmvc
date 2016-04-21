package com.alibaba.dubbo.rpc.protocol.springmvc.util;

import java.lang.reflect.Method;

import org.springframework.core.MethodParameter;
import org.springframework.util.ReflectionUtils;

public class MethodParameterUtil {
	
	public static Class getContainingClass(MethodParameter returnType) {
		try {
			Method getContainingClassMethod = ReflectionUtils.findMethod(MethodParameter.class, "getContainingClass");
			if (getContainingClassMethod != null) {
				return (Class) getContainingClassMethod.invoke(returnType);
			}
		} catch (Exception e) {
		}
		return returnType.getDeclaringClass();
	}

}
