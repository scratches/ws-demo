/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.samples.websocket.config;

import java.util.HashMap;
import java.util.Map;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.websocket.server.WsSci;
import org.springframework.boot.config.EnableAutoConfiguration;
import org.springframework.boot.samples.websocket.client.GreetingService;
import org.springframework.boot.samples.websocket.client.SimpleGreetingService;
import org.springframework.boot.samples.websocket.echo.DefaultEchoService;
import org.springframework.boot.samples.websocket.echo.EchoService;
import org.springframework.boot.samples.websocket.echo.EchoWebSocketHandler;
import org.springframework.boot.samples.websocket.snake.SnakeWebSocketHandler;
import org.springframework.boot.strap.SpringApplication;
import org.springframework.boot.strap.context.condition.ConditionalOnClass;
import org.springframework.boot.strap.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.boot.strap.web.SpringServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.support.DefaultSockJsService;
import org.springframework.web.socket.sockjs.support.SockJsHttpRequestHandler;
import org.springframework.web.socket.support.PerConnectionWebSocketHandler;

@Configuration
@EnableAutoConfiguration
public class ApplicationConfiguration extends SpringServletInitializer {

	@Override
	protected Class<?>[] getConfigClasses() {
		logger.debug("Standard Java for WebSocket not present, JSR-356 endpoints will not be loaded");
		return new Class<?>[] { ApplicationConfiguration.class };
	}

	public static void main(String[] args) {
		SpringApplication.run(new ApplicationConfiguration().getConfigClasses(), args);
	}

	@ConditionalOnClass(Tomcat.class)
	@Configuration
	protected static class WsInitializationConfiguration {
		
		@Bean
		public TomcatEmbeddedServletContainerFactory tomcatEmbeddedServletContainerFactory() {
			return new TomcatEmbeddedServletContainerFactory() {
				@Override
				protected void postProcessContext(Context context) {
					context.addServletContainerInitializer(new WsSci(), null);
				}
			};
		}

	}

	@Bean
	public EchoService echoService() {
		return new DefaultEchoService("Did you say \"%s\"?");
	}

	@Bean
	public GreetingService greetingService() {
		return new SimpleGreetingService();
	}

	@Bean
	public SimpleUrlHandlerMapping handlerMapping() {

		SockJsService sockJsService = new DefaultSockJsService(sockJsTaskScheduler());

		Map<String, Object> urlMap = new HashMap<String, Object>();

		urlMap.put("/echo", new WebSocketHttpRequestHandler(echoWebSocketHandler()));
		urlMap.put("/snake", new WebSocketHttpRequestHandler(snakeWebSocketHandler()));

		urlMap.put("/sockjs/echo/**", new SockJsHttpRequestHandler(sockJsService, echoWebSocketHandler()));
		urlMap.put("/sockjs/snake/**", new SockJsHttpRequestHandler(sockJsService, snakeWebSocketHandler()));

		SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
		handlerMapping.setOrder(-1);
		handlerMapping.setUrlMap(urlMap);

		return handlerMapping;
	}

	@Bean
	public DispatcherServlet dispatcherServlet() {
		DispatcherServlet servlet = new DispatcherServlet();
		servlet.setDispatchOptionsRequest(true);
		return servlet;
	}

	@Bean
	public WebSocketHandler echoWebSocketHandler() {
		return new PerConnectionWebSocketHandler(EchoWebSocketHandler.class);
	}

	@Bean
	public WebSocketHandler snakeWebSocketHandler() {
		return new SnakeWebSocketHandler();
	}

	@Bean
	public ThreadPoolTaskScheduler sockJsTaskScheduler() {
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setThreadNamePrefix("SockJS-");
		return taskScheduler;
	}
}
