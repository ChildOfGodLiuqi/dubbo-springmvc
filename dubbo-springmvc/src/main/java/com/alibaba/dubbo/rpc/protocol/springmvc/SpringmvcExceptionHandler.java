package com.alibaba.dubbo.rpc.protocol.springmvc;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.dubbo.rpc.protocol.springmvc.annotation.ErrorMsg;

/**
 * 
 * @author wuyu DATA:2016-3-1
 */
public class SpringmvcExceptionHandler implements HandlerExceptionResolver {

	public static final String JSON_TYPE = "application/json;charset=utf-8";
	public static final String XML_TYPE = "text/xml;charset=utf-8";

	@Override
	public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler,
			Exception ex) {
		if (handler instanceof HandlerMethod) {
			HandlerMethod methodHandler = (HandlerMethod) handler;
			ErrorMsg errorMsg = methodHandler.getMethodAnnotation(ErrorMsg.class);
			try {
				handlerExcption(response, errorMsg, ex);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public void handlerExcption(HttpServletResponse response, ErrorMsg errorMsg, Exception ex) throws IOException {

		if (errorMsg != null && errorMsg.customerMsgFormat()) {
			handlerCustomerMsg(response, errorMsg, ex);
			return;
		}

		if (errorMsg != null && errorMsg.responseType().contains("xml")) {
			handlerXml(response, errorMsg, ex);
			return;
		}

		handlerJson(response, errorMsg, ex);

	}

	public void handlerCustomerMsg(HttpServletResponse response, ErrorMsg errorMsg, Exception ex) throws IOException {
		writerMsg(response, errorMsg.msg(), errorMsg.responseType());
	}

	public void handlerXml(HttpServletResponse response, ErrorMsg errorMsg, Exception ex) throws IOException {
		String msg = "<server><msg>%s</msg><status>%d</status></server>";
		if (errorMsg != null) {
			String message = String.format(msg, errorMsg.msg(), errorMsg.status());
			writerMsg(response, message, XML_TYPE);
		} else {
			String message = String.format(msg, ex.getMessage(), 500);
			writerMsg(response, message, XML_TYPE);
		}
	}

	public void handlerJson(HttpServletResponse response, ErrorMsg errorMsg, Exception ex) throws IOException {
		String msg = "{\"msg\":\"%s\",\"status\":%d}";
		if (errorMsg != null) {
			String message = String.format(msg, errorMsg.msg().replace("\"", "'"), errorMsg.status());
			writerMsg(response, message, JSON_TYPE);
		} else {
			String message = "";
			if (ex.getMessage() != null) {
				message = String.format(msg, ex.getMessage().replace("\"", "'"), 500);
			} else {
				message = String.format(msg, ex.toString().replace("\"", "'"), 500);
			}

			writerMsg(response, message, JSON_TYPE);
		}
	}

	public void writerMsg(HttpServletResponse response, String msg, String responseType) throws IOException {
		response.setContentType(responseType);
		try {
			response.getWriter().write(msg);
		} catch (java.lang.IllegalStateException e) {
		}
	}

}
