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

    // --- Alert history + stats hexagonal rules ---

    @ArchTest
    val alertDomainMustNotDependOnSpring: ArchRule =
            noClasses()
                    .that()
                    .resideInAPackage("..features.alert.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.validation..",
                            "org.hibernate.."
                    )
                    .because("Alert domain layer must be pure Kotlin with zero framework dependencies")

    @ArchTest
    val alertDomainMustNotDependOnInfrastructure: ArchRule =
            noClasses()
                    .that()
                    .resideInAPackage("..features.alert.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..features.alert.infrastructure..")
                    .because("Alert domain must not depend on alert infrastructure (dependency inversion)")

    @ArchTest
    val alertDomainMustNotDependOnDto: ArchRule =
            noClasses()
                    .that()
                    .resideInAPackage("..features.alert.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..features.alert.dto..")
                    .because("Alert domain must not depend on DTOs")

    @ArchTest
    val alertUseCaseImplsMustImplementInputPort: ArchRule =
            classes()
                    .that()
                    .resideInAPackage("..features.alert.application.usecase..")
                    .and()
                    .haveNameMatching(".*UseCaseImpl")
                    .should()
                    .implement(
                            com.tngtech.archunit.base.DescribedPredicate.describe(
                                    "an interface residing in domain/port/input package"
                            ) { iface: com.tngtech.archunit.core.domain.JavaClass ->
                                iface.name.contains(".domain.port.input.")
                            }
                    )
                    .because("Every *UseCaseImpl must implement a use case interface from domain/port/input/")

    @ArchTest
    val alertQueryAdaptersMustImplementOutputPort: ArchRule =
            classes()
                    .that()
                    .resideInAPackage("..features.alert.infrastructure.adapter.output..")
                    .and()
                    .haveNameMatching(".*QueryAdapter")
                    .should()
                    .implement(
                            com.tngtech.archunit.base.DescribedPredicate.describe(
                                    "an interface residing in domain/port/output package"
                            ) { iface: com.tngtech.archunit.core.domain.JavaClass ->
                                iface.name.contains(".domain.port.output.")
                            }
                    )
                    .because("Every *QueryAdapter must implement a port interface from domain/port/output/")

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

    // --- Notification module hexagonal rules ---

    @ArchTest
    val notificationDomainMustNotDependOnSpring: ArchRule =
            noClasses()
                    .that()
                    .resideInAPackage("..features.notification.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.validation..",
                            "org.hibernate.."
                    )
                    .because("Notification domain layer must be pure Kotlin")

    @ArchTest
    val notificationDomainMustNotDependOnInfrastructure: ArchRule =
            noClasses()
                    .that()
                    .resideInAPackage("..features.notification.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..features.notification.infrastructure..")
                    .because("Notification domain must not depend on its infrastructure")

    @ArchTest
    val notificationDomainMustNotDependOnDto: ArchRule =
            noClasses()
                    .that()
                    .resideInAPackage("..features.notification.domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..features.notification.dto..")
                    .because("Notification domain must not depend on DTOs")

    @ArchTest
    val notificationApplicationMustNotDependOnSpring: ArchRule =
            noClasses()
                    .that()
                    .resideInAPackage("..features.notification.application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "jakarta.validation..",
                            "org.hibernate.."
                    )
                    .because("Notification application layer must remain framework-agnostic")

    @ArchTest
    val notificationUseCaseImplsMustImplementInputPort: ArchRule =
            classes()
                    .that()
                    .resideInAPackage("..features.notification.application.usecase..")
                    .and()
                    .haveNameMatching(".*UseCaseImpl")
                    .should()
                    .implement(
                            com.tngtech.archunit.base.DescribedPredicate.describe(
                                    "an interface residing in notification domain/port/input package"
                            ) { iface: com.tngtech.archunit.core.domain.JavaClass ->
                                iface.name.contains(".notification.domain.port.input.")
                            }
                    )
                    .because("Every notification *UseCaseImpl must implement a port from domain/port/input/")
}
