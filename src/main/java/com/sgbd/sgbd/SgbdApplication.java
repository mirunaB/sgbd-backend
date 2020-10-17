package com.sgbd.sgbd;

import com.sgbd.sgbd.model.Record;
import com.sgbd.sgbd.service.CatalogService;
import com.sgbd.sgbd.service.CatalogImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootApplication
public class SgbdApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		SpringApplication.run(SgbdApplication.class, args);
		CatalogService c=new CatalogImpl();
		c.dropTable("miruna","table2");
	}

}
