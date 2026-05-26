package ru.practicum.explorewithme;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import ru.practicum.explorewithme.event.client.EventCategoryClient;

@SpringBootApplication(scanBasePackages = "ru.practicum")
@EnableFeignClients(clients = {EventCategoryClient.class})
public class CategoryService {

    public static void main(String[] args) {
        SpringApplication.run(CategoryService.class, args);
    }
}
