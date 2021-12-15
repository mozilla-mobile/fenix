/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.experiments.nimbus.gradle

import org.gradle.api.Task

import java.util.zip.ZipFile
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.TaskProvider

abstract class NimbusPluginExtension {
    /**
     * The .fml.yaml manifest file.
     *
     * If absent this defaults to `nimbus.fml.yaml`.
     * If relative, it is relative to the project root.
     *
     * @return
     */
    abstract Property<String> getManifestFile()

    String getManifestFileActual(Project project) {
        var filename = this.manifestFile.get() ?: "nimbus.fml.yaml"
        return [project.rootDir, filename].join(File.separator)
    }

    /**
     * The mapping between the build variant and the release channel.
     *
     * Variants that are not in this map are used literally.
     * @return
     */
    abstract MapProperty<String, String> getChannels()

    String getChannelActual(variant) {
        Map<String, String> channels = this.channels.get() ?: new HashMap()
        return channels.getOrDefault(variant.name, variant.name)
    }

    /**
     * The qualified class name which is the destination for the feature classes.
     *
     * Like classes in the `AndroidManifest.xml` if the class name starts with a `.`,
     * the package is derived from the app's package name.
     *
     * If not present, it defaults to `.nimbus.MyNimbus`.
     *
     * @return
     */
    abstract Property<String> getDestinationClass()

    def getPackageClassLiteral() {
        var fqnClass = destinationClass.get() ?: ".nimbus.MyNimbus"
        def i = fqnClass.lastIndexOf('.')
        switch (i) {
            case -1: return ["", fqnClass]
            case 0: return [".", fqnClass.substring(i + 1)]
            default: return [fqnClass.substring(0, i), fqnClass.substring(i + 1)]
        }
    }

    String getPackageNameActual(variant) {
        def (packageName, _) = getPackageClassLiteral()
        if (!packageName.startsWith(".")) {
            return packageName
        }
        TaskProvider buildConfigProvider = variant.getGenerateBuildConfigProvider()
        def configProvider = buildConfigProvider.get()
        def appPackageName
        // In Gradle 6.x `getBuildConfigPackageName` was replaced by `namespace`.
        // We want to be forward compatible, so we check that first or fallback to the old method.
        if (configProvider.hasProperty("namespace")) {
            appPackageName = configProvider.namespace.get()
        } else {
            appPackageName = configProvider.getBuildConfigPackageName().get()
        }
        if (packageName == ".") {
            return appPackageName
        } else {
            return appPackageName + packageName
        }
    }

    String getClassNameActual() {
        def (_, className) = getPackageClassLiteral()
        return className
    }

    /**
     * The filename of the manifest ingested by Experimenter.
     *
     * If this is a relative name, it is taken to be relative to the project's root directory.
     *
     * If missing, this defaults to `.experimenter.json`.
     * @return
     */
    abstract Property<String> getExperimenterManifest()

    String getExperimenterManifestActual(Project project) {
        var filename = this.experimenterManifest.get() ?: ".experimenter.json"
        return [project.rootDir, filename].join(File.separator)
    }

    File getAppServicesActual(Project project) {
        if (!project.hasProperty("appServicesSrcDir")) {
            return null
        }
        def asDir = project.property"appServicesSrcDir"
        if (asDir == null || asDir.isBlank()) {
            return null
        }


        def dir = new File([project.rootDir, asDir].join(File.separator))
        if (dir.exists() && dir.isDirectory()) {
            return dir
        }

        return null
    }
}

class NimbusPlugin implements Plugin<Project> {

    public static final String APPSERVICES_FML_HOME = "components/support/nimbus-fml"

    void apply(Project project) {
        def extension = project.extensions.create('nimbus', NimbusPluginExtension)

        if (project.hasProperty("android")) {
            Collection<Task> oneTimeTasks = new ArrayList<>()
            if (project.android.hasProperty('applicationVariants')) {
                project.android.applicationVariants.all { variant ->
                    setupVariantTasks(variant, project, extension, oneTimeTasks, false)
                }
            }

            if (project.android.hasProperty('libraryVariants')) {
                project.android.libraryVariants.all { variant ->
                    setupVariantTasks(variant, project, extension, oneTimeTasks, true)
                }
            }
        }
    }

    def setupAssembleNimbusTools(Project project, NimbusPluginExtension extension) {
        return project.task("assembleNimbusTools") {
            group "Nimbus"
            description "Fetch the Nimbus FML tools from Application Services"
            doLast {
                def asDir = extension.getAppServicesActual(project)
                if (asDir == null) {
                    fetchNimbusBinaries(project)
                }
            }
        }
    }

    def getApplicationServiceVersion() {
        // We would get this from the plugin itself but we don't know this vital piece of information
        // We don't spend much time looking it up now because:
        // a) this plugin is going to live in the AS repo (eventually)
        // b) the nimbus-fml tooling is currently being built on a branch
        return "87.1.0"
    }

    // Try one or more hosts to download the given file.
    // Return the hostname that successfully downloaded, or null if none succeeded.
    def tryDownload(File directory, String filename, String[] urlPrefixes) {
        return urlPrefixes.find { prefix ->
            def urlString = filename == null ? prefix : "$prefix/$filename"
            try {
                new URL(urlString).withInputStream { from ->
                    new File(directory, filename).withOutputStream { out ->
                        out << from;
                    }
                }
                true
            } catch (e) {
                false
            }
        }
    }

    // Fetches and extracts the pre-built nimbus-fml binaries
    def fetchNimbusBinaries(Project project) {
        def fmlPath = new File(getFMLPath(project))
        println("Checking fml binaries in $fmlPath")
        if (fmlPath.exists()) {
            println("nimbus-fml already exists at $fmlPath")
            return
        }

        def rootDirectory = new File(getFMLRoot(project))
        def archive = new File(rootDirectory, "nimbus-fml.zip")
        ensureDirExists(rootDirectory)

        if (!archive.exists()) {
            println("Downloading archive to $archive")
            // Currently the app-services version is unknown to Fenix, so we're going to give a number of
            // options of where to get the tooling. Eventually, we'll just get the specific version
            // from CircleCI, tied to a version of the the Nimbus runtime.
            // However, while this plugin lives in Fenix, and the nimbus-fml isn't tied to a specific
            // release of AS, then we have to start somewhere.
            def asVersion = getApplicationServiceVersion()
            def successfulHost = tryDownload(archive.getParentFile(), archive.getName(),
                    // The version tied to this plugin
                    "https://github.com/mozilla/application-services/releases/download/v$asVersion",
                    // …the latest one from github.
                    "https://github.com/mozilla/application-services/releases/latest/download",
                    // …or the first one ever.
                    "https://123611-129966583-gh.circle-artifacts.com/0"
            )

            if (successfulHost == null) {
                throw java.io.IOException("Unable to download nimbus-fml tooling")
            }

            // We get the checksum, although don't do anything with it yet;
            // Checking it here would be able to detect if the zip file was tampered with
            // in transit between here and the server.
            // It won't detect compromise of the CI server.
            tryDownload(rootDirectory, "nimbus-fml.sha256", successfulHost)
        }

        def archOs = getArchOs()
        println("Unzipping binary, looking for $archOs/nimbus-fml")
        def zipFile = new ZipFile(archive)
        zipFile.entries().findAll { entry ->
            return !entry.directory && entry.name.contains(archOs)
        }.each { entry ->
            fmlPath.withOutputStream { out ->
                def from = zipFile.getInputStream(entry)
                out << from
                from.close()
            }

            fmlPath.setExecutable(true)
        }
    }

    /**
     * The directory where nimbus-fml will live.
     * We put it in a build directory so we refresh it on a clean build.
     * @param project
     * @return
     */
    def getFMLRoot(Project project) {
        return [project.buildDir, "bin", "nimbus"].join(File.separator)
    }

    def getArchOs() {
        String osPart
        String os = System.getProperty("os.name").toLowerCase()
        if (os.contains("win")) {
            osPart = "windows"
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            osPart = "unknown-linux-musl"
        } else if (os.contains("mac")) {
            osPart = "apple-darwin"
        } else {
            osPart = "unknown"
        }

        String arch = System.getProperty("os.arch").toLowerCase()
        String archPart
        if (arch.contains("x86_64")) {
            archPart = "x86_64"
        } else if (arch.contains("aarch")) {
            archPart = "aarch64"
        } else {
            archPart = "unknown"
        }

        return "${archPart}-${osPart}"
    }

    def getFMLPath(Project project) {
        return [getFMLRoot(project), "nimbus-fml"].join(File.separator)
    }

    def setupVariantTasks(variant, project, extension, oneTimeTasks, isLibrary = false) {
        def task = setupNimbusFeatureTasks(variant, project, extension)

        if (oneTimeTasks.isEmpty()) {
            // The extension doesn't seem to be ready until now, so we have this complicated
            // oneTimeTasks thing going on here. Ideally, we'd run this outside of this function.
            def assembleToolsTask = setupAssembleNimbusTools(project, extension)
            oneTimeTasks.add(assembleToolsTask)

            if (!isLibrary) {
                def experimenterTask = setupExperimenterTasks(project, extension, isLibrary)
                experimenterTask.dependsOn(assembleToolsTask)
                oneTimeTasks.add(experimenterTask)
            }
        }

        // Generating experimenter manifest is cheap, for now.
        // So we generate this every time.
        // In the future, we should try and make this an incremental task.
        oneTimeTasks.forEach {oneTimeTask ->
            if (oneTimeTask != null) {
                task.dependsOn(oneTimeTask)
            }
        }
    }

    def setupNimbusFeatureTasks(variant, project, extension) {
        String packageName = extension.getPackageNameActual(variant)
        String className = extension.classNameActual

        String channel = extension.getChannelActual(variant)

        var parts = [project.buildDir, "generated", "source", "nimbus", variant.name, "kotlin"]
        var sourceOutputDir = parts.join(File.separator)

        parts.addAll(packageName.split("\\."))
        var outputDir = parts.join(File.separator)
        var outputFile = [outputDir, "${className}.kt"].join(File.separator)

        var inputFile = extension.getManifestFileActual(project)

        var localAppServices = extension.getAppServicesActual(project)

        var generateTask = project.task("nimbusFeatures${variant.name.capitalize()}", type: Exec) {
            description = "Generate Kotlin data classes for Nimbus enabled features"
            group = "Nimbus"

            doFirst {
                ensureDirExists(new File(sourceOutputDir))
                ensureDirExists(new File(outputDir))
                println("Nimbus FML generating Kotlin")
                println("manifest   $inputFile")
                println("class      ${packageName}.${className}")
                println("channel    $channel")
            }

            doLast {
                println("outputFile $outputFile")
            }

            if (localAppServices == null) {
                workingDir project.rootDir
                commandLine getFMLPath(project)
            } else {
                workingDir new File(localAppServices, APPSERVICES_FML_HOME)
                commandLine "cargo"
                args "run", "--"
            }
            args inputFile
            args "android", "features"
            args "--classname", className
            args "--output", outputFile
            args "--package", packageName
            args "--channel", channel
        }
        variant.registerJavaGeneratingTask(generateTask, new File(sourceOutputDir))

        def generateSourcesTask = project.tasks.findByName("generate${variant.name.capitalize()}Sources")
        if (generateSourcesTask != null) {
            generateSourcesTask.dependsOn(generateTask)
        } else {
            def compileTask = project.tasks.findByName("compile${variant.name.capitalize()}Kotlin")
            compileTask.dependsOn(generateTask)
        }

        return generateTask
    }

    def setupExperimenterTasks(project, extension, isLibrary = false) {
        // Experimenter works at the application level, not the library/project
        // level. The experimenter task for the application should gather up manifests
        // from its constituent libraries.
        if (isLibrary) {
            return null
        }

        var inputFile = extension.getManifestFileActual(project)
        var outputFile = extension.getExperimenterManifestActual(project)
        var localAppServices = extension.getAppServicesActual(project)

        return project.task("nimbusExperimenter", type: Exec) {
            description = "Generate feature manifest for Nimbus server (Experimenter)"
            group = "Nimbus"

            doFirst {
                println("Nimbus FML generating JSON")
                println("manifest   $inputFile")
            }

            doLast {
                println("outputFile $outputFile")
            }

            if (localAppServices == null) {
                workingDir project.rootDir
                commandLine getFMLPath(project)
            } else {
                workingDir new File(localAppServices, APPSERVICES_FML_HOME)
                commandLine "cargo"
                args "run", "--"
            }
            args inputFile
            args "experimenter"
            args "--output", outputFile
        }
    }

    def ensureDirExists(File dir) {
        if (dir.exists()) {
            if (!dir.isDirectory()) {
                dir.delete()
                dir.mkdirs()
            }
        } else {
            dir.mkdirs()
        }
    }
}
