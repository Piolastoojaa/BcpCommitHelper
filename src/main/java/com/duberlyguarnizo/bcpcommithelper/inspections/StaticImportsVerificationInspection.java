package com.duberlyguarnizo.bcpcommithelper.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;

public class StaticImportsVerificationInspection extends LocalInspectionTool {
  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder,
                                                 boolean isOnTheFly) {
    return new JavaElementVisitor() {
      @Override
      public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);

        PsiMethod method = expression.resolveMethod();
        if (method != null) {
          String qualifiedName = method.getContainingClass().getQualifiedName();
          String canonicalText = expression.getMethodExpression().getCanonicalText();
          if (isJUnit5AssertionMethod(method, qualifiedName) && isNotStaticallyAssertionImported(canonicalText)) {
            holder.registerProblem(expression,
                "BCP: La llamada al metodo de Assertions debe ser estatica");
          }
          if (isMockitoMethod(method, qualifiedName)) {
            if (isNotStaticallyMockitoImported(canonicalText)) {
              holder.registerProblem(expression,
                  "BCP: La llamada al metodo de Mockito debe ser estatica");
            }
            if (isMockitoUnAllowedMethod(expression)) {
              holder.registerProblem(expression,
                  "BCP: Usa una alternativa a any() que especifique el tipo de objeto");
            }
          }

        }
      }
    };
  }

  private boolean isJUnit5AssertionMethod(PsiMethod method, String qualifiedName) {
    // Check if the method belongs to JUnit5 Assertions class
    return method.getContainingClass() != null && "org.junit.jupiter.api.Assertions".equals(qualifiedName);
  }

  private boolean isMockitoMethod(PsiMethod method, String qualifiedName) {
    // Check if the method belongs to JUnit5 Assertions class
    return method.getContainingClass() != null && (
        "org.mockito.Mockito".equals(qualifiedName) ||
        "org.mockito.ArgumentMatchers".equals(qualifiedName)
    );
  }

  private boolean isNotStaticallyAssertionImported(@NotNull String canonicalText) {
    return canonicalText.contains("Assertions.");
  }

  private boolean isNotStaticallyMockitoImported(@NotNull String canonicalText) {
    return canonicalText.contains("Mockito.");
  }


  private boolean isMockitoUnAllowedMethod(PsiMethodCallExpression expression) {
    String unAllowed = "any";
    return expression.getMethodExpression().getReferenceName().equals(unAllowed);
  }
}
