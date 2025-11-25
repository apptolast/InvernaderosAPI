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
}
