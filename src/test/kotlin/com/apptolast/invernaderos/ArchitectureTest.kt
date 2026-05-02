package com.apptolast.invernaderos

import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RestController

@AnalyzeClasses(
        packages = ["com.apptolast.invernaderos"],
        importOptions = [ImportOption.DoNotIncludeTests::class]
)
class ArchitectureTest {

    @ArchTest
    val controllersShouldNotAccessRepositoriesDirectly: ArchRule =
            noClasses()
                    .that()
                    .areAnnotatedWith(RestController::class.java)
                    .or()
                    .areAnnotatedWith(Controller::class.java)
                    .should()
                    .dependOnClassesThat()
                    .haveNameMatching(".*Repository")
                    .because(
                            "Controllers should access data via Services, not Repositories directly."
                    )

    @ArchTest
    val servicesShouldBeAnnotatedWithService: ArchRule =
            classes()
                    .that()
                    .haveNameMatching(".*Service")
                    .should()
                    .beAnnotatedWith(Service::class.java)
                    .because("Services should be annotated with @Service.")

    @ArchTest
    val repositoriesShouldBeInterfaces: ArchRule =
            classes()
                    .that()
                    .haveNameMatching(".*Repository")
                    .should()
                    .beInterfaces()
                    .because("Repositories should be interfaces.")

    @ArchTest
    val domainEntitiesShouldNotDependOnControllers: ArchRule =
            noClasses()
                    .that()
                    .areAnnotatedWith(jakarta.persistence.Entity::class.java)
                    .should()
                    .dependOnClassesThat()
                    .areAnnotatedWith(RestController::class.java)
                    .because("Domain entities should not depend on Controllers.")

    // --- Hexagonal Architecture Rules ---

    @ArchTest
    val domainMustNotDependOnSpring: ArchRule =
            noClasses()
                    .that()
                    .resideInAPackage("..features..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.validation..",
                            "org.hibernate.."
                    )
                    .because("Domain layer must be pure Kotlin with zero framework dependencies")

    @ArchTest
    val domainMustNotDependOnInfrastructure: ArchRule =
            noClasses()
                    .that()
                    .resideInAPackage("..features..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..infrastructure..")
                    .because("Domain must not depend on infrastructure (dependency inversion)")

    @ArchTest
    val domainMustNotDependOnDto: ArchRule =
            noClasses()
                    .that()
                    .resideInAPackage("..features..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..dto..")
                    .because("Domain must not depend on DTOs")

    // --- refresh-token specific rule (explicit documentation, redundant with domainMustNotDependOnSpring) ---

    @ArchTest
    val refreshTokenDomainPure: ArchRule =
            noClasses()
                    .that()
                    .resideInAPackage("..features.auth.refresh.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta..",
                            "org.hibernate..",
                            "io.jsonwebtoken.."
                    )
                    .because("refresh-token domain must remain pure Kotlin")
}
