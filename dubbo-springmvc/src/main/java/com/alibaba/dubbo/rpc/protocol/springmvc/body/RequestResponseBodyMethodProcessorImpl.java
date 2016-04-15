
package com.alibaba.dubbo.rpc.protocol.springmvc.body;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.SpringVersion;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.util.ReflectionUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.AbstractMessageConverterMethodProcessor;
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
			return (Class) ReflectionUtils.findMethod(MethodParameter.class, "getContainingClass").invoke(returnType);
		} catch (Exception e) {
			e.printStackTrace();
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
