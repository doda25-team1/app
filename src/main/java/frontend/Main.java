package frontend;

import java.lang.Runtime.Version;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import doda2025.group1.VersionUtil;

@SpringBootApplication
public class Main {

    public static void main(String[] args) {
        System.out.println("Using lib-version:" + VersionUtil.getVersion());
        SpringApplication.run(Main.class, args);
    }

}