package com.annotation.project.entity;

import com.annotation.core.annotation.CheckEntity;
import com.annotation.project.dto.PersonDto;
import com.annotation.project.entity.common.CommonEntity;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@CheckEntity(entity = Person.class, dto = PersonDto.class, excludeFields = {})
public class Person extends CommonEntity<Person, PersonDto> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private LocalDateTime createdAt;
    @Embedded
    private PersonInfo personInfo;

    public Person(String name, LocalDateTime createdAt, PersonInfo personInfo){
        this.name = name;
        this.createdAt = createdAt;
        this.personInfo = personInfo;
    }

    @Override
    public String getIdName() {
        return "id";
    }

    @Override
    public void update(PersonDto dto) {
        this.updateEntity(this, dto, this.getIdName());
    }

    @Override
    public PersonDto getDto(PersonDto dto) {
        return this.makeDto(this, dto);
    }
}
