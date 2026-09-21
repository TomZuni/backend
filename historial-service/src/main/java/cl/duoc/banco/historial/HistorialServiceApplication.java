package cl.duoc.banco.historial;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class HistorialServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HistorialServiceApplication.class, args);
    }
}
