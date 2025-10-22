package run;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@SpringBootApplication
@ComponentScan("startpay")
public class Mock {
	public static void main(String[] args) throws Exception {
 
		SpringApplication.run(Mock.class, args);

	}

}
