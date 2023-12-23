package com.duberlyguarnizo.bcpcommithelper.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import org.jetbrains.annotations.NotNull;

public class OtherVerificationInspections extends LocalInspectionTool {

  private static final String INIT_MOCKS_METHOD = "initMocks";
  private static final String OPEN_MOCKS_METHOD = "openMocks";

  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder,
                                                 boolean isOnTheFly) {
    return new JavaElementVisitor() {
      @Override
      public void visitMethodCallExpression(@NotNull PsiMethodCallExpression expression) {
        super.visitMethodCallExpression(expression);

        PsiMethod method = expression.resolveMethod();
        if (method != null) {
          PsiClass containingClass = method.getContainingClass();
          if (containingClass != null && isMockitoClass(containingClass)) {
            String methodName = method.getName();
            if (methodName.equals(INIT_MOCKS_METHOD) || methodName.equals(OPEN_MOCKS_METHOD)) {
              holder.registerProblem(expression,
                  "BCP: No se debe usar el metodo " + methodName
                  + "() de Mockito. En su lugar, usa @ExtendWith() en la clase.",
                  ProblemHighlightType.WARNING);
            }
          }
        }
      }

      private boolean isMockitoClass(PsiClass psiClass) {
        return psiClass.getQualifiedName() != null &&
               psiClass.getQualifiedName().startsWith("org.mockito");
      }
    };
  }
}
