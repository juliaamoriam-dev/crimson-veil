package com.crimsonveil;

import com.crimsonveil.configuration.DotenvLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CrimsonVeilApplication {
    public static void main(String[] args) {
        DotenvLoader.carregarSeExistir();
        SpringApplication.run(CrimsonVeilApplication.class, args);
    }
}
