package com.alibaba.dubbo.rpc.protocol.springmvc.web;

import java.io.InputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 用于加载webjar
 * 
 * @author wuyu
 *
 */
public class WebJarsController {

	String mvcPrefix = "/META-INF/resources/webjars/";

	@RequestMapping("/webjars/{webjar}/**")
	public void loadWebJar(@PathVariable("webjar") String webjar, HttpServletResponse response,
			HttpServletRequest request) {
		try {
			ServletContext servletContext = request.getServletContext();
			ClassPathResource classPathResource = new ClassPathResource(mvcPrefix + webjar);
			InputStream in = classPathResource.getInputStream();
			MediaType mediaType = getMediaType(servletContext, classPathResource);
			ServletOutputStream out = response.getOutputStream();
			response.setContentType(mediaType.getType());
			StreamUtils.copy(in, out);
			response.setStatus(200);
		} catch (Exception e) {
			response.setStatus(404);
		}

	}

	protected MediaType getMediaType(ServletContext servletContext, Resource resource) {
		MediaType mediaType = null;
		String mimeType = servletContext.getMimeType(resource.getFilename());
		if (StringUtils.hasText(mimeType)) {
			mediaType = MediaType.parseMediaType(mimeType);
		}
		return mediaType;
	}

}
