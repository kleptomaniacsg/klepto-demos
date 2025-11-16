package com.example.demoproject;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RestController;
import com.example.demoproject.service.DataMapper;
import java.util.Map;	

@RestController
@SpringBootApplication
public class DemoApplication {
	private final DataMapper dataMapper;

	public DemoApplication(DataMapper dataMapper){
		this.dataMapper=dataMapper;
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@GetMapping("/hello")
	public Map<String,Object> sayHello() throws Exception {

		 boolean isDryRun = true; //args.length > 0 && "--dry-run".equals(args[0]);
		boolean jsonOutput = false; //args.length > 1 && "--json-output".equals(args[1]);		 
		return dataMapper.dryRun(isDryRun).jsonDryRunOutput(jsonOutput).mapData("/2.mapping-config.yml", "/2.data.json");
		
	}

}
