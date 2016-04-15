package com.alibaba.dubbo.rpc.protocol.springmvc.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ErrorMsg {

	String msg() default "";

	int status() default 500;

	boolean customerMsgFormat() default false;

	String responseType() default "application/json;charset=utf-8";

}
