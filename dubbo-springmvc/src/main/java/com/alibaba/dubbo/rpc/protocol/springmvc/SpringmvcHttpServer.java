package com.alibaba.dubbo.rpc.protocol.springmvc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethodSelector;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.remoting.http.servlet.BootstrapListener;
import com.alibaba.dubbo.remoting.http.servlet.ServletManager;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;

/**
 * 
 * @author wuyu DATA:2016-2-27
 */

public class SpringmvcHttpServer {

	private DispatcherServlet dispatcher = new DispatcherServlet();
	private HttpBinder httpBinder;
	private HttpServer httpServer;
	private List<String> produce = Arrays.asList("application/json;charset=utf-8"/*, "text/xml;charset=utf-8"*/);
	private List<String> applicationTypes = Arrays.asList("json"/*, "xml"*/);
	private final Map<Object, HashSet<String>> urls = new ConcurrentHashMap<Object, HashSet<String>>();
	private final Map<Object, HashSet<RequestMappingInfo>> mappingds = new ConcurrentHashMap<Object, HashSet<RequestMappingInfo>>();

	public SpringmvcHttpServer(HttpBinder httpBinder) {
		this.httpBinder = httpBinder;
	}

	public void stop() {
		dispatcher.destroy();
		httpServer.close();
	}

	protected void doStart(URL url) {
		httpServer = httpBinder.bind(url, new SpringmvcHandler());

		ServletContext servletContext = ServletManager.getInstance().getServletContext(url.getPort());
		if (servletContext == null) {
			servletContext = ServletManager.getInstance().getServletContext(ServletManager.EXTERNAL_SERVER_PORT);
		}
		if (servletContext == null) {
			throw new RpcException("No servlet context found. If you are using server='servlet', "
					+ "make sure that you've configured " + BootstrapListener.class.getName() + " in web.xml");
		}
		dispatcher.setContextConfigLocation("classpath:dubbo-springmvc.xml");

		try {
			dispatcher.init(new SimpleServletConfig(servletContext));
			// 注册ResponseBodyWrap,免除ResponseBody注解
			registerResponseBodyWrap();
		} catch (ServletException e) {
			throw new RpcException(e);
		}
	}

	protected boolean checkRootPath(String contextPath) {
		return contextPath.replace("/", "").trim().length() > 0;
	}

	protected String getContextPath(URL url) {
		int pos = url.getPath().lastIndexOf("/");
		return pos > 0 ? url.getPath().substring(0, pos) : "";
	}

	public DispatcherServlet getDispacherServlet() {
		return dispatcher;
	}

	private class SpringmvcHandler implements HttpHandler {
		public void handle(HttpServletRequest request, HttpServletResponse response)
				throws IOException, ServletException {
			RpcContext.getContext().setRemoteAddress(request.getRemoteAddr(), request.getRemotePort());
			String requestURI = request.getRequestURI();
			if ("/services".equals(requestURI)) {
				int serverPort = request.getServerPort();
				String hostAddress = NetUtils.getLocalAddress().getHostAddress();
				String addr = request.getScheme() + "://" + hostAddress + ":" + serverPort + "/";
				String genHtml = WebManager.genHtml(urls, addr);
				response.setContentType("text/html;charset=utf-8");
				response.getWriter().write(genHtml);
				response.flushBuffer();
				return;
			}

			dispatcher.service(request, response);
		}
	}

	public void start(URL url) {
		doStart(url);
	}

	@SuppressWarnings("unchecked")
	public void deploy(Class resourceDef, URL url) {

		try {

			// 反射SpringExtensionFactory 拿到所有的ApplicatonContext 通过class类型获取bean
			Set<Object> beans = SpringUtil.getBeans(resourceDef);
			for (Object bean : beans) {
				registerHandler(bean);
				detectHandlerMethods(resourceDef, bean, url);
			}
		} catch (Exception e) {
			throw new RpcException(e);
		}

	}

	public void undeploy(Class resourceDef) {
		Set<Object> beans = SpringUtil.getBeans(resourceDef);
		for (Object bean : beans) {
			try {
				unRegisterHandler(bean);
			} catch (Exception e) {
			}
		}
	}

	public Set<RequestMappingInfo> getRequestMappingInfos(Object handler) throws Exception {
		Set<Method> methods = selectMethods(handler);
		Set<RequestMappingInfo> requestMappingInfos = new HashSet<RequestMappingInfo>();
		for (Method method : methods) {
			RequestMappingInfo info = null;
			RequestMapping methodAnnotation = AnnotationUtils.findAnnotation(method, RequestMapping.class);
			if (methodAnnotation != null) {
				info = createRequestMappingInfo(methodAnnotation);
				RequestMapping typeAnnotation = AnnotationUtils.findAnnotation(handler.getClass(),
						RequestMapping.class);
				if (typeAnnotation != null) {
					info = createRequestMappingInfo(typeAnnotation).combine(info);
					requestMappingInfos.add(info);
				}
			}
		}
		return requestMappingInfos;
	}

	public Set<String> getUrlPathsByHandler(Object handler) throws Exception {
		Set<RequestMappingInfo> requestMappingInfos = getRequestMappingInfos(handler);
		Set<String> paths = new HashSet<String>();
		for (RequestMappingInfo requestMappingInfo : requestMappingInfos) {
			Map<String, Object> urlMap = getRequestMappingUrlMap();
			Map<RequestMappingInfo, Object> requestMethodHandler = getRequestMethodHandlerMap();
			Set<String> patterns = requestMappingInfo.getPatternsCondition().getPatterns();
			for (String pattern : patterns) {
				Object object = urlMap.get(pattern);
				if (object != null) {
					for (String patthern : patterns) {
						paths.add(patthern);
					}
				}
			}
		}
		return paths;
	}

	public RequestMappingHandlerMapping getRequestMapping(DispatcherServlet dispatcherServlet) {
		return dispatcherServlet.getWebApplicationContext().getBean(RequestMappingHandlerMapping.class);
	}

	public void unRegisterHandler(Object handler) throws Exception {
		removeRequestUrl(handler);
		removeRequestMappingInfo(handler);
	}

	public Map<String, Object> getRequestMappingUrlMap() {
		RequestMappingHandlerMapping requestMapping = getRequestMapping(dispatcher);
		Field urlMapFiled = ReflectionUtils.findField(RequestMappingHandlerMapping.class, "urlMap");
		urlMapFiled.setAccessible(true);
		Map<String, Object> urlMap = (Map<String, Object>) ReflectionUtils.getField(urlMapFiled, requestMapping);
		return urlMap;
	}

	public Map<RequestMappingInfo, Object> getRequestMethodHandlerMap() {
		RequestMappingHandlerMapping requestMapping = getRequestMapping(dispatcher);
		Field handlerMethodsFiled = ReflectionUtils.findField(RequestMappingHandlerMapping.class, "handlerMethods");
		handlerMethodsFiled.setAccessible(true);
		return (Map<RequestMappingInfo, Object>) ReflectionUtils.getField(handlerMethodsFiled, requestMapping);
	}

	public void registerHandler(Object handler) throws Exception {
		RequestMappingHandlerMapping requestMapping = getRequestMapping(dispatcher);
		Method registerHandler = ReflectionUtils.findMethod(RequestMappingHandlerMapping.class, "detectHandlerMethods",
				Object.class);
		registerHandler.setAccessible(true);
		registerHandler.invoke(requestMapping, handler);
	}

	protected void detectHandlerMethods(Object type, final Object handler, URL url) throws Exception {
		Set<Method> methods = selectMethods(handler);
		String path = "%s%s/%s/%s/%s/%s";
		String serviceName = firstLow(((Class) type).getSimpleName());
		String version = url.getParameter("version", "0.0.0");
		String group = url.getParameter("group", "defaultGroup");
		String contextPath = getContextPath(url).equals("") ? "" : getContextPath(url) + "/";

		HashSet<String> paths = new HashSet<String>();
		for (Method method : methods) {
			for (int i = 0; i < applicationTypes.size(); i++) {
				String p = String.format(path, contextPath, group, version, applicationTypes.get(i), serviceName,
						method.getName());
				registerHandlerMethod(handler, method, p, new String[] { produce.get(i) });
				paths.add(p);
			}
		}
		urls.put(handler, paths);
	}

	public Set<Method> selectMethods(Object handler) {
		Class<?> handlerType = handler.getClass();
		final Class<?> userType = ClassUtils.getUserClass(handlerType);
		Set<Method> methods = HandlerMethodSelector.selectMethods(userType, new MethodFilter() {

			@Override
			public boolean matches(Method method) {
				return true;
			}
		});
		return methods;
	}

	public void registerHandlerMethod(Object handler, Method method, String path, String[] produce) throws Exception {
		RequestMapping requestMappingAno = createRequestMappingAno(null, path, produce);
		RequestMappingInfo requestMappingInfo = createRequestMappingInfo(requestMappingAno);
		HashSet<RequestMappingInfo> list = mappingds.get(handler);
		if (list != null) {
			list.add(requestMappingInfo);
		} else {
			list = new HashSet<RequestMappingInfo>();
			list.add(requestMappingInfo);
			mappingds.put(handler, list);
		}
		registerHandlerMethod(handler, method, requestMappingInfo);
	}

	public void registerHandlerMethod(Object handler, Method method, RequestMappingInfo requestMappingInfo)
			throws Exception {
		RequestMappingHandlerMapping requestMappingHandlerMapping = getRequestMapping(dispatcher);
		Method registerHandlerMethod = ReflectionUtils.findMethod(RequestMappingHandlerMapping.class,
				"registerHandlerMethod", Object.class, Method.class, Object.class);
		registerHandlerMethod.setAccessible(true);
		registerHandlerMethod.invoke(requestMappingHandlerMapping, handler, method, requestMappingInfo);
	}

	public RequestMappingInfo createRequestMappingInfo(RequestMapping annotation) throws Exception {
		RequestMappingHandlerMapping requestMapping = getRequestMapping(dispatcher);
		Method registerHandlerMethod = ReflectionUtils.findMethod(RequestMappingHandlerMapping.class,
				"createRequestMappingInfo", RequestMapping.class, RequestCondition.class);
		registerHandlerMethod.setAccessible(true);
		return (RequestMappingInfo) registerHandlerMethod.invoke(requestMapping, annotation, null);
	}

	public RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) throws Exception {
		RequestMappingHandlerMapping requestMappingHandlerMapping = getRequestMapping(dispatcher);
		Method getMappingForMethod = ReflectionUtils.findMethod(RequestMappingHandlerMapping.class,
				"registerHandlerMethod", Method.class, Object.class);
		method.setAccessible(true);
		return (RequestMappingInfo) getMappingForMethod.invoke(requestMappingHandlerMapping, method, handlerType);
	}

	public void removeRequestMappingInfo(Object handler) throws Exception {
		HashSet<RequestMappingInfo> list = mappingds.get(handler);
		Map<RequestMappingInfo, Object> requestMethodHandler = getRequestMethodHandlerMap();
		if (list != null) {
			for (RequestMappingInfo requestMappingInfo : list) {
				requestMethodHandler.remove(requestMappingInfo);
			}
		}
		mappingds.remove(handler);
	}

	public void removeRequestUrl(Object handler) {
		Map<String, Object> requestMappingUrlMap = getRequestMappingUrlMap();
		HashSet<String> paths = urls.get(handler);
		for (String path : paths) {
			requestMappingUrlMap.remove(path);
		}
		urls.remove(handler);
	}

	public void registerResponseBodyWrap() {
		RequestMappingHandlerAdapter adapter = getRequestMappingHandlerAdapter(dispatcher);
		List<HttpMessageConverter<?>> messageConverters = adapter.getMessageConverters();
		ContentNegotiationManager contentNegotiationManager = getContentNegotiationManager();
		List<Object> responseBodyAdvice = getResponseBodyAdvice();
		RequestResponseBodyMethodProcessorWrap responseBody = new RequestResponseBodyMethodProcessorWrap(
				messageConverters, contentNegotiationManager, responseBodyAdvice);
		HandlerMethodReturnValueHandlerComposite handlerComposite = getHandlerMethodReturnValueHandlerComposite();
		List<HandlerMethodReturnValueHandler> returnValueHandlerComposite = getHandlersByHandlerMethodReturnValueHandlerComposite(
				handlerComposite);
		returnValueHandlerComposite.add(0, responseBody);
	}

	public List<HandlerMethodReturnValueHandler> getHandlersByHandlerMethodReturnValueHandlerComposite(
			HandlerMethodReturnValueHandlerComposite composite) {
		Field returnValueHandlers = ReflectionUtils.findField(HandlerMethodReturnValueHandlerComposite.class,
				"returnValueHandlers");
		returnValueHandlers.setAccessible(true);
		return (List<HandlerMethodReturnValueHandler>) ReflectionUtils.getField(returnValueHandlers,
				getHandlerMethodReturnValueHandlerComposite());
	}

	public HandlerMethodReturnValueHandlerComposite getHandlerMethodReturnValueHandlerComposite() {
		RequestMappingHandlerAdapter adapter = getRequestMappingHandlerAdapter(dispatcher);
		Field handlerMethodReturnValueHandlerCompositeField = ReflectionUtils
				.findField(RequestMappingHandlerAdapter.class, "returnValueHandlers");
		handlerMethodReturnValueHandlerCompositeField.setAccessible(true);
		return (HandlerMethodReturnValueHandlerComposite) ReflectionUtils
				.getField(handlerMethodReturnValueHandlerCompositeField, adapter);
	}

	public ContentNegotiationManager getContentNegotiationManager() {
		RequestMappingHandlerAdapter adapter = getRequestMappingHandlerAdapter(dispatcher);
		Field contentNegotiationManagerField = ReflectionUtils.findField(RequestMappingHandlerAdapter.class,
				"contentNegotiationManager");
		contentNegotiationManagerField.setAccessible(true);
		return (ContentNegotiationManager) ReflectionUtils.getField(contentNegotiationManagerField, adapter);
	}

	public List<Object> getResponseBodyAdvice() {
		RequestMappingHandlerAdapter adapter = getRequestMappingHandlerAdapter(dispatcher);
		Field responseBodyAdviceField = ReflectionUtils.findField(RequestMappingHandlerAdapter.class,
				"responseBodyAdvice");
		responseBodyAdviceField.setAccessible(true);
		return (List<Object>) ReflectionUtils.getField(responseBodyAdviceField, adapter);
	}

	public RequestMappingHandlerAdapter getRequestMappingHandlerAdapter(DispatcherServlet dispatcherServlet) {
		return dispatcherServlet.getWebApplicationContext().getBean(RequestMappingHandlerAdapter.class);
	}

	public RequestMapping createRequestMappingAno(final RequestMapping requestMapping, final String requestPath,
			final String[] produce) {
		return new RequestMapping() {

			@Override
			public Class<? extends Annotation> annotationType() {
				return RequestMapping.class;
			}

			@Override
			public String name() {
				return requestMapping != null ? requestMapping.name() : new String();
			}

			@Override
			public String[] value() {
				return new String[] { requestPath };
			}

			@Override
			public RequestMethod[] method() {
				return requestMapping != null ? requestMapping.method() : new RequestMethod[] {};
			}

			@Override
			public String[] params() {
				return requestMapping != null ? requestMapping.params() : new String[] {};
			}

			@Override
			public String[] headers() {
				return requestMapping != null ? requestMapping.headers() : new String[] {};
			}

			@Override
			public String[] consumes() {
				return requestMapping != null ? requestMapping.consumes() : new String[] {};
			}

			@Override
			public String[] produces() {
				return produce != null ? produce
						: (requestMapping != null ? requestMapping.produces() : new String[] {});
			}
		};
	}

	private static class SimpleServletConfig implements ServletConfig {

		private final ServletContext servletContext;

		public SimpleServletConfig(ServletContext servletContext) {
			this.servletContext = servletContext;
		}

		public String getServletName() {
			return "DispatcherServlet";
		}

		public ServletContext getServletContext() {
			return servletContext;
		}

		public String getInitParameter(String s) {
			return null;
		}

		public Enumeration getInitParameterNames() {
			return new Enumeration() {
				public boolean hasMoreElements() {
					return false;
				}

				public Object nextElement() {
					return null;
				}
			};
		}
	}

	public String firstLow(String str) {
		return str.substring(0, 1).toLowerCase() + str.substring(1);
	}

}
