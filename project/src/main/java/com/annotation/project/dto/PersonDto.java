package com.annotation.project.dto;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PersonDto {
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    private PersonInfoDto personInfo;
}
