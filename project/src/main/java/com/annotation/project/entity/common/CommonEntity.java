package com.annotation.project.entity.common;

import javax.persistence.Embedded;
import java.lang.reflect.Field;

public abstract class CommonEntity<E, D> {
    public abstract String getIdName();

    public abstract void update(D dto) throws Exception;

    public abstract D getDto(D dto) throws IllegalAccessException;

    protected D makeDto(E entity, D dto) {
        try {
            Field[] entityFields = entity.getClass().getDeclaredFields();
            Field[] dtoFields = dto.getClass().getDeclaredFields();

            for (Field entityField : entityFields) {
                entityField.setAccessible(true);

                for (Field dtoField : dtoFields) {
                    dtoField.setAccessible(true);

                    if (entityField.getName().equals(dtoField.getName())) {
                        Object value = entityField.get(entity);
                        if (entityField.isAnnotationPresent(Embedded.class)) {
                            Class<?> dtoFieldType = dtoField.getType();
                            Object dtoFieldValue = dtoFieldType.getDeclaredConstructor().newInstance();
                            dtoField.set(dto, dtoFieldValue);
                            makeDto((E) value, (D) dtoFieldValue);
                        } else {
                            dtoField.set(dto, value);
                        }


                        break;
                    }
                }
            }
            return dto;
        } catch (Exception e) {
            return null;
        }
    }

    protected void updateEntity(E entity, D dto, String id) {
        try {
            Field[] entityFields = entity.getClass().getDeclaredFields();
            Field[] dtoFields = dto.getClass().getDeclaredFields();
            if(id != null) {
                Field entityIdField = findFieldByName(entityFields, id);
                Field dtoIdField = findFieldByName(dtoFields, id);

                entityIdField.setAccessible(true);
                dtoIdField.setAccessible(true);

                if (!entityIdField.get(entity).equals(dtoIdField.get(dto))) {
                    //TODO ID 가 다르기 때문에 예외 발생
                    throw new Exception();
                }
            }
            for (Field entityField : entityFields) {
                entityField.setAccessible(true);

                for (Field dtoField : dtoFields) {
                    dtoField.setAccessible(true);

                    if (entityField.getName().equals(dtoField.getName())) {
                        Object entityValue = entityField.get(entity);
                        Object dtoValue = dtoField.get(dto);

                        if(entityField.isAnnotationPresent(Embedded.class)){
                            updateEntity((E) entityValue, (D) dtoValue, null);
                        }
                        else if (!entityValue.equals(dtoValue)) {
                            entityField.set(entity, dtoValue);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            //TODO 에러메시지 출력
        }
    }

    private Field findFieldByName(Field[] fields, String fieldName) {
        for (Field field : fields) {
            if (field.getName().equals(fieldName)) {
                return field;
            }
        }
        return null;
    }
}
