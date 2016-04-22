package com.alibaba.dubbo.rpc.protocol.springmvc.web;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.springframework.context.ApplicationContext;
import org.springframework.core.MethodParameter;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils.MethodFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.method.HandlerMethodSelector;

import com.alibaba.dubbo.common.utils.NetUtils;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.protocol.springmvc.entity.ResponseEntity;
import com.alibaba.dubbo.rpc.protocol.springmvc.util.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class WebManager {

	private static String profix = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><script src=\"http://cdn.bootcss.com/jquery/1.11.3/jquery.min.js\"></script><link rel = \"stylesheet\" href=\"http://cdn.bootcss.com/bootstrap/3.3.5/css/bootstrap.min.css\"><script src=\"http://cdn.bootcss.com/bootstrap/2.3.1/js/bootstrap-transition.js\"></script><script src=\"http://cdn.bootcss.com/bootstrap/2.3.1/js/bootstrap-modal.js\"></script>%s<title>Dubbo-springmvc Manager</title></head><body><div class=\"\"><table class=\"table table-striped  table-hover\"><thead><tr><th>服务名</th><th>方法</th><th>url</th><th>操作</th></tr>";
	private static String suffix = "<tbody>%s</tbody></table></div>%s</body></html>";
	private static String template = "<tr class=\"%s\"><td>%s</td><td>%s</td></td><td><a href=\"%s\">%s</td><td><button onclick='invokerPop(\"%s\")' class=\"btn btn-default btn-primary btn-sm\">调用</button></td></tr>";
	private static List<String> cssTrClass = Arrays.asList("", "info");

	private boolean enableWebManager = true;

	private Map<Object, HashSet<String>> handlerMappings = new ConcurrentHashMap<Object, HashSet<String>>();
	private Map<String, HandlerMethod> handlerMethods = new ConcurrentHashMap<String, HandlerMethod>();

	@RequestMapping("/services")
	public void services(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String serverName = request.getServerName();
		int port = request.getServerPort();
		String contextPath = request.getContextPath();
		String addr = request.getScheme() + "://" + serverName + ":" + port;
		if (!StringUtils.isBlank(contextPath)) {
			addr += contextPath;
		}
		String genHtml = WebManager.genHtml(handlerMappings, addr);
		response.setContentType("text/html;charset=utf-8");
		response.getWriter().write(genHtml);
		response.flushBuffer();
	}

	@RequestMapping(value = { "/api" }, method = { RequestMethod.GET })
	@ResponseBody
	public Object api(HttpServletRequest request) {
		JSONObject json = new JSONObject();
		String serverName = request.getServerName();
		int port = request.getServerPort();
		String contextPath = request.getContextPath();
		String addr = request.getScheme() + "://" + serverName + ":" + port;
		if (!StringUtils.isBlank(contextPath)) {
			addr += contextPath;
		}

		for (String path : handlerMethods.keySet()) {
			HandlerMethod handlerMethod = handlerMethods.get(path);
			Class<?> userClass = ClassUtils.getUserClass(handlerMethod.getBean());
			JSONObject jsonObject = new JSONObject();
			Class<?>[] serviceInterfaces = userClass.getInterfaces();
			JSONArray array = null;

			if (serviceInterfaces.length == 1) {
				String interfaceName = serviceInterfaces[0].getName();
				if (json.getJSONArray(interfaceName) != null) {
					array = json.getJSONArray(interfaceName);
				} else {
					array = new JSONArray();
				}
				json.put(interfaceName, array);
			} else {
				if (json.getJSONArray(userClass.getName()) != null) {
					array = json.getJSONArray(userClass.getName());
				} else {
					array = new JSONArray();
				}
				json.put(userClass.getName(), array);
			}

			jsonObject.put("service", userClass.getName());
			jsonObject.put("method", handlerMethod.getMethod().getName());
			jsonObject.put("url", addr + path);
			MethodParameter[] methodParameters = handlerMethod.getMethodParameters();
			JSONArray args = new JSONArray();
			for (MethodParameter methodParameter : methodParameters) {
				args.add(methodParameter.getParameterType());
			}

			jsonObject.put("argsType", args);

			String[] split = path.split("/");
			if (split.length > 4) {
				jsonObject.put("group", split[1]);
				jsonObject.put("version", split[2]);
				jsonObject.put("serviceThin", split[3]);
			}
			array.add(jsonObject);

			jsonObject.put("returnType", handlerMethod.getMethod().getReturnType());

		}
		return new ResponseEntity().setMsg("welcome to dubbo-springmvc rest api!").setStatus(200).setResult(json);
	}

	public WebManager() {
		super();
		// TODO Auto-generated constructor stub
	}

	@RequestMapping("/beans")
	@ResponseBody
	public JSONObject beans() {
		JSONObject jsonObject = new JSONObject();
		Set<ApplicationContext> applicationContexts = SpringUtil.getApplicationContexts();
		for (ApplicationContext applicationContext : applicationContexts) {
			String[] beanNames = applicationContext.getBeanDefinitionNames();
			for (String beanName : beanNames) {
				Object bean = applicationContext.getBean(beanName);
				Set<Method> selectMethods = HandlerMethodSelector.selectMethods(bean.getClass(), new MethodFilter() {

					public boolean matches(Method method) {
						return true;
					}
				});

				List<String> methods = new ArrayList<String>();
				for (Method method : selectMethods) {
					methods.add(method.toString());
				}
				jsonObject.put(ClassUtils.getUserClass(bean).getName(), methods);
			}
		}
		return jsonObject;
	}

	@RequestMapping("/dataSource")
	@ResponseBody
	public Object dataSource() throws SQLException {
		Set<String> dataSourceNames = SpringUtil.getBeanNamesForType(DataSource.class);
		JSONObject jsonObject = new JSONObject();
		JSONObject dataSourceInfo = new JSONObject();
		for (String dataSourceName : dataSourceNames) {
			JSONObject dataSourceJson = new JSONObject();
			DataSource dataSource = SpringUtil.getBean(dataSourceName);

			Connection conn = dataSource.getConnection();
			if (conn == null) {
				continue;
			}
			DatabaseMetaData metaData = conn.getMetaData();
			String username = metaData.getUserName();
			String url = metaData.getURL();
			String driverName = metaData.getDriverVersion();
			dataSourceInfo.put("url", url);
			dataSourceInfo.put("username", username);
			dataSourceInfo.put("driverName", driverName);
			ResultSet resultSet = metaData.getTables(null, null, null, null);
			JSONObject tables = new JSONObject();
			while (resultSet.next()) {
				String tableName = resultSet.getString("TABLE_NAME");
				ResultSet colRet = metaData.getColumns(null, null, tableName, null);
				JSONObject columnJson = new JSONObject();
				tables.put(tableName, columnJson);
				while (colRet.next()) {
					JSONObject columnInfo = new JSONObject();
					String columnName = colRet.getString("COLUMN_NAME");
					// 字段类型
					String columnType = colRet.getString("TYPE_NAME");
					// 字段长度
					int datasize = colRet.getInt("COLUMN_SIZE");
					// 注释
					String remarks = colRet.getString("REMARKS");
					// DECIMAL精度
					int digits = colRet.getInt("DECIMAL_DIGITS");
					columnInfo.put("columnType", columnType);
					columnInfo.put("columnSize", datasize);
					columnInfo.put("digits", digits);
					columnInfo.put("remarks", remarks);
					columnJson.put(columnName, columnInfo);
				}
			}
			conn.close();
			dataSourceJson.put("tables", tables);
			dataSourceJson.put("dataSourceInfo", dataSourceInfo);
			jsonObject.put(dataSourceName, dataSourceJson);

			Field[] fields = dataSource.getClass().getDeclaredFields();
			for (Field field : fields) {
				try {
					Class<?> type = field.getType();
					if (isNumber(type)) {

						field.setAccessible(true);
						dataSourceInfo.put(field.getName(), field.get(dataSource));
					}
				} catch (Exception e) {
				}
			}

			Method[] methods = dataSource.getClass().getMethods();
			for (Method method : methods) {
				try {
					String methodName = method.getName();
					if (methodName.equals("getMaxActive")) {
						dataSourceInfo.put("maxActive", method.invoke(dataSource));
					}
					if (methodName.equals("getMaxIdle")) {
						dataSourceInfo.put("maxIdle", method.invoke(dataSource));
					}

					if (methodName.equals("getMinIdle")) {
						dataSourceInfo.put("minIdle", method.invoke(dataSource));
					}

					if (methodName.equals("getMaxWait")) {
						dataSourceInfo.put("maxWait", method.invoke(dataSource));
					}

					if (methodName.equals("getQueryTimeout")) {
						dataSourceInfo.put("queryTimeout", method.invoke(dataSource));
					}
					if (methodName.equals("getExecuteCount")) {
						dataSourceInfo.put("executeCount", method.invoke(dataSource));
					}
					if (methodName.equals("getInitialSize")) {
						dataSourceInfo.put("initialSize", method.invoke(dataSource));
					}
					if (methodName.equals("getTimeBetweenConnectErrorMillis")) {
						dataSourceInfo.put("timeBetweenConnectErrorMillis", method.invoke(dataSource));
					}
					if (methodName.equals("getTimeBetweenEvictionRunsMillis()")) {
						dataSourceInfo.put("timeBetweenEvictionRunsMillis", method.invoke(dataSource));
					}
					if (methodName.equals("getMinEvictableIdleTimeMillis()")) {
						dataSourceInfo.put("minEvictableIdleTimeMillis", method.invoke(dataSource));
					}

				} catch (Exception e) {
				}

			}

		}
		return jsonObject;

	}

	@RequestMapping("/env")
	@ResponseBody
	public Object env() {
		return System.getProperties();
	}

	@RequestMapping("/mem")
	@ResponseBody
	public JSONObject mem() {
		JSONObject jsonObject = new JSONObject();
		Runtime r = Runtime.getRuntime();
		jsonObject.put("jvmTotal", r.totalMemory());// java总内存
		jsonObject.put("jvmUse", r.totalMemory() - r.freeMemory());// JVM使用内存
		jsonObject.put("jvmFree", r.freeMemory());// JVM剩余内存
		BigDecimal divide = new BigDecimal((r.totalMemory() - r.freeMemory())).divide(new BigDecimal(r.totalMemory()),
				2, 4);
		jsonObject.put("jvmUsage", divide.doubleValue());// JVM使用率
		jsonObject.put("cpu", r.availableProcessors());
		return jsonObject;
	}

	@RequestMapping("/thread")
	@ResponseBody
	public Object thread() {
		JSONArray jsonArray = new JSONArray();
		long[] allThreadIds = ManagementFactory.getThreadMXBean().getAllThreadIds();
		for (long l : allThreadIds) {
			ThreadInfo threadInfo = ManagementFactory.getThreadMXBean().getThreadInfo(l);
			jsonArray.add(JSON.parseObject(JSON.toJSONString(threadInfo)));
		}
		return jsonArray;
	}

	public boolean isNumber(Class clazz) {
		return clazz.isAssignableFrom(Integer.class) || clazz.isAssignableFrom(Long.class)
				|| clazz.getName().equalsIgnoreCase("INT") || clazz.getName().equalsIgnoreCase("LONG");
	}

	public WebManager(Map<Object, HashSet<String>> urls) {
		super();
		this.handlerMappings = urls;
	}

	public static String genHtml(Map<Object, HashSet<String>> mappingds, String addr) {
		String str = "";
		int i = 1;
		for (Object handler : mappingds.keySet()) {
			Class<?> userClass = ClassUtils.getUserClass(handler);
			i++;
			String handlerName = userClass.getSimpleName();
			ArrayList<String> paths = new ArrayList<String>(mappingds.get(handler));
			Collections.sort(paths);
			Method[] methods = userClass.getMethods();
			for (Method method : methods) {
				for (String path : paths) {
					String[] split = path.split("/");
					String methodName = split[split.length - 1];
					if (method.getName().equals(methodName)) {
						String url = addr + path;
						String tr = String.format(template, cssTrClass.get(i % cssTrClass.size()), handlerName,
								method.toString(), url, url, url);
						str += tr;
					}
				}
			}

		}
		return String.format(profix, script) + String.format(suffix, str, invokerPop);
	}

	public void setUrls(Map<Object, HashSet<String>> urls) {
		this.handlerMappings = urls;
	}

	public boolean isEnableWebManager() {
		return enableWebManager;
	}

	public void setEnableWebManager(boolean enableWebManager) {
		this.enableWebManager = enableWebManager;
	}

	private static String invokerPop = "<div class='modal' id='mymodal'><div class='modal-dialog'><div class='modal-content'><div class='modal-header'><button type='button' class='close' data-dismiss='modal'><span aria-hidden='true'>×</span><span class='sr-only'>Close</span></button><h4 class='modal-title'>调用方法,url注意跨域!</h4></div><div class='modal-body'><form role='form'><div class='form-group'><label for='invokerUrl'>地址：</label> <input type='url'class='form-control' id='invokerUrl' value='' placeholder='调用地址'></div><div class='form-group'><label for='invokerMethod'>参数:</label> <input type='text'class='form-control' value='' id='invokerArgs'placeholder='参数以json格式传递:{\"name\":\"wuyu\"},如果没有参数:{}'></div><div class='form-group'><label for='invokerMethod'>次数:</label> <input type='number'class='form-control' value='1' id='invokerCount'placeholder='調用次数'></div><div class='form-group'><label for='invokerMethod'> 请求头:</label></br> <label class='radio-inline'> <input type='radio' name='reqeustHeader' checked='checked' id='inlineRadio1' value='application/x-www-form-urlencoded; charset=UTF-8'> application/x-www-form-urlencoded</label> <label class='radio-inline'> <input type='radio' name='reqeustHeader' id='inlineRadio2' value='application/json;charset=UTF-8'>application/json</div><div class='form-group'><label for='invokerMethod'>result:</label><div id='result'></div></div><div class='modal-footer'><button type='button' class='btn btn-default' data-dismiss='modal'>关闭</button><button type='button' id='execute' onclick='invoker()' class='btn btn-primary'>执行</button></div></form></div></div></div></div>";

	private static String script = "<script>\r\n" + "function invokerPop(url) {\r\n" + "$('#result').html('');\r\n"
			+ "$('#invokerUrl').val(url);\r\n" + "$('#mymodal').modal('toggle');\r\n" + "}\r\n"
			+ "function invoker(){\r\n" + "var invokerUrl=$('#invokerUrl').val().trim();\r\n"
			+ "var invokerArgs=$('#invokerArgs').val().trim();\r\n" + "var data;\r\n"
			+ "var invokerCount=$('#invokerCount').val().trim();\r\n"
			+ "var contentType=$('input:radio[name=\"reqeustHeader\"]:checked').val();\r\n"
			+ "var header={\"Content-Type\":contentType};" + "var headers = {header};\r\n"
			+ "if(contentType=='application/json;charset=UTF-8'){\r\n" + "data=invokerArgs;\r\n" + "}else{\r\n"
			+ "data=JSON.parse(invokerArgs)\r\n" + "}\r\n" + "var result='';\r\n" + "if(invokerCount==1){\r\n"
			+ "var resultJson=requestJson(invokerUrl,data,contentType);\r\n" + "result=JSON.stringify(resultJson);\r\n"
			+ "$('#result').html(result);\r\n" + "}else{\r\n" + "for (var i = 0; i < invokerCount; i++) {\r\n"
			+ "var resultJson=requestJson(invokerUrl,data,headers);\r\n"
			+ "result+=i+1+':'+JSON.stringify(resultJson)+'</br>';\r\n" + "$('#result').html(result);}\r\n" + "}\r\n"
			+ "}\r\n" + "function requestJson(url, data, contentType) {\r\n" + "var result;\r\n" + "$.ajax({\r\n"
			+ "type : 'post',\r\n" + "async : false,\r\n" + "dataType : 'json',\r\n" + "url : url,\r\n"
			+ "data : data,\r\n"
			// + "headers : headers,\r\n"
			+ "beforeSend: function( request ) {\r\n" + "request.setRequestHeader(\"Content-Type\", contentType);\r\n"
			+ "},\r\n" + "success : function(data) {\r\n" + "result = data;\r\n" + "}\r\n"
			+ ",error : function(data) {\r\n" + "result = data;\r\n" + "}\r\n" + "});\r\n" + "return result;\r\n"
			+ "}\r\n" + "</script>";

	public WebManager(Map<Object, HashSet<String>> urls, Map<String, HandlerMethod> handlerMethods) {
		this.handlerMappings = urls;
		this.handlerMethods = handlerMethods;
	}

	public void setHandlerMethods(Map<String, HandlerMethod> handlerMethods) {
		this.handlerMethods = handlerMethods;
	}

}