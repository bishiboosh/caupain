/*
 * MIT License
 *
 * Copyright (c) 2025 Deezer
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.deezer.caupain

import com.deezer.caupain.model.Configuration
import com.deezer.caupain.model.Dependency
import com.deezer.caupain.model.GradleDependencyVersion
import com.deezer.caupain.model.Ignores
import com.deezer.caupain.model.Logger
import com.deezer.caupain.model.versionCatalog.Version
import com.deezer.caupain.model.versionCatalog.VersionCatalog
import com.deezer.caupain.serialization.DefaultJson
import com.deezer.caupain.serialization.DefaultXml
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.serialization.kotlinx.xml.xml
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DependencyUpdateCheckerEngineTest {

    private lateinit var fileSystem: FakeFileSystem

    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var checker: DependencyUpdateChecker

    @BeforeTest
    fun setup() {
        fileSystem = FakeFileSystem()
        val configuration = Configuration()
        fileSystem.createDirectories(configuration.versionCatalogPath.parent!!)
        fileSystem.write(configuration.versionCatalogPath) {}
        checker = DefaultDependencyUpdateChecker(
            configuration = configuration,
            fileSystem = fileSystem,
            ioDispatcher = testDispatcher,
            versionCatalogParser = FixedVersionCatalogParser,
            logger = Logger.EMPTY,
            policies = emptyList(),
            httpClient = HttpClient {
                install(ContentNegotiation) {
                    json(DefaultJson)
                    xml(DefaultXml, ContentType.Any)
                }
            },
            currentGradleVersion = null
        )
    }

    @AfterTest
    fun teardown() {
        fileSystem.checkNoOpenFiles()
        fileSystem.close()
    }

    @Test
    fun testEngine() = runTest(testDispatcher) {
        checker.checkForUpdates()
    }

    private object FixedVersionCatalogParser : VersionCatalogParser {
        override suspend fun parseDependencyInfo() =
            VersionCatalogParseResult(VERSION_CATALOG, Ignores())
    }
}

private val VERSION_CATALOG = VersionCatalog(
    versions = mapOf(
        "groovy" to Version.Simple(GradleDependencyVersion.Exact("3.0.5-alpha-1")),
        "checkstyle" to Version.Simple(GradleDependencyVersion.Exact("8.37"))
    ),
    libraries = mapOf(
        "groovy-core" to Dependency.Library(
            module = "org.codehaus.groovy:groovy",
            version = Version.Reference("groovy")
        ),
        "groovy-json" to Dependency.Library(
            module = "org.codehaus.groovy:groovy-json",
            version = Version.Reference("groovy")
        ),
        "groovy-nio" to Dependency.Library(
            module = "org.codehaus.groovy:groovy-nio",
            version = Version.Reference("groovy")
        ),
        "commons-lang3" to Dependency.Library(
            group = "org.apache.commons",
            name = "commons-lang3",
            version = Version.Rich(
                strictly = GradleDependencyVersion.Range("[3.8, 4.0["),
                prefer = GradleDependencyVersion.Exact("3.9")
            )
        )
    ),
    plugins = mapOf(
        "versions" to Dependency.Plugin(
            id = "com.github.ben-manes.versions",
            version = Version.Simple(GradleDependencyVersion.Snapshot("0.45.0-SNAPSHOT"))
        )
    )
)