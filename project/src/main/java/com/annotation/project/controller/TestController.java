package com.annotation.project.controller;

import com.annotation.project.service.TestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    @GetMapping("/test")
    public void aa(
            @RequestParam long id,
            @RequestParam String modifyName
    ) {
        testService.test(id, modifyName);
    }
}
