package com.janusa.seaty

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.RestController

/**
 * Encodes the Controller -> Repository -> SQLite layering as architecture
 * rules. ArchUnit analyses compiled bytecode (the Kotlin/K2 version is irrelevant), so these run as
 * ordinary JUnit5 tests under Surefire and fail the build on a violation.
 *
 * Rules are written against Spring stereotype annotations rather than package or class names,
 * because the codebase uses a single flat package and Kotlin emits synthetic classes that
 * name-based rules would have to special-case.
 */
@AnalyzeClasses(
    packages = ["com.janusa.seaty"],
    importOptions = [ImportOption.DoNotIncludeTests::class],
)
class ArchitectureTest {
    /** The data layer must not reach back up into the web layer. */
    @ArchTest
    fun `repositories must not depend on controllers`(classes: JavaClasses) {
        noClasses()
            .that()
            .areAnnotatedWith(Repository::class.java)
            .should()
            .dependOnClassesThat()
            .areAnnotatedWith(RestController::class.java)
            .check(classes)
    }

    /** Controllers go through a repository for data access; they must not touch JdbcClient directly. */
    @ArchTest
    fun `controllers must not use the database directly`(classes: JavaClasses) {
        noClasses()
            .that()
            .areAnnotatedWith(RestController::class.java)
            .should()
            .dependOnClassesThat()
            .areAssignableTo(JdbcClient::class.java)
            .check(classes)
    }
}
