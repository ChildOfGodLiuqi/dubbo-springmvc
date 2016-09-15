package com.alibaba.dubbo.rpc.protocol.springmvc.exception;

import com.alibaba.fastjson.support.spring.FastJsonJsonView;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Created by wuyu on 2016/9/15.
 */
public class SpringMvcExceptionHandler implements HandlerExceptionResolver {

    @Override
    public ModelAndView resolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        FastJsonJsonView view = new FastJsonJsonView();
        view.addStaticAttribute("message", ex.toString());
        view.addStaticAttribute("status", response.getStatus());
        return new ModelAndView(view);
    }
}
