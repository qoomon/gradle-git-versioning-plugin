package me.qoomon.gradle.gitversioning;

import me.qoomon.gitversioning.commons.GitSituation;
import me.qoomon.gradle.gitversioning.GitVersioningPluginConfig.RefPatchDescription;
import org.gradle.api.HasImplicitReceiver;

@FunctionalInterface
@HasImplicitReceiver
interface RefPatchDescriptionProvider {
    void action(RefPatchDescription description, GitSituation gitSituation);
}
