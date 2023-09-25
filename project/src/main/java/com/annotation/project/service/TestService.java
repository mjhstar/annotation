package com.annotation.project.service;

import com.annotation.project.dto.PersonDto;
import com.annotation.project.entity.Person;
import com.annotation.project.entity.PersonInfo;
import com.annotation.project.repository.NameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TestService {
    private final NameRepository nameRepository;

    @Transactional
    public void create(String n) {
        Person person = new Person(n, LocalDateTime.now(), new PersonInfo("서울시", 20, "010-0000-0000"));
        nameRepository.save(person);
    }

    @Transactional
    public void test(long id, String modifyName) {
        Optional<Person> nameOptional = nameRepository.findById(id);
        if (!nameOptional.isPresent()) {
            return;
        }
        Person person = nameOptional.get();
        PersonDto dto = person.getDto(new PersonDto());

        //비즈니스
        dto.setName(modifyName);
        //비즈니스 끝

        try {
            person.update(dto);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        nameRepository.save(person);
    }
}
