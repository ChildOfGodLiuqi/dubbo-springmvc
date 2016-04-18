package com.alibaba.dubbo.rpc.protocol.springmvc;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.HashMap;
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

import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerMethodSelector;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.HttpHandler;
import com.alibaba.dubbo.remoting.http.HttpServer;
import com.alibaba.dubbo.remoting.http.servlet.BootstrapListener;
import com.alibaba.dubbo.remoting.http.servlet.ServletManager;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.springmvc.annotation.Interceptor;
import com.alibaba.dubbo.rpc.protocol.springmvc.body.RequestResponseBodyMethodProcessorImpl;
import com.alibaba.dubbo.rpc.protocol.springmvc.message.HessainHttpMessageConverter;
import com.alibaba.dubbo.rpc.protocol.springmvc.util.SpringUtil;
import com.alibaba.dubbo.rpc.protocol.springmvc.web.SpringmvcHandlerInvoker;
import com.alibaba.dubbo.rpc.protocol.springmvc.web.WebJarsController;
import com.alibaba.dubbo.rpc.protocol.springmvc.web.WebManager;

/**
 * 
 * @author wuyu DATA:2016-2-27
 */

public class SpringmvcHttpServer {

	private DispatcherServlet dispatcher = new DispatcherServlet();
	private HttpBinder httpBinder;
	private HttpServer httpServer;

	private final Map<Object, HashSet<String>> urls = new ConcurrentHashMap<Object, HashSet<String>>();
	private final Map<Object, HashSet<RequestMappingInfo>> mappingds = new ConcurrentHashMap<Object, HashSet<RequestMappingInfo>>();
	private final Map<String, HandlerMethod> handlerMethods = new ConcurrentHashMap<String, HandlerMethod>();
	private final Set<Class> clazzs = new HashSet<Class>();

	private final String JSON_TYPE = "application/json;charset=utf-8";

	public SpringmvcHttpServer(HttpBinder httpBinder) {
		this.httpBinder = httpBinder;
	}

	public void stop() {
		dispatcher.destroy();
		httpServer.close();
	}

	protected void doStart(URL url, Class clazz) {
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
			XmlWebApplicationContext webApplicationContext = (XmlWebApplicationContext) dispatcher
					.getWebApplicationContext();
//			ApplicationContext parent = SpringUtil.getApplicationContext(clazz);
//			webApplicationContext.setParent(parent);
//			webApplicationContext.refresh();
			// 注册ResponseBodyWrap,免除ResponseBody注解
			registerResponseBodyWrap();

			// 注册服务网页管理器,可自定义相关网页管理器
			String[] webManagers = webApplicationContext.getBeanNamesForType(WebManager.class);
			for (String webManagerName : webManagers) {
				WebManager webManager = (WebManager) webApplicationContext.getBean(webManagerName);
				if (webManager.isEnableWebManager()) {
					webManager.setUrls(urls);
					registerHandler(webManager);
				}
			}

			// 注册执行器,可自定义相关执行器
			String[] springmvcHandlerInvokers = webApplicationContext
					.getBeanNamesForType(SpringmvcHandlerInvoker.class);
			for (String springmvcHandlerInvokerName : springmvcHandlerInvokers) {
				SpringmvcHandlerInvoker springmvcHandlerInvoker = (SpringmvcHandlerInvoker) webApplicationContext
						.getBean(springmvcHandlerInvokerName);
				if (springmvcHandlerInvoker.isEnableSpringmvcHandlerInvoker()) {
					springmvcHandlerInvoker.setHandlerMethods(handlerMethods);
					registerHandler(springmvcHandlerInvoker);
				}
			}
			
			//支持webjar
			registerHandler(new WebJarsController());
			
		} catch (Exception e) {
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
			dispatcher.service(request, response);
		}
	}

	public void start(URL url, Class type) {
		doStart(url, type);
	}

	@SuppressWarnings("unchecked")
	public void deploy(Class resourceDef, URL url) {

		try {

			// 反射SpringExtensionFactory 拿到所有的ApplicatonContext 通过class类型获取bean
			Set<Object> beans = SpringUtil.getBeans(resourceDef);
			for (Object bean : beans) {
				// 注册拦截器
				if (bean instanceof HandlerInterceptor || bean instanceof MappedInterceptor) {
					registerInterceptors((HandlerInterceptor) bean);
				} else {
					registerHandler(bean);
					detectHandlerMethods(resourceDef, bean, url);
					clazzs.add(ClassUtils.getUserClass(bean));
				}

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

	public WebApplicationContext createWebApplicationContext(ApplicationContext parent) throws Exception {
		Method createWebApplicationContext = ReflectionUtils.findMethod(DispatcherServlet.class,
				"createWebApplicationContext", ApplicationContext.class);
		createWebApplicationContext.setAccessible(true);
		return (WebApplicationContext) createWebApplicationContext.invoke(dispatcher, parent);
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
				} else {
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
		if (urlMapFiled == null) {
			return new HashMap<String, Object>();
		}
		urlMapFiled.setAccessible(true);
		Map<String, Object> urlMap = (Map<String, Object>) ReflectionUtils.getField(urlMapFiled, requestMapping);
		return urlMap;
	}

	public Map<RequestMappingInfo, HandlerMethod> getRequestMethodHandlerMap() {
		RequestMappingHandlerMapping requestMapping = getRequestMapping(dispatcher);
		return requestMapping.getHandlerMethods();
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
		String path = "%s/%s/%s/%s";
		String serviceName = firstLow(((Class) type).getSimpleName());
		String version = url.getParameter("version", "0.0.0");
		String group = url.getParameter("group", "defaultGroup");

		HashSet<String> paths = new HashSet<String>();
		for (Method method : methods) {
			String p = String.format(path, group, version, serviceName, method.getName());
			p = p.substring(0, 1).equals("/") ? p : "/" + p;

			// 由于springmvc不支持方法重载 ,故过滤相同方法
			if (paths.contains(p)) {
				continue;
			}
			registerHandlerMethod(handler, method, p, new String[] { JSON_TYPE });
			paths.add(p);
		}

		Set<String> custemerPaths = getUrlPathsByHandler(handler);
		paths.addAll(custemerPaths);
		urls.put(handler, paths);
	}

	public Set<Method> selectMethods(Object handler) {
		Class<?> handlerType = handler.getClass();
		final Class<?> userType = ClassUtils.getUserClass(handlerType);
		Set<Method> methods = HandlerMethodSelector.selectMethods(userType, new MethodFilter() {

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
		HandlerMethod handlerMethod = getRequestMethodHandlerMap().get(requestMappingInfo);
		handlerMethods.put(path, handlerMethod);
	}

	public void registerInterceptors(HandlerInterceptor interceptor) throws Exception {
		Interceptor interceptorAnnotation = findInterceptorAnnotation(interceptor);
		if (interceptorAnnotation != null) {

			MappedInterceptor mappedInterceptor = new MappedInterceptor(interceptorAnnotation.includePatterns(),
					interceptorAnnotation.excludePatterns(), interceptor);
			// 兼容3.0旧版本
			Class<?>[] interfaces = MappedInterceptor.class.getInterfaces();
			if (interfaces.length == 0) {
				getMappedInterceptors().add(mappedInterceptor);
			} else {
				getAdaptedInterceptors().add(mappedInterceptor);
			}
		}

	}

	public List getAdaptedInterceptors() throws Exception {
		RequestMappingHandlerMapping requestMappingHandlerMapping = getRequestMapping(dispatcher);
		Field interceptors = ReflectionUtils.findField(RequestMappingHandlerMapping.class, "adaptedInterceptors");
		interceptors.setAccessible(true);
		return (List) interceptors.get(requestMappingHandlerMapping);
	}

	public List getMappedInterceptors() throws Exception {
		RequestMappingHandlerMapping requestMappingHandlerMapping = getRequestMapping(dispatcher);
		Field interceptors = ReflectionUtils.findField(RequestMappingHandlerMapping.class, "mappedInterceptors");
		interceptors.setAccessible(true);
		return (List) interceptors.get(requestMappingHandlerMapping);
	}

	public void registerHandlerMethod(Object handler, Method method, RequestMappingInfo requestMappingInfo)
			throws Exception {
		RequestMappingHandlerMapping requestMappingHandlerMapping = getRequestMapping(dispatcher);
		Method registerHandlerMethod = ReflectionUtils.findMethod(RequestMappingHandlerMapping.class,
				"registerHandlerMethod", Object.class, Method.class, Object.class);
		registerHandlerMethod.setAccessible(true);
		registerHandlerMethod.invoke(requestMappingHandlerMapping, handler, method, requestMappingInfo);
	}

	public Interceptor findInterceptorAnnotation(Object handlerInterceptor) {
		return handlerInterceptor.getClass().getAnnotation(Interceptor.class);
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
		Map<RequestMappingInfo, HandlerMethod> requestMethodHandler = getRequestMethodHandlerMap();
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
			handlerMethods.remove(path);
		}
		urls.remove(handler);
	}

	public void registerResponseBodyWrap() {
		RequestMappingHandlerAdapter adapter = getRequestMappingHandlerAdapter(dispatcher);
		List<HttpMessageConverter<?>> messageConverters = adapter.getMessageConverters();
		messageConverters.add(new HessainHttpMessageConverter());
		ContentNegotiationManager contentNegotiationManager = getContentNegotiationManager();
		List<Object> responseBodyAdvice = getResponseBodyAdvice();
		RequestResponseBodyMethodProcessorImpl responseBody = new RequestResponseBodyMethodProcessorImpl(
				messageConverters, contentNegotiationManager, responseBodyAdvice);
		responseBody.setClazzs(clazzs);
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
		if (responseBodyAdviceField == null) {
			return null;
		}
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

			public String name() {
				return new String();
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

			public String[] path() {
				return new String[] { requestPath };
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
