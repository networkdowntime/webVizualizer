package net.networkdowntime.webVizualizer.configuration;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@Configuration
@EnableWebMvc
@ComponentScan(basePackages = "net.networkdowntime")
public class WebVizualizerServerConfiguration extends WebMvcConfigurerAdapter {
	
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
	    int cachePeriod = 3600 * 24 * 15;
	    registry.addResourceHandler("/**").addResourceLocations("/").setCachePeriod(cachePeriod);
	    registry.addResourceHandler("/favicon.ico").addResourceLocations("/").setCachePeriod(cachePeriod);
	    registry.addResourceHandler("/robots.txt").addResourceLocations("/").setCachePeriod(cachePeriod);
	}

}