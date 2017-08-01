/*
 * Copyright (c) 2006 Rogério Liesenfeld
 * This file is subject to the terms of the MIT license (see LICENSE.txt).
 */
package mockit.internal.injection.field;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;
import javax.annotation.*;
import javax.persistence.*;
import static java.lang.reflect.Modifier.*;
import static java.util.regex.Pattern.*;

import mockit.internal.injection.*;
import mockit.internal.injection.full.*;
import mockit.internal.reflection.*;
import mockit.internal.util.*;
import static mockit.internal.injection.InjectionPoint.*;
import static mockit.internal.injection.InjectionProvider.NULL;

public final class FieldInjection extends Injector
{
   private static final Pattern TYPE_NAME = compile("class |interface |java\\.lang\\.");

   private boolean requireDIAnnotation;
   @Nonnull private Class<?> targetClass;
   private Field targetField;

   public FieldInjection(
      @Nonnull InjectionState injectionState, @Nonnull TestedClass testedClass, @Nullable FullInjection fullInjection,
      boolean requireDIAnnotation)
   {
      super(testedClass, injectionState, fullInjection);
      this.requireDIAnnotation = requireDIAnnotation;
      targetClass = testedClass.targetClass;
   }

   @Nonnull
   public List<Field> findAllTargetInstanceFieldsInTestedClassHierarchy(@Nonnull Class<?> actualTestedClass)
   {
      requireDIAnnotation = false;

      List<Field> targetFields = new ArrayList<Field>();
      Class<?> classWithFields = actualTestedClass;

      do {
         addEligibleFields(targetFields, classWithFields);
         classWithFields = classWithFields.getSuperclass();
      }
      while (testedClass.isClassFromSameModuleOrSystemAsTestedClass(classWithFields) || isServlet(classWithFields));

      return targetFields;
   }

   private void addEligibleFields(@Nonnull List<Field> targetFields, @Nonnull Class<?> classWithFields)
   {
      Field[] fields = classWithFields.getDeclaredFields();

      for (Field field : fields) {
         if (isEligibleForInjection(field)) {
            targetFields.add(field);
         }
      }
   }

   private boolean isEligibleForInjection(@Nonnull Field field)
   {
      int modifiers = field.getModifiers();

      if (isFinal(modifiers)) {
         return false;
      }

      if (kindOfInjectionPoint(field) != KindOfInjectionPoint.NotAnnotated) {
         requireDIAnnotation = true;
         return true;
      }

      //noinspection SimplifiableIfStatement
      if (PERSISTENCE_UNIT_CLASS != null && field.getType().isAnnotationPresent(Entity.class)) {
         return false;
      }

      return !isStatic(modifiers) && !isVolatile(modifiers);
   }

   public void injectIntoEligibleFields(@Nonnull List<Field> targetFields, @Nonnull Object testedObject)
   {
      targetClass = testedObject.getClass();

      for (Field field : targetFields) {
         targetField = field;

         if (targetFieldWasNotAssignedByConstructor(testedObject)) {
            Object injectableValue = getValueForFieldIfAvailable(targetFields);

            if (injectableValue != null && injectableValue != NULL) {
               injectableValue = wrapInProviderIfNeeded(field.getGenericType(), injectableValue);
               FieldReflection.setFieldValue(field, testedObject, injectableValue);
            }
         }
      }
   }

   private boolean targetFieldWasNotAssignedByConstructor(@Nonnull Object testedObject)
   {
      if (kindOfInjectionPoint(targetField) != KindOfInjectionPoint.NotAnnotated) {
         return true;
      }

      Object fieldValue = FieldReflection.getFieldValue(targetField, testedObject);

      if (fieldValue == null) {
         return true;
      }

      Class<?> fieldType = targetField.getType();

      if (!fieldType.isPrimitive()) {
         return false;
      }

      Object defaultValue = DefaultValues.defaultValueForPrimitiveType(fieldType);

      return fieldValue.equals(defaultValue);
   }

   @Nullable
   private Object getValueForFieldIfAvailable(@Nonnull List<Field> targetFields)
   {
      @Nullable String qualifiedFieldName = getQualifiedName(targetField.getDeclaredAnnotations());
      InjectionProvider injectable = findAvailableInjectableIfAny(targetFields, qualifiedFieldName);

      if (injectable != null) {
         return injectionState.getValueToInject(injectable);
      }

      InjectionProvider fieldToInject = new FieldToInject(targetField);
      Type typeToInject = fieldToInject.getDeclaredType();
      InjectionPoint injectionPoint = new InjectionPoint(typeToInject, fieldToInject.getName(), qualifiedFieldName);
      Object testedValue = injectionState.getTestedValue(testedClass, injectionPoint);

      if (testedValue != null) {
         return testedValue;
      }

      KindOfInjectionPoint kindOfInjectionPoint = kindOfInjectionPoint(targetField);

      if (fullInjection != null) {
         if (requireDIAnnotation && kindOfInjectionPoint == KindOfInjectionPoint.NotAnnotated) {
            Object existingInstance = fullInjection.reuseInstance(testedClass, fieldToInject, qualifiedFieldName);
            return existingInstance;
         }

         Object newInstance = fullInjection.createOrReuseInstance(this, fieldToInject, qualifiedFieldName);

         if (newInstance != null) {
            return newInstance;
         }
      }

      if (kindOfInjectionPoint == KindOfInjectionPoint.WithValue) {
         return getValueFromAnnotation(targetField);
      }

      throwExceptionIfUnableToInjectRequiredTargetField(kindOfInjectionPoint);
      return null;
   }

   @Nullable
   private InjectionProvider findAvailableInjectableIfAny(
      @Nonnull List<Field> targetFields, @Nullable String qualifiedTargetFieldName)
   {
      injectionState.setTypeOfInjectionPoint(targetField.getGenericType());

      if (qualifiedTargetFieldName != null && !qualifiedTargetFieldName.isEmpty()) {
         String injectableName = convertToLegalJavaIdentifierIfNeeded(qualifiedTargetFieldName);
         return injectionState.findInjectableByTypeAndName(injectableName);
      }

      String targetFieldName = targetField.getName();

      return withMultipleTargetFieldsOfSameType(targetFields) ?
         injectionState.findInjectableByTypeAndName(targetFieldName) :
         injectionState.getProviderByTypeAndOptionallyName(targetFieldName);
   }

   private boolean withMultipleTargetFieldsOfSameType(@Nonnull List<Field> targetFields)
   {
      for (Field anotherTargetField : targetFields) {
         if (
            anotherTargetField != targetField &&
            injectionState.isAssignableToInjectionPoint(anotherTargetField.getGenericType())
         ) {
            return true;
         }
      }

      return false;
   }

   private void throwExceptionIfUnableToInjectRequiredTargetField(@Nonnull KindOfInjectionPoint kindOfInjectionPoint)
   {
      if (kindOfInjectionPoint == KindOfInjectionPoint.Required) {
         Type fieldType = targetField.getGenericType();
         String fieldTypeName = fieldType.toString();
         fieldTypeName = TYPE_NAME.matcher(fieldTypeName).replaceAll("");
         String kindOfInjectable = "@Injectable";

         if (fullInjection != null) {
            if (targetField.getType().isInterface()) {
               kindOfInjectable = "@Tested instance of an implementation class";
            }
            else {
               kindOfInjectable = "@Tested object";
            }
         }

         throw new IllegalStateException(
            "Missing " + kindOfInjectable + " for field \"" + fieldTypeName + ' ' + targetField.getName() + "\" in " +
            targetField.getDeclaringClass().getSimpleName());
      }
   }

   @Override
   public void fillOutDependenciesRecursively(@Nonnull Object dependency)
   {
      Class<?> dependencyClass = dependency.getClass();
      boolean previousRequireDIAnnotation = requireDIAnnotation;
      List<Field> targetFields = findAllTargetInstanceFieldsInTestedClassHierarchy(dependencyClass);

      if (!targetFields.isEmpty()) {
         List<InjectionProvider> currentlyConsumedInjectables = injectionState.saveConsumedInjectionProviders();

         injectIntoEligibleFields(targetFields, dependency);

         injectionState.restoreConsumedInjectionProviders(currentlyConsumedInjectables);
      }

      requireDIAnnotation = previousRequireDIAnnotation;
   }

   public boolean isDIAnnotationRequired() { return requireDIAnnotation; }
}
