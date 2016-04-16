
package com.alibaba.dubbo.rpc.protocol.springmvc.body;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import org.springframework.core.MethodParameter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

/**
 * 为了省略 @ResponseBody注解
 * 
 * @author wuyu
 *
 */
public class RequestResponseBodyMethodProcessorImpl extends RequestResponseBodyMethodProcessor {

	private Set<Class> clazzs;

	public RequestResponseBodyMethodProcessorImpl(List<HttpMessageConverter<?>> messageConverters) {
		super(messageConverters);
	}

	public RequestResponseBodyMethodProcessorImpl(List<HttpMessageConverter<?>> messageConverters,
			ContentNegotiationManager contentNegotiationManager) {

		super(messageConverters, contentNegotiationManager);
	}

	public RequestResponseBodyMethodProcessorImpl(List<HttpMessageConverter<?>> messageConverters,
			ContentNegotiationManager contentNegotiationManager, List<Object> responseBodyAdvice) {

		super(messageConverters, contentNegotiationManager);
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return super.supportsReturnType(returnType) || clazzs.contains(returnType.getDeclaringClass())
				|| clazzs.contains(getContainingClass(returnType));
	}

	public Class getContainingClass(MethodParameter returnType) {
		try {
			Method getContainingClassMethod = ReflectionUtils.findMethod(MethodParameter.class, "getContainingClass");
			if (getContainingClassMethod != null) {
				return (Class) getContainingClassMethod.invoke(returnType);
			}
		} catch (Exception e) {
		}
		return returnType.getDeclaringClass();
	}

	public Set<Class> getClazzs() {
		return clazzs;
	}

	public void setClazzs(Set<Class> clazzs) {
		this.clazzs = clazzs;
	}

}
