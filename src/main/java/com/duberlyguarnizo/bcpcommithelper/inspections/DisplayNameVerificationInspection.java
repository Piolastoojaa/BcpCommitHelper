package com.duberlyguarnizo.bcpcommithelper.inspections;

import com.duberlyguarnizo.bcpcommithelper.util.MessageProvider;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.JavaElementVisitor;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiMethod;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

public class DisplayNameVerificationInspection extends LocalInspectionTool {
  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder,
                                                 boolean isOnTheFly) {
    return new JavaElementVisitor() {
      @Override
      public void visitMethod(@NotNull PsiMethod method) {
        super.visitMethod(method);
        if (isJUnit5TestMethod(method) && Objects.nonNull(method.getIdentifyingElement())) {

          PsiAnnotation displayNameAnnotation = method.getAnnotation("org.junit.jupiter.api"
                                                                     + ".DisplayName");
          if (displayNameAnnotation == null) {
            holder.registerProblem(method.getIdentifyingElement(),
                MessageProvider.getMessage("insp_display_annotation_missing"),
                ProblemHighlightType.ERROR);
          } else {
            String displayNameValue = getDisplayNameValue(displayNameAnnotation, holder);
            if (!displayNameValue.isEmpty()) {
              String expectedMethodName = generateExpectedMethodName(displayNameValue,
                  displayNameAnnotation, holder);
              var displayUpper = verifyDisplayNameStartsWithUpperCase(displayNameValue,
                  displayNameAnnotation, holder);
              var methodLower = verifyMethodNameStartsWithLowerCase(method, holder);
              if (displayUpper && methodLower) {
                verifyDisplayNameEqualsMethodName(method, expectedMethodName, displayNameAnnotation,
                    holder);
              }
            }
            verifyNameContainsWhen(method, holder);
          }
        }
      }
    };
  }

  private static void verifyDisplayNameEqualsMethodName(@NotNull PsiMethod method,
                                                        String expectedMethodName,
                                                        PsiAnnotation displayNameAnnotation,
                                                        @NotNull ProblemsHolder holder) {
    if (!expectedMethodName.equals(method.getName())) {
      holder.registerProblem(displayNameAnnotation.getNavigationElement(),
          MessageProvider.getMessage("insp_display_does_not_equal_method"),
          ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    }
  }

  private boolean verifyDisplayNameStartsWithUpperCase(String displayNameValue,
                                                       PsiAnnotation displayNameAnnotation,
                                                       @NotNull ProblemsHolder holder) {
    if (!startsWithUppercase(displayNameValue)) {
      holder.registerProblem(displayNameAnnotation.getNavigationElement(),
          MessageProvider.getMessage("insp_display_must_start_uppercase"),
          ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return false;
    }
    return true;
  }

  private boolean verifyMethodNameStartsWithLowerCase(@NotNull PsiMethod method,
                                                      @NotNull ProblemsHolder holder) {
    if (startsWithUppercase(method.getName())) {
      holder.registerProblem(method.getIdentifyingElement(),
          MessageProvider.getMessage("insp_method_must_start_lowercase"),
          ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      return false;
    }
    return true;
  }

  private void verifyNameContainsWhen(@NotNull PsiMethod method, @NotNull ProblemsHolder holder) {
    if (!hasWhenWord(method.getName())) {
      holder.registerProblem(method.getIdentifyingElement(),
          MessageProvider.getMessage("insp_when_word_missing"),
          ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
    }
  }

  private boolean isJUnit5TestMethod(PsiMethod method) {
    PsiAnnotation testAnnotation = method.getAnnotation("org.junit.jupiter.api.Test");
    PsiAnnotation parameterizedTestAnnotation = method.getAnnotation(
        "org.junit.jupiter.params.ParameterizedTest");
    return testAnnotation != null || parameterizedTestAnnotation != null;
  }

  private String generateExpectedMethodName(String displayName,
                                            PsiAnnotation displayNameAnnotation,
                                            ProblemsHolder holder) {
    //replace hyphen
    displayName = displayName.replace("-", " ");
    // split into single words when space or uppercase
    if (displayName.contains("  ")) {
      holder.registerProblem(displayNameAnnotation.getNavigationElement(),
          MessageProvider.getMessage("insp_display_no_multiple_spaces"),
          ProblemHighlightType.WEAK_WARNING);
    }
    var wordsArray = displayName.split("(?=[A-Z\\s])");
    StringBuilder builder = new StringBuilder();

    for (String word : wordsArray) {
      if (word.equals(" ")) {
        continue;
      }
      word = word.toLowerCase().trim();
      String firstUpper = capitalizeFirstLetter(word);
      builder.append(firstUpper);
    }
    String result = builder.toString();
    return unCapitalizeFirstLetter(result);
  }

  @NotNull
  private static String capitalizeFirstLetter(String phrase) {
    if (phrase.length() > 1) {
      return phrase.substring(0, 1).toUpperCase() + phrase.substring(1);
    }
    return phrase.toUpperCase();
  }

  @NotNull
  private static String unCapitalizeFirstLetter(String phrase) {
    if (phrase.length() > 1) {
      return phrase.substring(0, 1).toLowerCase() + phrase.substring(1);
    }
    return phrase.toLowerCase();
  }

  private boolean hasWhenWord(String name) {
    return name.toLowerCase().contains("when");
  }

  private boolean startsWithUppercase(String name) {
    if (name.isEmpty()) {
      return false;
    }
    String firstLetter = name.substring(0, 1);
    return firstLetter.equals(firstLetter.toUpperCase());
  }

  private String getDisplayNameValue(PsiAnnotation displayNameAnnotation,
                                     @NotNull ProblemsHolder holder) {
    PsiAnnotationMemberValue value = displayNameAnnotation.findAttributeValue("value");
    if (value != null && value.getText() != null && value.getText().length() >= 2) {
      //do not count quotation marks
      String result = value.getText().substring(1, value.getText().length() - 1);
      if (result.trim().equals(result)) {
        return result;
      }
      holder.registerProblem(displayNameAnnotation.getNavigationElement(),
          MessageProvider.getMessage("insp_display_no_spaces_start_or_end"),
          ProblemHighlightType.WEAK_WARNING);
    }
    return "";
  }
}
