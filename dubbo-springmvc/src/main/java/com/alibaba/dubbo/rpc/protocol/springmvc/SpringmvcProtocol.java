package com.alibaba.dubbo.rpc.protocol.springmvc;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.transform.Source;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.converter.xml.SourceHttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.serialize.support.json.JsonObjectInput;
import com.alibaba.dubbo.common.serialize.support.json.JsonObjectOutput;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.remoting.http.servlet.DispatcherServlet;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import com.alibaba.dubbo.rpc.protocol.springmvc.entity.RequestEntity;
import com.alibaba.dubbo.rpc.protocol.springmvc.entity.ResponseEntity;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;

/**
 * 
 * @author wuyu DATA:2016-2-10
 */
public class SpringmvcProtocol extends AbstractProxyProtocol {

	private static final int DEFAULT_PORT = 8080;

	private final Map<String, SpringmvcHttpServer> servers = new ConcurrentHashMap<String, SpringmvcHttpServer>();

	private final Map<String, DispatcherServlet> dispatcherServlets = new ConcurrentHashMap<String, DispatcherServlet>();

	private final SpringmvcServerFactory serverFactory = new SpringmvcServerFactory();

	@Override
	public int getDefaultPort() {
		return DEFAULT_PORT;
	}

	public void setHttpBinder(HttpBinder httpBinder) {
		serverFactory.setHttpBinder(httpBinder);
	}

	@Override
	protected <T> Runnable doExport(T impl, final Class<T> type, URL url) throws RpcException {
		final String addr = url.getIp() + ":" + url.getPort();
		SpringmvcHttpServer server = servers.get(addr);
		if (server == null) {
			if (server == null) {
				server = serverFactory.createServer(url.getParameter(Constants.SERVER_KEY, "jetty9"));
				server.start(url);
				servers.put(addr, server);
			}
		}

		server.deploy(type, url);

		return new Runnable() {
			public void run() {
				servers.get(addr).undeploy(type);
			}
		};
	}

	@SuppressWarnings("unchecked")
	@Override
	// 暂时不支持基于springmvc的消费
	protected <T> T doRefer(Class<T> type, URL url) throws RpcException {
		PoolingHttpClientConnectionManager manager = new PoolingHttpClientConnectionManager();
		CloseableHttpClient httpClient = HttpClientBuilder.create().setConnectionManager(manager).build();
		HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
		final RestTemplate restTemplate = new RestTemplate(factory);
		List<HttpMessageConverter<?>> messageConverters = getHttpMessageConverters();
		restTemplate.setMessageConverters(messageConverters);

		final String addr = "http://" + url.getIp() + ":" + url.getPort() + "/" + getContextPath(url);
		final String group = url.getParameter("group", "defaultGroup");
		final String version = url.getParameter("version", "0.0.0");
		final String service = url.getParameter("interface");
		final String contextPath = getContextPath(url);

		return (T) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[] { type },
				new InvocationHandler() {

					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

						RequestEntity requestEntity = new RequestEntity(group, version, service, method.getName(), args,
								contextPath);
						HttpHeaders headers = new HttpHeaders();
						headers.setContentType(MediaType.valueOf("application/springmvc_fastjson_bytes"));
						ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
						JsonObjectOutput out = new JsonObjectOutput(byteArrayOut, true);
						out.writeObject(requestEntity);
						HttpEntity httpEntity = new HttpEntity(byteArrayOut.toByteArray(), headers);
						ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler() {

							@Override
							public void handleError(ClientHttpResponse response) throws IOException {
								String copyToString = StreamUtils.copyToString(response.getBody(),
										Charset.forName("utf-8"));
								throw new RpcException(response.getStatusCode().value(), copyToString);
							}

						};
						restTemplate.setErrorHandler(errorHandler);
						byte[] bytes = restTemplate.postForObject(addr, httpEntity, byte[].class);
						JsonObjectInput in = new JsonObjectInput(new ByteArrayInputStream(bytes));
						ResponseEntity responseEntity = in.readObject(ResponseEntity.class);
						return responseEntity.getResult();
					}
				});
	}

	public List<HttpMessageConverter<?>> getHttpMessageConverters() {
		List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>(6);
		messageConverters.add(new FastJsonHttpMessageConverter());
		StringHttpMessageConverter stringHttpMessageConverter = new StringHttpMessageConverter();
		stringHttpMessageConverter.setWriteAcceptCharset(false); // see SPR-7316
		messageConverters.add(new ByteArrayHttpMessageConverter());
		messageConverters.add(stringHttpMessageConverter);
		messageConverters.add(new SourceHttpMessageConverter<Source>());
		messageConverters.add(new AllEncompassingFormHttpMessageConverter());
		return messageConverters;
	}

	public String getShortName(String str) {
		return firstLow(str.split("[.]")[str.split("[.]").length - 1]);
	}

	public String firstLow(String str) {
		return str.substring(0, 1).toLowerCase() + str.substring(1);
	}

	protected String getContextPath(URL url) {
		int pos = url.getPath().lastIndexOf("/");
		return pos > 0 ? url.getPath().substring(0, pos) : "";
	}

	protected int getErrorCode(Throwable e) {
		return super.getErrorCode(e);
	}

	public void destroy() {
		servers.clear();
		super.destroy();
	}

}
