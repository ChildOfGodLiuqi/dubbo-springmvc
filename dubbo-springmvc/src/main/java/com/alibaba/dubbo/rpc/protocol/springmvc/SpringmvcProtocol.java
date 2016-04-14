package com.alibaba.dubbo.rpc.protocol.springmvc;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.remoting.http.HttpBinder;
import com.alibaba.dubbo.rpc.RpcException;
import com.alibaba.dubbo.rpc.protocol.AbstractProxyProtocol;
import com.alibaba.dubbo.rpc.protocol.springmvc.entity.RequestEntity;
import com.alibaba.dubbo.rpc.protocol.springmvc.entity.ResponseEntity;
import com.alibaba.dubbo.rpc.protocol.springmvc.message.HessainHttpMessageConverter;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;

/**
 * 
 * @author wuyu DATA:2016-2-10
 */
public class SpringmvcProtocol extends AbstractProxyProtocol {

	private static final int DEFAULT_PORT = 8080;

	private final Map<String, SpringmvcHttpServer> servers = new ConcurrentHashMap<String, SpringmvcHttpServer>();

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
				server.start(url, type);
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
		restTemplate.getMessageConverters().add(0, new HessainHttpMessageConverter());
		restTemplate.getMessageConverters().add(1, new FastJsonHttpMessageConverter());
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
						headers.setContentType(new MediaType("application", "hessain2"));
						HttpEntity httpEntity = new HttpEntity(requestEntity, headers);
						ResponseErrorHandler errorHandler = new DefaultResponseErrorHandler() {

							@Override
							public void handleError(ClientHttpResponse response) throws IOException {
								String copyToString = StreamUtils.copyToString(response.getBody(),
										Charset.forName("utf-8"));
								throw new RpcException(response.getStatusCode().value(), copyToString);
							}

						};
						restTemplate.setErrorHandler(errorHandler);
						ResponseEntity response = restTemplate.postForObject(addr, httpEntity, ResponseEntity.class);
						if (response.getStatus() != 200) {
							throw new RpcException(response.getStatus(), response.getMsg());
						}
						return response.getResult();
					}
				});
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
