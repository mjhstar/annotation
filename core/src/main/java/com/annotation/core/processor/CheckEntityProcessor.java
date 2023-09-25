package com.annotation.core.processor;

import com.annotation.core.annotation.CheckEntity;
import com.annotation.core.model.FieldData;
import com.google.auto.service.AutoService;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.persistence.Embedded;
import javax.tools.Diagnostic;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class CheckEntityProcessor extends AbstractProcessor {

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(CheckEntity.class.getName());
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Elements elementUtil = processingEnv.getElementUtils();
        for (Element element : roundEnv.getElementsAnnotatedWith(CheckEntity.class)) {
            TypeElement typeElement = (TypeElement) element;
            CheckEntity annotation = typeElement.getAnnotation(CheckEntity.class);
            String className = typeElement.getQualifiedName().toString();
            String entityClassName = getClassName(annotation.toString(), "entity");
            if (!className.equals(entityClassName)) {
                printMessage(Diagnostic.Kind.ERROR, element, "Not Match Target Entity - " + className + " ::: " + "Input Entity - " + entityClassName);
            }

            try {
                String dtoClassName = getClassName(annotation.toString(), "dto");
                String[] excludeFields = annotation.excludeFields();
                TypeElement entityClass = elementUtil.getTypeElement(entityClassName);
                TypeElement dtoClass = elementUtil.getTypeElement(dtoClassName);
                checkExcludeFields(excludeFields, entityClass, element);
                if (excludeFields.length > 0)
                    processExclude(entityClass, dtoClass, excludeFields, element);
                else
                    processAllFields(entityClass, dtoClass, element);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }
        return true;
    }

    private void processExclude(TypeElement entity, TypeElement dto, String[] excludeFields, Element element) {
        List<FieldData> entityFields = getFields(entity);
        List<FieldData> dtoFields = getFields(dto);
        List<String> excludeFieldList = Arrays.asList(excludeFields);
        List<FieldData> checkFields = entityFields.stream().filter(en -> !excludeFieldList.contains(en.getName()))
                .collect(Collectors.toList());
        Set<String> checkFieldSet = getFieldSet(checkFields);
        Set<String> dtoFieldSet = getFieldSet(dtoFields);
        checkNameAndType(entity, dto, checkFieldSet, dtoFieldSet, element);
        checkSize(entity, dto, checkFieldSet, dtoFieldSet, element);
    }

    private void processAllFields(TypeElement entity, TypeElement dto, Element element) {
        List<FieldData> entityFields = getFields(entity);
        List<FieldData> dtoFields = getFields(dto);
        Set<String> entityFieldSet = getFieldSet(entityFields);
        Set<String> dtoFieldSet = getFieldSet(dtoFields);
        checkNameAndType(entity, dto, entityFieldSet, dtoFieldSet, element);
        checkSize(entity, dto, entityFieldSet, dtoFieldSet, element);
    }

    private void checkExcludeFields(String[] excludeFields, TypeElement entityClass, Element element) {
        List<FieldData> entityFields = getFields(entityClass);
        List<String> fieldNames = entityFields.stream().map(FieldData::getName).collect(Collectors.toList());
        for (String excludeField : excludeFields) {
            if (!fieldNames.contains(excludeField)) {
                if (excludeField.isEmpty()) {
                    printMessage(Diagnostic.Kind.ERROR, element, "Not Exist Field in Entity : \"\"(emptyString)");
                } else {
                    printMessage(Diagnostic.Kind.ERROR, element, "Not Exist Field in Entity : " + excludeField);
                }
            }
        }
    }

    private List<FieldData> getFields(TypeElement typeElement) {
        List<FieldData> fields = new ArrayList<>();
        for (Element el : typeElement.getEnclosedElements()) {
            if (el instanceof VariableElement) {
                VariableElement variableElement = (VariableElement) el;
                if (variableElement.getAnnotation(Embedded.class) != null) {
                    System.out.println(variableElement.getSimpleName().toString() + " ::: " + variableElement.asType().toString());
                    continue;
                }
                String name = variableElement.getSimpleName().toString();
                String type = variableElement.asType().toString();
                fields.add(new FieldData(name, type));
            }
        }
        return fields;
    }

    private Set<String> getFieldSet(List<FieldData> fields) {
        Set<String> fieldSet = new HashSet<>();
        for (FieldData field : fields) {
            String fieldInfo = field.getName() + " : " + field.getType();
            fieldSet.add(fieldInfo);
        }
        return fieldSet;
    }

    private String getClassName(String input, String key) {
        String regex = key + "=([^,\\)]+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        if (matcher.find()) {
            return matcher.group(1).trim();
        } else {
            return null;
        }
    }

    private void checkSize(TypeElement entity, TypeElement dto, Set<String> entitySet, Set<String> dtoSet, Element element) {
        if (entitySet.size() != dtoSet.size()) {
            printMessage(Diagnostic.Kind.WARNING, element,
                    "Entity Class : " + entity + "  /  Dto Class : " + dto + "   (Check your Entity, Dto Field) ::: " + "entity : {" + entitySet + "} - dto : {" + dtoSet + "}");
        }
    }

    private void checkNameAndType(TypeElement entity, TypeElement dto, Set<String> entitySet, Set<String> dtoSet, Element element) {
        for (String entityField : entitySet) {
            if (!dtoSet.contains(entityField)) {
                printMessage(
                        Diagnostic.Kind.ERROR, element,
                        "Entity Class : " + entity + "  /  Dto Class : " + dto + "   (Not Exist Field in Dto) : {" + entityField + "}");
            }
        }
    }


    private void printMessage(Diagnostic.Kind level, Element element, String message) {
        if (Diagnostic.Kind.WARNING.equals(level)) {
            processingEnv.getMessager().printMessage(level, "\n" + message);
        } else {
            processingEnv.getMessager().printMessage(level, "\n" + message, element);
        }
    }
}