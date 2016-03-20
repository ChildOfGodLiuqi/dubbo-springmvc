#基于dubbo 扩展的springmvc插件,自动发布相关rest接口,零入侵现有代码.

	把spring(duubo)接入到http网关,基于springmvc实现
	1.	自动注册到本地springmvc容器.
	2.	完成参数注入与rest输出(json/xml),开放支持个性化设置
	3.	注册到注册中心与网关服务打通(nginx),建议使用基于nginx的 kong ,后台监控zookeeper,自动配置存活机器url,建立一定的规则进行转发.
	4.	暂不支持消费 ,(客户端从网关加载api列表统一网关调用(需要dubbo客户端完成))
	5.	授权/负载/监控

	
	默认发布地址
		发布规则:http://ip:port:8080/组/service版本/接口名/方法名
		
		默认每个方法暴露地址
		地址:http://localhost:8090/defaultGroup/0.0.0/userService/getById?id=1

	
	同时也支持springmvc的注解,自定义url使用.
	
可使用http://ip:port/services查看发布的url服务.
	
		
#安装
mvn install -Dmaven.test.skip=true

		<!-- dubbo-springmvc插件 -->
		<dependency>
			<groupId>com.alibaba</groupId>
			<artifactId>dubbo-springmvc</artifactId>
			<version>0.0.1-SNAPSHOT</version>
		</dependency>

#example
	增加了两个http容器 tomcat,jetty9
	
	<!-- 如果要使用tomcat server -->
	<dubbo:protocol name="springmvc" server="tomcat" port="8080" />
	
	<!-- 如果要使用jetty9 server -->
	<dubbo:protocol name="springmvc" server="jetty9" port="8080" />
	

#增加异常处理SpringmvcExceptionHandler

	1.自动将异常以json/xml方式响应给浏览器或调用的客户端
	2.可以打上@ErrorMsg注解,自定义要返回异常信息.
		@ErrorMsg(msg = "错误信息",status=500,responseType="application/json;charset=utf-8")
		
#自定义token拦截器
	只需要把jar里的dubbo-springmvc.xml文件拿出来,配置基于springmvc的拦截器即可.
#

#依赖jar
		<dependency>
			<groupId>org.hibernate</groupId>
			<artifactId>hibernate-validator</artifactId>
			<version>4.3.1.Final</version>
		</dependency>
		
		<dependency>
			<groupId>com.alibaba</groupId>
			<artifactId>dubbo</artifactId>
			<version>2.5.3</version>
		</dependency>
		
		<dependency>
			<groupId>org.springframework</groupId>
			<artifactId>spring-webmvc</artifactId>
			<version>4.1.7.RELEASE</version>
			<scope>compile</scope>
		</dependency>
		
		<!-- 如果要使用tomcat server -->
		<dependency>
			<groupId>org.apache.tomcat.embed</groupId>
			<artifactId>tomcat-embed-core</artifactId>
			<version>8.0.11</version>
			<scope>compile</scope>
		</dependency>
		<dependency>
			<groupId>org.apache.tomcat.embed</groupId>
			<artifactId>tomcat-embed-logging-juli</artifactId>
			<version>8.0.11</version>
			<scope>compile</scope>
		</dependency>
		
		<!-- 如果要使用jett9 server -->
		<dependency>
			<groupId>org.eclipse.jetty.aggregate</groupId>
			<artifactId>jetty-all</artifactId>
			<version>9.2.15.v20160210</version>
		</dependency>
		
		<!-- 如果要使用xml  需要在dubbo-springmvc.xml配置相关转换器,具体参考springmvc -->
		<dependency>
			<groupId>com.fasterxml.jackson.core</groupId>
			<artifactId>jackson-core</artifactId>
			<version>2.3.3</version>
		</dependency>
		<dependency>
			<groupId>com.fasterxml.jackson.core</groupId>
			<artifactId>jackson-databind</artifactId>
			<version>2.3.3</version>
		</dependency>