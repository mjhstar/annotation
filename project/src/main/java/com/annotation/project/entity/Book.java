package com.annotation.project.entity;

import com.annotation.core.annotation.CheckEntity;
import com.annotation.project.dto.BookDto;
import com.annotation.project.entity.common.CommonEntity;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@CheckEntity(entity = Book.class, dto = BookDto.class, excludeFields = {})
public class Book extends CommonEntity<Book, BookDto> {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private LocalDateTime createdAt;

    @Override
    public String getIdName() {
        return "id";
    }

    @Override
    public void update(BookDto dto) {
        this.updateEntity(this, dto, this.getIdName());
    }

    @Override
    public BookDto getDto(BookDto dto) {
        return this.makeDto(this, dto);
    }
}
