package com.annotation.project.entity.common;

import javax.persistence.Embedded;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;

abstract class EntitySupport<E> {
    protected E cloneEntity(E entity) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(bos);
            out.writeObject(entity);
            out.flush();
            out.close();

            ByteArrayInputStream bis = new ByteArrayInputStream(bos.toByteArray());
            ObjectInputStream in = new ObjectInputStream(bis);
            return (E) in.readObject();
        } catch (Exception e) {
            return null;
        }
    }

    protected void rollback(E entity, E prevEntity) {
        try {
            Field[] prevEntityFields = prevEntity.getClass().getDeclaredFields();
            Field[] entityFields = entity.getClass().getDeclaredFields();
            for (Field prevField : prevEntityFields) {
                prevField.setAccessible(true);
                for (Field entityField : entityFields) {
                    entityField.setAccessible(true);
                    if (entityField.getName().equals(prevField.getName())) {
                        Object entityValue = entityField.get(entity);
                        Object prevValue = prevField.get(prevEntity);
                        if (entityField.isAnnotationPresent(Embedded.class)) {
                            rollback((E) entityValue, (E) prevValue);
                        } else if (!entityValue.equals(prevValue)) {
                            entityField.set(entity, prevValue);
                        }
                        break;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }
}
