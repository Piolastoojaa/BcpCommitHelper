package com.duberlyguarnizo.bcpcommithelper.inspections;

import com.duberlyguarnizo.bcpcommithelper.util.MessageProvider;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

public class AnemicTestVerificationInspection extends LocalInspectionTool {

  @Override
  @NotNull
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new JavaElementVisitor() {
      private int assertCount = 0;
      private boolean callIsInsideTestMethod = false;

      @Override
      public void visitMethod(@NotNull PsiMethod method) {
        super.visitMethod(method);

        if (isJUnit5TestMethod(method)) {
          callIsInsideTestMethod = true;
          assertCount = 0;
          PsiCodeBlock codeBlock = method.getBody();

          if (codeBlock != null) {
            codeBlock.accept(new JavaRecursiveElementVisitor() {
              @Override
              public void visitMethodCallExpression(PsiMethodCallExpression expression) {
                super.visitMethodCallExpression(expression);
                PsiReferenceExpression referenceExpression = expression.getMethodExpression();
                PsiMethod method = (PsiMethod) referenceExpression.resolve();
                verifyStartsWithAssert(method);
              }
            });
          }

          showErrorIfFewAsserts(method);
        }
        callIsInsideTestMethod = false;
      }

      private void showErrorIfFewAsserts(@NotNull PsiMethod method) {
        if (assertCount < 3) {
          holder.registerProblem(method.getIdentifyingElement(),
              MessageProvider.getMessage("insp_test_possible_anemic"));
        }
      }

      private void verifyStartsWithAssert(PsiMethod method) {
        if (method != null) {
          method.getName();
          if (method.getName().startsWith("assert")) {
            assertCount++;
          }
        }
      }

      @Override
      public void visitElement(PsiElement element) {
        super.visitElement(element);
        // Reset the assertion count outside of test method context
        if (!(element instanceof PsiMethod) && !callIsInsideTestMethod) {
          assertCount = 0;
        }
      }
    };
  }

  private boolean isJUnit5TestMethod(PsiMethod method) {
    PsiAnnotation testAnnotation = method.getAnnotation("org.junit.jupiter.api.Test");
    PsiAnnotation parameterizedTestAnnotation = method.getAnnotation(
        "org.junit.jupiter.params.ParameterizedTest");
    return testAnnotation != null || parameterizedTestAnnotation != null;
  }
}
