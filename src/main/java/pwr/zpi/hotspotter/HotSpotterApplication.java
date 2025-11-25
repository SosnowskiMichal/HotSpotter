package pwr.zpi.hotspotter;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@Slf4j
@SpringBootApplication
public class HotSpotterApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        log.info("Application timezone set to: {}", TimeZone.getDefault().getID());
    }

	static void main(String[] args) {
		SpringApplication.run(HotSpotterApplication.class, args);
	}

}
