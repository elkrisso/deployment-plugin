package net.mayope.deployplugin

import org.gradle.api.Project

internal class ProfileStore {
    fun init(project: Project) {
        addProfiles(
            project.defaultExtension().deploymentProfiles(), project.extension()?.profiles ?: emptyList()
        )
    }

    private val profileMap = mutableMapOf<String, Profile>()

    fun addProfiles(defaultProfiles: List<Profile>, profiles: List<Profile>) {
        profiles.forEach {
            profileMap[it.name] = it
        }
        defaultProfiles.forEach {
            fillMissingProperties(it)
        }
    }

    fun profiles() = profileMap.values.map { ValidatedProfile(it) }

    private fun fillMissingProperties(profile: Profile) {
        if (profile.name !in profileMap) {
            return
        }
        profileMap[profile.name]!!.let {
            configureProfile(it, profile)
        }
    }

    private fun configureProfile(existing: Profile, profile: Profile) {
        existing.deploy?.apply {
            configureDeploy(profile)
        }
        existing.dockerBuild?.apply {
            configureDockerBuild(profile)
        }
        existing.helmPush?.apply {
            configureHelmPush(profile)
        }
        existing.dockerLogin?.apply {
            configureDockerLogin(profile)
        }
        existing.dockerPush?.apply {
            configureDockerPush(profile)
        }
    }

    private fun DockerBuildProfile.configureDockerBuild(profile: Profile) {
        prepareTask = profile.dockerBuild?.prepareTask ?: prepareTask
        dockerDir = dockerDir ?: profile.dockerBuild?.dockerDir
        version = version ?: profile.dockerBuild?.version
        buildOutputTask = buildOutputTask ?: profile.dockerBuild?.buildOutputTask
    }

    private fun DockerPushProfile.configureDockerPush(profile: Profile) {
        awsProfile = awsProfile ?: profile.dockerPush?.awsProfile
        loginMethod = loginMethod ?: profile.dockerPush?.loginMethod
        registryRoot = registryRoot ?: profile.dockerPush?.registryRoot
        loginUsername = loginUsername ?: profile.dockerPush?.loginUsername
        loginPassword = loginPassword ?: profile.dockerPush?.loginPassword
    }

    private fun DockerLoginProfile.configureDockerLogin(profile: Profile) {
        awsProfile = awsProfile ?: profile.dockerPush?.awsProfile
        loginMethod = loginMethod ?: profile.dockerPush?.loginMethod
        registryRoot = registryRoot ?: profile.dockerPush?.registryRoot
        loginUsername = loginUsername ?: profile.dockerPush?.loginUsername
        loginPassword = loginPassword ?: profile.dockerPush?.loginPassword
    }

    private fun DeployProfile.configureDeploy(profile: Profile) {
        attributes = attributes ?: profile.deploy?.attributes
        helmDir = helmDir ?: profile.deploy?.helmDir
        kubeConfig = kubeConfig ?: profile.deploy?.kubeConfig
        targetNamespaces = targetNamespaces ?: profile.deploy?.targetNamespaces
    }

    private fun HelmPushProfile.configureHelmPush(profile: Profile) {
        helmDir = helmDir ?: profile.helmPush?.helmDir
        repositoryUrl = repositoryUrl ?: profile.helmPush?.repositoryUrl
        repositoryUsername = repositoryUsername ?: profile.helmPush?.repositoryUsername
        repositoryPassword = repositoryPassword ?: profile.helmPush?.repositoryPassword
    }
}

private fun Project.extension() = extensions.findByType(DeployExtension::class.java)

private fun Project.defaultExtension() =
    rootProject.extensions.findByType(DefaultDeployExtension::class.java) ?: DefaultDeployExtension()
