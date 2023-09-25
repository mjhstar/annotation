package com.annotation.project.controller;

import com.annotation.project.service.TestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class TestController {
    private final TestService testService;

    @PostMapping("/test")
    public void test(
            @RequestParam String name
    ){
      testService.create(name);
    }

    @PutMapping("/test")
    public void aa(
            @RequestParam long id,
            @RequestParam String modifyName
    ) {
        testService.test(id, modifyName);
    }
}
