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
            String dtoClassName = getClassName(annotation.toString(), "dto");

            if (!className.equals(entityClassName)) {
                printMessage(Diagnostic.Kind.ERROR, element, "Not Match Target Entity - " + className + " ::: " + "Input Entity - " + entityClassName);
            }

            try {
                String[] excludeFields = annotation.excludeFields();
                TypeElement entityClass = elementUtil.getTypeElement(entityClassName);
                TypeElement dtoClass = elementUtil.getTypeElement(dtoClassName);
                List<String> embeddedFields = getEmbeddedFields(entityClass);
                checkExcludeFields(excludeFields, entityClass, element);
                if (excludeFields.length > 0) {
                    processExclude(entityClass, dtoClass, excludeFields, element, embeddedFields);
                }
                else
                    processAllFields(entityClass, dtoClass, element, embeddedFields);
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }
        return true;
    }

    private void processExclude(TypeElement entity, TypeElement dto, String[] excludeFields, Element element, List<String> embeddedFields) {
        List<FieldData> entityFields = getFields(entity, true);
        List<FieldData> dtoFields = getFields(dto, true);
        List<String> excludeFieldList = Arrays.asList(excludeFields);
        List<String> checkEmbeddedFields = embeddedFields.stream().filter(it -> !excludeFieldList.contains(it))
                .collect(Collectors.toList());
        List<FieldData> checkFields = entityFields.stream().filter(en -> !excludeFieldList.contains(en.getName()))
                .collect(Collectors.toList());
        checkEmbeddedFields(checkEmbeddedFields, dtoFields, element);
        checkNameAndType(entity, dto, checkFields, dtoFields, element);
        checkSize(entity, dto, checkFields, dtoFields, element);
    }

    private void processAllFields(TypeElement entity, TypeElement dto, Element element, List<String> embeddedFields) {
        List<FieldData> entityFields = getFields(entity, true);
        List<FieldData> dtoFields = getFields(dto, true);
        checkEmbeddedFields(embeddedFields, dtoFields, element);
        checkNameAndType(entity, dto, entityFields, dtoFields, element);
        checkSize(entity, dto, entityFields, dtoFields, element);
    }

    private void checkExcludeFields(String[] excludeFields, TypeElement entityClass, Element element) {
        List<FieldData> entityFields = getFields(entityClass, false);
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

    private void checkEmbeddedFields(List<String> embeddedFields, List<FieldData> dtoFields, Element element) {
        for (String field : embeddedFields) {
            if (!dtoFields.stream().map(FieldData::getName).collect(Collectors.toList()).contains(field)) {
                System.out.println(field + dtoFields.stream().map(FieldData::getName).collect(Collectors.toList()));
                printMessage(Diagnostic.Kind.ERROR, element,
                        "Not Exist Field in Dto ::: " + field
                );
            }
        }

    }

    private List<FieldData> getFields(TypeElement typeElement, boolean removeEmbedded) {
        List<FieldData> fields = new ArrayList<>();
        for (Element el : typeElement.getEnclosedElements()) {
            if (el instanceof VariableElement) {
                VariableElement variableElement = (VariableElement) el;
                if (removeEmbedded && variableElement.getAnnotation(Embedded.class) != null) {
                    continue;
                }
                String name = variableElement.getSimpleName().toString();
                String type = variableElement.asType().toString();
                fields.add(new FieldData(name, type));
            }
        }
        return fields;
    }

    private List<String> getEmbeddedFields(TypeElement typeElement) {
        List<String> embeddedFields = new ArrayList<>();
        for (Element el : typeElement.getEnclosedElements()) {
            if (el instanceof VariableElement) {
                VariableElement variableElement = (VariableElement) el;
                if (variableElement.getAnnotation(Embedded.class) != null) {
                    embeddedFields.add(variableElement.getSimpleName().toString());
                }
            }
        }
        return embeddedFields;
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

    private void checkSize(TypeElement entity, TypeElement dto, List<FieldData> checkFields, List<FieldData> dtoFields, Element element) {
        if (checkFields.size() != dtoFields.size()) {
            printMessage(Diagnostic.Kind.WARNING, element,
                    "Entity Class : " + entity + "  /  Dto Class : " + dto + "   (Check your Entity, Dto Field) ::: " + "entity : {" + checkFields + "} - dto : {" + dtoFields + "}");
        }
    }

    private void checkNameAndType(TypeElement entity, TypeElement dto, List<FieldData> checkFields, List<FieldData> dtoFields, Element element) {
        for (FieldData entityField : checkFields) {
            boolean isCheck = false;

            for (FieldData dtoField : dtoFields) {
                if (entityField.getName().equals(dtoField.getName()) && entityField.getType().equals(dtoField.getType())) {
                    isCheck = true;
                }
                if (isCheck) break;
            }
            if (!isCheck) {
                printMessage(Diagnostic.Kind.ERROR, element,
                        "Entity Class : " + entity + "  /  Dto Class : " + dto + "   (Not Exist Field in Dto) : {" + entityField + "}"
                );
            }
//            if (!dtoFields.contains(entityField)) {
//                printMessage(
//                        Diagnostic.Kind.ERROR, element,
//                        "Entity Class : " + entity + "  /  Dto Class : " + dto + "   (Not Exist Field in Dto) : {" + entityField + "}");
//            }
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