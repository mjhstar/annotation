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
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Elements elementUtil = processingEnv.getElementUtils();
        for (Element element : roundEnvironment.getElementsAnnotatedWith(CheckEntity.class)) {
            _process((TypeElement) element, elementUtil);
        }

        return true;
    }

    private void _process(TypeElement element, Elements elementUtil) {
        CheckEntity checkEntity = element.getAnnotation(CheckEntity.class);
        String targetClassName = element.getQualifiedName().toString();
        String entityClassName = getClassName(checkEntity.toString(), "entity");
        String dtoClassName = getClassName(checkEntity.toString(), "dto");
        String[] excludeFields = checkEntity.excludeFields();
        if (!targetClassName.equals(entityClassName)) {
            printMessage(Diagnostic.Kind.ERROR, element, "Not Match Target Entity - " + targetClassName + " ::: " + "Input Entity - " + entityClassName);
        }

        try {
            TypeElement entityTE = elementUtil.getTypeElement(entityClassName);
            TypeElement dtoTE = elementUtil.getTypeElement(dtoClassName);
            List<FieldData> embeddedFields = getEmbeddedElements(entityTE, excludeFields, elementUtil);
            checkExcludeFields(excludeFields, entityTE, element);

            List<FieldData> entityFields = getFields(entityTE, false);
            List<FieldData> dtoFields = getFields(dtoTE, false);
            checkSize(entityTE, dtoTE, entityFields, dtoFields, element);

            entityFields = getFields(entityTE, true).stream().filter(it -> !Arrays.asList(excludeFields).contains(it.getName())).collect(Collectors.toList());
            entityFields.addAll(embeddedFields);
            checkNameAndType(entityTE, dtoTE, entityFields, dtoFields, element);
        } catch (Exception e) {
            printMessage(Diagnostic.Kind.ERROR, element, e.getMessage());
        }

    }

    private void checkNameAndType(TypeElement entity, TypeElement dto, List<FieldData> entityFields, List<FieldData> dtoFields, Element element) {
        for (FieldData entityField : entityFields) {
            boolean isCheck = false;
            for (FieldData dtoField : dtoFields) {
                if (entityField.getName().equals(dtoField.getName()) && entityField.getType().equals(dtoField.getType())) {
                    isCheck = true;
                    break;
                }
            }
            if (!isCheck) {
                printMessage(Diagnostic.Kind.ERROR, element,
                        "Entity Class : " + entity + "  /  Dto Class : " + dto + "   (Not Exist Field in Dto) : { FieldName : " + entityField.getName() + " }"
                );
            }
        }
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

    private List<FieldData> getEmbeddedElements(TypeElement entityTE, String[] excludeFields, Elements elementUtil) {
        List<FieldData> embeddedFieldData = new ArrayList<>();
        for (Element el : entityTE.getEnclosedElements()) {
            if (el instanceof VariableElement) {
                VariableElement variableElement = (VariableElement) el;
                if (variableElement.getAnnotation(Embedded.class) != null) {
                    FieldData fieldData = getEmbeddedFieldData(
                            variableElement.getSimpleName().toString(),
                            variableElement.asType().toString(),
                            elementUtil
                    );
                    if (Arrays.asList(excludeFields).contains(variableElement.getSimpleName().toString()) || fieldData == null) {
                        continue;
                    }
                    embeddedFieldData.add(fieldData);
                }
            }
        }
        return embeddedFieldData;
    }

    private FieldData getEmbeddedFieldData(String fieldName, String className, Elements elementUtil) {
        TypeElement element = elementUtil.getTypeElement(className);
        CheckEntity checkEntity = element.getAnnotation(CheckEntity.class);
        if (checkEntity == null) {
            printMessage(Diagnostic.Kind.ERROR, element, "Not define @CheckEntity ::: " + className + " " + fieldName);
            return null;
        }
        String dtoClassName = getClassName(checkEntity.toString(), "dto");
        return new FieldData(fieldName, dtoClassName);
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

    private void printMessage(Diagnostic.Kind level, Element element, String message) {
        if (Diagnostic.Kind.WARNING.equals(level)) {
            processingEnv.getMessager().printMessage(level, "\n" + message);
        } else {
            processingEnv.getMessager().printMessage(level, "\n" + message, element);
        }
    }

}
