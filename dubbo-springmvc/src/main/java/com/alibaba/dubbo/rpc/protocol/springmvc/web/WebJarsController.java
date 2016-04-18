package com.alibaba.dubbo.rpc.protocol.springmvc.web;

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.HandlerMapping;

/**
 * 用于加载webjar
 * 
 * @author wuyu
 *
 */
public class WebJarsController {

	String mvcPrefix = "/META-INF/resources";

	@RequestMapping("/webjars/{webjar}/**")
	public void loadWebJar(@PathVariable("webjar") String webjar, HttpServletResponse response,
			HttpServletRequest request) {
		String mvcPath = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		ClassPathResource classPathResource = new ClassPathResource(mvcPrefix+mvcPath);
		write(request, response, classPathResource);
	}


	public void write(HttpServletRequest request, HttpServletResponse response, Resource resource) {
		try {
			ServletContext servletContext = request.getServletContext();
			InputStream in = resource.getInputStream();
			String mediaType = getMediaType(servletContext, resource);
			ServletOutputStream out = response.getOutputStream();
			response.setContentType(mediaType);
			StreamUtils.copy(in, out);
			response.setStatus(200);
		} catch (Exception e) {
			response.setStatus(404);
		}
	}

	protected String getMediaType(ServletContext servletContext, Resource resource) {
		return servletContext.getMimeType(resource.getFilename());
	}

}
