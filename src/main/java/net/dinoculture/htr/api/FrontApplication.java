package net.dinoculture.htr.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages="net.dinoculture")
@EnableAutoConfiguration(exclude={DataSourceAutoConfiguration.class})												// 데이터소스 없이 가동.
public class FrontApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(FrontApplication.class, args);
	}
}