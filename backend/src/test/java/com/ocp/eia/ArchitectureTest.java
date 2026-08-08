package com.ocp.eia;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

class ArchitectureTest {

    private final JavaClasses classes = new ClassFileImporter().importPackages("com.ocp.eia");

    @Test
    void domainMustNotDependOnInfrastructureOrPresentation() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "..infrastructure..",
                        "..presentation..",
                        "..application.service.."
                );
        rule.check(classes);
    }

    @Test
    void knowledgeDomainMustNotDependOnSpringAi() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..modules.knowledge.domain..")
                .should().dependOnClassesThat().resideInAnyPackage("org.springframework.ai..");
        rule.check(classes);
    }

    @Test
    void maintenanceDomainMustNotDependOnKnowledgeModule() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..modules.maintenance.domain..")
                .should().dependOnClassesThat().resideInAnyPackage("..modules.knowledge..");
        rule.check(classes);
    }

    @Test
    void analyticsApplicationMustNotDependOnPresentation() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..modules.analytics..")
                .should().dependOnClassesThat().resideInAnyPackage("..presentation..");
        rule.check(classes);
    }
}
