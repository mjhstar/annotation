package com.annotation.project.repository;


import com.annotation.project.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NameRepository extends JpaRepository<Person, Long> {
}
