package com.duberlyguarnizo.bcpcommithelper.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class AnemicVerifyMethodInspection extends LocalInspectionTool {

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder,
                                                 boolean isOnTheFly) {
    return new JavaElementVisitor() {

      private PsiMethod lastScannedMethod = null;
      private final Map<String, String> whenMethodMap = new HashMap<>();

      @Override
      public void visitMethod(@NotNull PsiMethod method) {
        super.visitMethod(method);
        if (isJUnit5TestMethod(method) && Objects.nonNull(method.getIdentifyingElement())) {
          var methodStatements = method.getBody().getStatements();
          for (PsiStatement statement : methodStatements) {
            String text = statement.getText();
            if (isMockitoWhenMethodCall(statement)) {
              // Handle when() method call
              System.out.println("Mockito when() method call: " + text);
              String[] methodCallAndArguments =
                  statement.getText().substring("when(".length()).split("\\.");
              String key = methodCallAndArguments[1].replace(" ", "");
              key = key.substring(0, key.length() - 1); //strip last parenthesis
              String value = methodCallAndArguments[0];
              whenMethodMap.put(key, value); //TODO: verify the case when there is a verify call before a when() call

            } else if (isMockitoVerifyMethodCall(statement)) {
              // Handle verify() method call
              System.out.println("Mockito verify() method call: " + text);
              String[] methodCallAndArguments =
                  statement.getText().substring("verify(".length()).split(".");
              var key = methodCallAndArguments[1];
              key = key.substring(0, key.length() - 1); //strip last semi-colon
              System.out.println("Current map: " + whenMethodMap);
              System.out.println("Current evaluated key:" + key);
              String value = methodCallAndArguments[0];
              value = value.replace(")", "");
              System.out.println("Current evaluated value:" + value);

              if (whenMethodMap.containsKey(key) && whenMethodMap.get(key).equals(value)) {
                holder.registerProblem(statement.getNavigationElement(),
                    "BCP: este verify() es redundante por estar incluido dentro de un stub",
                    ProblemHighlightType.WARNING);
              }
            } else {
              System.out.println("Not a Mockito call");
            }
          }

        }
      }

      private boolean isMockitoWhenMethodCall(PsiStatement statement) {
        if (statement instanceof PsiExpressionStatement) {
          PsiExpression expression = ((PsiExpressionStatement) statement).getExpression();
          if (expression instanceof PsiMethodCallExpression methodCallExpression) {
            PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();

            // Check if it's a call to Mockito.when()
            return isMockitoMethodCall(methodExpression, "when");
          }
        }
        return false;
      }

      private boolean isMockitoVerifyMethodCall(PsiStatement statement) {
        if (statement instanceof PsiExpressionStatement) {
          PsiExpression expression = ((PsiExpressionStatement) statement).getExpression();
          if (expression instanceof PsiMethodCallExpression methodCallExpression) {
            PsiReferenceExpression methodExpression = methodCallExpression.getMethodExpression();

            // Check if it's a call to Mockito.verify()
            return isMockitoMethodCall(methodExpression, "verify");
          }
        }
        return false;
      }

      private boolean isMockitoMethodCall(PsiReferenceExpression methodExpression,
                                          String methodName) {
        PsiExpression qualifierExpression = methodExpression.getQualifierExpression();
        if (qualifierExpression instanceof PsiMethodCallExpression) {
          PsiReferenceExpression qualifier =
              ((PsiMethodCallExpression) qualifierExpression).getMethodExpression();
          return methodName.equals(qualifier.getReferenceName());
        }
        return false;
      }


      private boolean isJUnit5TestMethod(PsiMethod method) {
        PsiAnnotation testAnnotation = method.getAnnotation("org.junit.jupiter.api.Test");
        PsiAnnotation parameterizedTestAnnotation = method.getAnnotation(
            "org.junit.jupiter.params.ParameterizedTest");
        return testAnnotation != null || parameterizedTestAnnotation != null;
      }
    };
  }
}