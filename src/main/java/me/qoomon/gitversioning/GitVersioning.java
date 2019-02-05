package me.qoomon.gitversioning;

import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import static me.qoomon.UncheckedExceptions.unchecked;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import me.qoomon.gradle.gitversioning.VersionFormatDescription;

public class GitVersioning {

    private final Boolean providedClean = null; // TODO
    private final String providedCommit = null; // TODO
    private final String providedBranch = null; // TODO
    private final String providedTag = null; // TODO
    private final VersionFormatDescription commitVersionDescription = new VersionFormatDescription(); // TODO
    private final List<VersionFormatDescription> branchVersionDescriptions = emptyList(); // TODO
    private final List<VersionFormatDescription> tagVersionDescriptions = emptyList(); // TODO

    private final File gitDir;

    public GitVersioning(File gitDir) {
        this.gitDir = gitDir;
    }

    public GitVersionDetails determineVersion(String currentVersion) {
        final GitRepoData gitRepoData = getGitRepoData(gitDir);
        if (providedClean != null) {
            gitRepoData.clean = providedClean;
        }
        if (providedCommit != null) {
            gitRepoData.commit = providedCommit;
        }
        if (providedBranch != null) {
            gitRepoData.branch = providedBranch.equals("") ? null : providedBranch;
        }
        if (providedTag != null) {
            gitRepoData.tags = providedTag.equals("") ? emptyList() : singletonList(providedTag);
        }

        // TODO cache gitRepoData

        // default versioning
        String projectCommitRefType = "commit";
        String projectCommitRefName = gitRepoData.commit;
        VersionFormatDescription projectVersionFormatDescription = commitVersionDescription;

        // branch versioning
        String gitRepoBranch = gitRepoData.branch;
        if (gitRepoBranch != null) {
            for (VersionFormatDescription versionFormatDescription : branchVersionDescriptions) {
                if (gitRepoBranch.matches(versionFormatDescription.pattern)) {
                    projectCommitRefType = "branch";
                    projectCommitRefName = gitRepoBranch;
                    projectVersionFormatDescription = versionFormatDescription;
                    break;
                }
            }
        } else {
            // tag versioning
            List<String> gitRepoTags = gitRepoData.tags;
            if (!gitRepoTags.isEmpty()) {
                for (VersionFormatDescription versionFormatDescription : tagVersionDescriptions) {
                    String gitRepoVersionTag = gitRepoTags.stream().sequential()
                            .filter(tag -> tag.matches(versionFormatDescription.pattern))
                            .max((tagLeft, tagRight) -> {
                                String versionLeft = StringUtil.removePrefix(tagLeft, versionFormatDescription.prefix);
                                String versionRight = StringUtil.removePrefix(tagRight, versionFormatDescription.prefix);
                                DefaultArtifactVersion tagVersionLeft = new DefaultArtifactVersion(versionLeft);
                                DefaultArtifactVersion tagVersionRight = new DefaultArtifactVersion(versionRight);
                                return tagVersionLeft.compareTo(tagVersionRight);
                            }).orElse(null);
                    if (gitRepoVersionTag != null) {
                        projectCommitRefType = "tag";
                        projectCommitRefName = gitRepoVersionTag;
                        projectVersionFormatDescription = versionFormatDescription;
                        break;
                    }
                }
            }
        }

        String projectCommitRefNameTrimed = StringUtil.removePrefix(projectCommitRefName, projectVersionFormatDescription.prefix);

        Map<String, String> projectVersionDataMap = new HashMap<>();
        projectVersionDataMap.put("version", currentVersion);
        projectVersionDataMap.put("version.release", currentVersion.replaceFirst("-SNAPSHOT$", ""));
        projectVersionDataMap.put("commit", gitRepoData.commit);
        projectVersionDataMap.put("commit.short", gitRepoData.commit.substring(0, min(7, gitRepoData.commit.length())));
        projectVersionDataMap.put(projectCommitRefType, projectCommitRefNameTrimed);
        projectVersionDataMap.putAll(StringUtil.valueGroupMap(projectVersionFormatDescription.pattern, projectCommitRefName));

        String gitVersion = escapeVersion(
                StringUtil.substituteText(projectVersionFormatDescription.versionFormat, projectVersionDataMap));

        return new GitVersionDetails(
                gitRepoData.directory, gitRepoData.clean,
                gitRepoData.commit,
                projectCommitRefType,
                projectCommitRefNameTrimed,
                gitVersion
        );
    }

    private GitRepoData getGitRepoData(File directory) {
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder().findGitDir(directory);
        if (repositoryBuilder.getGitDir() == null){
            throw new IllegalArgumentException(directory + " directory is not a git repository (or any of the parent directories)");
        }
        try (Repository repository =  unchecked(repositoryBuilder::build)) {
                boolean headClean = GitUtil.status(repository).isClean();
                String headCommit = GitUtil.revParse(repository, Constants.HEAD);
                String headBranch = GitUtil.branch(repository);
                List<String> headTags = GitUtil.tag_pointsAt(repository, Constants.HEAD);
                return new GitRepoData(repository.getDirectory(), headClean, headCommit, headBranch, headTags);
            }

    }

    private static String escapeVersion(String version) {
        return version.replace("/", "-");
    }

    private class GitRepoData {

        private File directory;
        private boolean clean;
        private String commit;
        private String branch;
        private List<String> tags;

        GitRepoData(File directory, boolean clean, String commit, String branch, List<String> tags) {
            this.directory = directory;
            this.clean = clean;
            this.commit = commit;
            this.branch = branch;
            this.tags = tags;
        }
    }
}
