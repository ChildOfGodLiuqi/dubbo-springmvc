
package com.alibaba.dubbo.rpc.protocol.springmvc;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

import org.springframework.core.Conventions;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.server.ServletServerHttpRequest;
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

/**
 * 为了省略 @ResponseBody注解
 * 
 * @author wuyu
 *
 */
public class RequestResponseBodyMethodProcessorWrap extends AbstractMessageConverterMethodProcessor {

	private Set<Class> clazzs;

	public RequestResponseBodyMethodProcessorWrap(List<HttpMessageConverter<?>> messageConverters) {
		super(messageConverters);
	}

	public RequestResponseBodyMethodProcessorWrap(List<HttpMessageConverter<?>> messageConverters,
			ContentNegotiationManager contentNegotiationManager) {

		super(messageConverters, contentNegotiationManager);
	}

	public RequestResponseBodyMethodProcessorWrap(List<HttpMessageConverter<?>> messageConverters,
			ContentNegotiationManager contentNegotiationManager, List<Object> responseBodyAdvice) {

		super(messageConverters, contentNegotiationManager, responseBodyAdvice);
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return parameter.hasParameterAnnotation(RequestBody.class);
	}

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		Class<?> containingClass = returnType.getContainingClass();
		return clazzs.contains(containingClass)
				|| (AnnotationUtils.findAnnotation(containingClass, ResponseBody.class) != null
						|| returnType.getMethodAnnotation(ResponseBody.class) != null);
	}

	/**
	 * Throws MethodArgumentNotValidException if validation fails.
	 * 
	 * @throws HttpMessageNotReadableException
	 *             if {@link RequestBody#required()} is {@code true} and there
	 *             is no body content or if there is no suitable converter to
	 *             read the content with.
	 */
	@Override
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {

		Object arg = readWithMessageConverters(webRequest, parameter, parameter.getGenericParameterType());
		String name = Conventions.getVariableNameForParameter(parameter);
		WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name);
		if (arg != null) {
			validateIfApplicable(binder, parameter);
			if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {
				throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());
			}
		}
		mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());
		return arg;
	}

	@Override
	protected <T> Object readWithMessageConverters(NativeWebRequest webRequest, MethodParameter methodParam,
			Type paramType) throws IOException, HttpMediaTypeNotSupportedException {

		HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
		HttpInputMessage inputMessage = new ServletServerHttpRequest(servletRequest);

		InputStream inputStream = inputMessage.getBody();
		if (inputStream == null) {
			return handleEmptyBody(methodParam);
		} else if (inputStream.markSupported()) {
			inputStream.mark(1);
			if (inputStream.read() == -1) {
				return handleEmptyBody(methodParam);
			}
			inputStream.reset();
		} else {
			final PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
			int b = pushbackInputStream.read();
			if (b == -1) {
				return handleEmptyBody(methodParam);
			} else {
				pushbackInputStream.unread(b);
			}
			HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(servletRequest) {
				@Override
				public ServletInputStream getInputStream() throws IOException {
					return new ServletInputStream() {
						@Override
						public int read() throws IOException {
							return pushbackInputStream.read();
						}

						@Override
						public void close() throws IOException {
							super.close();
							pushbackInputStream.close();
						}

						@Override
						public boolean isFinished() {
							return false;
						}

						@Override
						public boolean isReady() {
							return false;
						}

						@Override
						public void setReadListener(ReadListener listener) {

						}
					};
				}
			};
			inputMessage = new ServletServerHttpRequest(wrappedRequest) {
				@Override
				public InputStream getBody() {
					// Form POST should not get here
					return pushbackInputStream;
				}
			};
		}

		return super.readWithMessageConverters(inputMessage, methodParam, paramType);
	}

	private Object handleEmptyBody(MethodParameter param) {
		if (param.getParameterAnnotation(RequestBody.class).required()) {
			throw new HttpMessageNotReadableException("Required request body content is missing: " + param);
		}
		return null;
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest) throws IOException, HttpMediaTypeNotAcceptableException {

		mavContainer.setRequestHandled(true);

		// Try even with null return value. ResponseBodyAdvice could get
		// involved.
		writeWithMessageConverters(returnValue, returnType, webRequest);
	}

	public Set<Class> getClazzs() {
		return clazzs;
	}

	public void setClazzs(Set<Class> clazzs) {
		this.clazzs = clazzs;
	}

}
