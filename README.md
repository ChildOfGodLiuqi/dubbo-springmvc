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

//接口
public class UserService{

    //可以不指定produce  默认会自动序列化成json
    @RequestMapping(value="/{id}",method=RequestMethod.GET)
    public User findById(@PathVariable("id") Integer id);
            
    //只接受请求头为application/json
    @RequestMapping(value="/",method=RequestMethod.POST,consumes=MediaType.APPLICATION_JSON_VALUE)
    //只做简单返回
    public User isnert(@RequestBody User user);
            
    //注入request,response
    @RequestMapping(value="/{id}",,method=RequestMethod.DELETE)
    public String delete(@PathVariable("id") Integer id);
    
    //注入request,response
    @RequestMapping(value="/",,method=RequestMethod.PUT,consumes=MediaType.APPLICATION_JSON_VALUE)
    public User update(@RequestBody User user);
 
}
	
//实现类
public class UserServiceImpl{
    public Integer findById(@PathVariable("id") Integer id){
        return id;
    }
            
    public User isnert(@RequestBody User user){
            return user;
    }
            
    public String delete(@PathVariable("id") Integer id){
        return id;
    }
    
    public User update(@RequestBody User user){
        return user;
    }

}
```



###增加了两个http容器 tomcat,jetty9
```
<!-- 如果要使用tomcat server -->
<dubbo:protocol name="springmvc" server="tomcat" port="8080" />

<!-- 如果要使用jetty9 server -->
<dubbo:protocol name="springmvc" server="jetty9" port="8080" />

```
	
	

###依赖jar

```
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

<!-- springmvc 注解解析构建请求 -->
<dependency>
    <groupId>com.netflix.feign</groupId>
    <artifactId>feign-core</artifactId>
    <version>8.16.2</version>
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

```
