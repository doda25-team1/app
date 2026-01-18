package frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import doda2025.group1.VersionUtil;

@SpringBootApplication
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("Using lib-version: {}", VersionUtil.getVersion());
        SpringApplication.run(Main.class, args);
    }

}
