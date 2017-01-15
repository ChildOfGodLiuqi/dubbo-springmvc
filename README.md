#基于dubbo 扩展的springmvc插件
###安装

````

mvn install -Dmaven.test.skip=true

<!-- dubbo-springmvc插件 -->
<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dubbo-rpc-springmvc</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>


```

###example
```
git: https://github.com/wu191287278/dubbo-springmvc-example
//接口
@RequestMapping("/user")
public interface UserService {

	// 可以不指定produce 默认会自动序列化成json
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public User findById(@PathVariable("id") Integer id);

	// 只接受请求头为application/json
	@RequestMapping(value = "/", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
	// 只做简单返回
	public User insert(@RequestBody User user);

	// 注入request,response
	@RequestMapping(value = "/{id}", method = RequestMethod.DELETE)
	public Integer delete(@PathVariable("id") Integer id);

	// 注入request,response
	@RequestMapping(value = "/", method = RequestMethod.PUT, consumes = MediaType.APPLICATION_JSON_VALUE)
	public User update(@RequestBody User user);


}
	
//实现类
@RestController
public class UserServiceImpl implements UserService {
	public User findById(@PathVariable("id") Integer id) {
		return new User()
				.setId(id);
	}

	public User insert(@RequestBody User user) {
		return user;
	}

	public Integer delete(@PathVariable("id") Integer id) {
		return id;
	}

	public User update(@RequestBody User user) {
		return user;
	}

}

```


###exmpale2 消费 普通http api.

```
//接口
public class UserService{

    //可以不指定produce  默认会自动序列化成json
    @RequestMapping(value="/{id}",method=RequestMethod.GET,consumes=MediaType.APPLICATION_JSON_VALUE)
    public User findById(@PathVariable("id") Integer id);
 
}

<dubbo:reference id="userService" interface="com.vcg.UserService" protocol="springmvc" url="springmvc://提供服务的server,可以非dubbo服务端" />

```

---

###熔断
```
@Api(fallback=UserServiceExtendFallback.class)
public interface UserService{

    //可以不指定produce  默认会自动序列化成json
    @RequestMapping(value="/{id}",method=RequestMethod.GET,consumes=MediaType.APPLICATION_JSON_VALUE)
    public User findById(@PathVariable("id") Integer id);
 
}

public class UserServiceExtendFallback implements UserService {

    //fallbakc data
    private User user =new User(1,"xxx");

    public User findById(@PathVariable("id") Integer id){
        return user;
    }
}
```

---

###oauth2
配置文件:
```
1. resources/META-INF/dubbo/oauth2/oauth2.properties
2. OAuth2Property spring bean

以上两种都可以完成配置
```

服务端:

```
<dubbo:protocol name="springmvc" server="tomcat" port="8080"/>

<!--验证权限 临时 使用 token作为所需权限 token="ROLE_USER,ROLE_ADMIN"-->
<bean class="com.alibaba.dubbo.demo.provider.UserServiceImpl" id="userService" />
<dubbo:service interface="com.alibaba.dubbo.demo.UserService" filter="oAuth2Filter" ref="userService" token="ROLE_USER,ROLE_ADMIN" />

```

客户端:
```
<!--加入 权限验证 -->
<!--<dubbo:reference id="demoService" interface="com.alibaba.dubbo.demo.DemoService" filter="oAuth2Filter"/>-->
```

---

###dubbo代理,把dubbo服务转化成rest服务

```
<!--代理 Dubbo,并转化为Rest服务 可通过http方式调用dubbo服务-->
<bean class="com.alibaba.dubbo.rpc.protocol.springmvc.proxy.ProxyServiceImpl" id="proxyService"/>

<!--如果本身是web服务,可以省略这一步.该步骤是为了初始化springmvc容器-->
<dubbo:service interface="com.alibaba.dubbo.rpc.protocol.springmvc.proxy.ProxyService" ref="proxyService" protocol="springmvc"/>
```

调用示例:
```
/**
 * http://localhost:8080/
 * POST,PUT,DELETE
 * 调用示例
 * {
 * "service":"com.alibaba.dubbo.demo.DemoService",
 * "method":"sayHello",
 * "group":"defaultGroup",//可以不写
 * "version":"1.0" ,//可以不写
 * "argsType":["java.lang.String"],
 * "args":["wuyu"]
 * }
 */
```

---



###增加了两个http容器 tomcat,jetty9,b
```
//dubbo下仅支持 tomcat,jetty9,dubbox 下支持 jetty,servlet,jetty9,tomcaat

<!-- 如果要使用tomcat server -->
<dubbo:protocol name="springmvc" server="tomcat" port="8080" />

<!-- 如果要使用jetty9 server -->
<dubbo:protocol name="springmvc" server="jetty9" port="8080" />

<!-- 只注册服务,并不提供服务. 可以与springboot 结合,让springboot提供服务,dubbo负责注册发现服务 -->
<dubbo:protocol name="springmvc" server="none" port="8080" />

```

	
	

###依赖jar

```
<dependency>
    <groupId>org.hibernate</groupId>
    <artifactId>hibernate-validator</artifactId>
    <version>5.2.4.Final</version>
</dependency>

<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>dubbo</artifactId>
    <version>2.5.3</version>
</dependency>

<!-- 建议使用高版本 springmvc, 同样也支持 3.2.x版本 -->
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-webmvc</artifactId>
    <version>4.3.2.RELEASE</version>
    <scope>compile</scope>
</dependency>

<!-- springmvc 注解解析构建请求 -->
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-core</artifactId>
    <version>9.3.1</version>
</dependency>

<!-- hystrix 熔断 -->
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-hystrix</artifactId>
    <version>9.3.1</version>
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


<dependency>
    <groupId>com.alibaba</groupId>
    <artifactId>fastjson</artifactId>
    <version>1.2.7</version>
</dependency>

<dependency>
    <groupId>org.apache.httpcomponents</groupId>
    <artifactId>httpclient</artifactId>
    <version>4.5.2</version>
</dependency>


```