package com.annotation.project.entity.common;

import javax.persistence.Embedded;
import java.lang.reflect.Field;

public abstract class CommonEntity<E, D> extends EntitySupport<E> implements CommonObject {
    public abstract String getIdName();

    public abstract void update(D dto) throws Exception;

    public abstract D getDto(D dto) throws Exception;

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
        E prevEntity = this.cloneEntity(entity);
        try {
            Field[] entityFields = entity.getClass().getDeclaredFields();
            Field[] dtoFields = dto.getClass().getDeclaredFields();
            if (id != null) {
                Field entityIdField = findFieldByName(entityFields, id);
                Field dtoIdField = findFieldByName(dtoFields, id);

                entityIdField.setAccessible(true);
                dtoIdField.setAccessible(true);

                if (!entityIdField.get(entity).equals(dtoIdField.get(dto))) {
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

                        if (entityField.isAnnotationPresent(Embedded.class)) {
                            updateEntity((E) entityValue, (D) dtoValue, null);
                        } else if (!entityValue.equals(dtoValue)) {
                            entityField.set(entity, dtoValue);
                        }
                        break;
                    }
                }
            }
            throw new Exception();
        } catch (Exception e) {
            this.rollback(entity, prevEntity);
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
