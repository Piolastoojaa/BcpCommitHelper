package com.duberlyguarnizo.bcpcommithelper.inspections;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.XmlElementVisitor;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;

public class UnwantedPomPropertiesInspection extends LocalInspectionTool {
  @Override
  public @NotNull PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder,
                                                 boolean isOnTheFly) {
    return new XmlElementVisitor() {
      @Override
      public void visitXmlFile(@NotNull XmlFile file) {
        if (!"pom.xml".equals(file.getName())) {
          return;
        }

        XmlDocument document = file.getDocument();
        if (document == null) {
          return;
        }

        XmlTag rootTag = document.getRootTag();
        if (rootTag == null || !"project".equals(rootTag.getName())) {
          return;
        }

        findSonarExclusionsTag(rootTag, holder);
        validatePlugins(rootTag, holder);
        validateSnapshotInVersion(rootTag, holder);
      }


    };

  }

  private void findSonarExclusionsTag(XmlTag tag, ProblemsHolder holder) {
    for (XmlTag subTag : tag.getSubTags()) {
      if ("sonar.exclusions".equals(subTag.getName()) || "developers".equals(subTag.getName())) {
        holder.registerProblem(
            subTag,
            "El uso de tags como <" + subTag.getName() + "> no está recomendado en el pom.xml",
            ProblemHighlightType.WARNING
        );
      }
      findSonarExclusionsTag(subTag, holder);
    }
  }

  private void validatePlugins(XmlTag rootTag, ProblemsHolder holder) {
    XmlTag buildTag = rootTag.findFirstSubTag("build");
    if (buildTag == null) {
      return;
    }

    XmlTag pluginsTag = buildTag.findFirstSubTag("plugins");
    if (pluginsTag == null) {
      return;
    }

    for (XmlTag pluginTag : pluginsTag.getSubTags()) {
      XmlTag artifactIdTag = pluginTag.findFirstSubTag("artifactId");
      if (artifactIdTag != null) {
        String artifactId = artifactIdTag.getValue().getText();
        if ("jacoco-maven-plugin".equals(artifactId) || "spring-boot-maven-plugin".equals(artifactId)) {
          holder.registerProblem(
              pluginTag,
              "El uso del plugin " + artifactId + " no está permitido en el pom.xml",
              ProblemHighlightType.WARNING
          );
        }
      }
    }
  }

  private void validateSnapshotInVersion(XmlTag rootTag, ProblemsHolder holder) {
    XmlTag versionTag = rootTag.findFirstSubTag("version");
    if (versionTag == null) {
      holder.registerProblem(
          rootTag,
          "No se encontró la etiqueta <version> en el archivo pom.xml",
          ProblemHighlightType.WARNING
      );
      return;
    }

    String version = versionTag.getValue().getText();
    if (!version.contains("SNAPSHOT")) {
      holder.registerProblem(
          versionTag,
          "La versión debe contener la palabra 'SNAPSHOT' (por ejemplo, 1.0.0-SNAPSHOT)",
          ProblemHighlightType.WARNING
      );
    }
  }
}
