/*
 * Copyright 2017-2018 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.cli.io.support

import io.micronaut.cli.profile.Feature
import io.micronaut.cli.profile.Profile
import org.eclipse.aether.graph.Dependency

/**
 * @author James Kleeh
 * @sicen 1.0
 */
class GradleBuildTokens {

    Map getTokens(Profile profile, List<Feature> features) {
        Map tokens = [:]

        def ln = System.getProperty("line.separator")

        Closure repositoryUrl = { int spaces, String repo ->
            repo.startsWith('http') ? "${' ' * spaces}maven { url \"${repo}\" }" : "${' ' * spaces}${repo}"
        }

        def repositories = profile.repositories.collect(repositoryUrl.curry(4)).unique().join(ln)

        List<Dependency> profileDependencies = profile.dependencies
        def dependencies = profileDependencies.findAll() { Dependency dep ->
            dep.scope != 'build'
        }
        def buildDependencies = profileDependencies.findAll() { Dependency dep ->
            dep.scope == 'build'
        }

        for (Feature f in features) {
            dependencies.addAll f.dependencies.findAll() { Dependency dep -> dep.scope != 'build' }
            buildDependencies.addAll f.dependencies.findAll() { Dependency dep -> dep.scope == 'build' }
        }

        dependencies = dependencies.unique()

        dependencies = dependencies.sort({ Dependency dep -> dep.scope }).collect() { Dependency dep ->
            String artifactStr = resolveArtifactString(dep)
            "    ${dep.scope} \"${artifactStr}\"".toString()
        }.unique().join(ln)

        def buildRepositories = profile.buildRepositories.collect(repositoryUrl.curry(8)).unique().join(ln)

        buildDependencies = buildDependencies.collect() { Dependency dep ->
            String artifactStr = resolveArtifactString(dep)
            "        classpath \"${artifactStr}\"".toString()
        }.unique().join(ln)

        def buildPlugins = profile.buildPlugins.collect() { String name ->
            "apply plugin:\"$name\""
        }

        for (Feature f in features) {
            buildPlugins.addAll f.buildPlugins.collect() { String name ->
                "apply plugin:\"$name\""
            }
        }

        buildPlugins = buildPlugins.unique().join(ln)

        tokens.put("buildPlugins", buildPlugins)
        tokens.put("dependencies", dependencies)
        tokens.put("buildDependencies", buildDependencies)
        tokens.put("buildRepositories", buildRepositories)
        tokens.put("repositories", repositories)

        tokens
    }

    Map getTokens(List<String> services) {
        final String serviceString = services.collect { String name ->
            "include \'$name\'"
        }.join(System.getProperty("line.separator"))

        ["services": serviceString]
    }

    protected String resolveArtifactString(Dependency dep) {
        def artifact = dep.artifact
        def v = artifact.version.replace('BOM', '')

        return v ? "${artifact.groupId}:${artifact.artifactId}:${v}" : "${artifact.groupId}:${artifact.artifactId}"
    }
}
