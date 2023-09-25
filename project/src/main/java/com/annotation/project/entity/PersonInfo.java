package com.annotation.project.entity;

import com.annotation.core.annotation.CheckEntity;
import com.annotation.project.dto.PersonInfoDto;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@CheckEntity(entity = PersonInfo.class, dto = PersonInfoDto.class, excludeFields = {"phone"})
public class PersonInfo {
    private String address;
    private int age;
    private String phone;
}
