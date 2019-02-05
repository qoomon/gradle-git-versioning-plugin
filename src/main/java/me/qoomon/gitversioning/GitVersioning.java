package me.qoomon.gitversioning;

import me.qoomon.gradle.gitversioning.VersionFormatDescription;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static me.qoomon.UncheckedExceptions.unchecked;
import static me.qoomon.gitversioning.StringUtil.*;

public class GitVersioning {

    private final Boolean providedClean = null; // TODO
    private final String providedCommit = null; // TODO
    private final String providedBranch = null; // TODO
    private final String providedTag = null; // TODO
    private final VersionFormatDescription commitVersionDescription = new VersionFormatDescription(); // TODO
    private final List<VersionFormatDescription> branchVersionDescriptions = emptyList(); // TODO
    private final List<VersionFormatDescription> tagVersionDescriptions = emptyList(); // TODO

    private final File gitDir;

    private GitVersionDetails defaultGitVersionDetails;
    private VersionFormatDescription versionFormatDescription;

    public GitVersioning(File gitDir) {
        this.gitDir = gitDir;
    }

    public GitVersionDetails determineVersion(String currentVersion) {
        if (defaultGitVersionDetails == null) {
            GitRepoData gitRepoData = getGitRepoData(gitDir);
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

            // default versioning
            String gitRefType = "commit";
            String gitRefName = gitRepoData.commit;
            versionFormatDescription = commitVersionDescription;

            // branch versioning
            String gitRepoBranch = gitRepoData.branch;
            if (gitRepoBranch != null) {
                for (VersionFormatDescription versionFormatDescription : branchVersionDescriptions) {
                    if (gitRepoBranch.matches(versionFormatDescription.pattern)) {
                        gitRefType = "branch";
                        gitRefName = gitRepoBranch;
                        this.versionFormatDescription = versionFormatDescription;
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
                                    String versionLeft = removePrefix(tagLeft, versionFormatDescription.prefix);
                                    String versionRight = removePrefix(tagRight, versionFormatDescription.prefix);
                                    DefaultArtifactVersion tagVersionLeft = new DefaultArtifactVersion(versionLeft);
                                    DefaultArtifactVersion tagVersionRight = new DefaultArtifactVersion(versionRight);
                                    return tagVersionLeft.compareTo(tagVersionRight);
                                }).orElse(null);
                        if (gitRepoVersionTag != null) {
                            gitRefType = "tag";
                            gitRefName = gitRepoVersionTag;
                            this.versionFormatDescription = versionFormatDescription;
                            break;
                        }
                    }
                }
            }

            defaultGitVersionDetails = new GitVersionDetails(
                    gitRepoData.directory,
                    gitRepoData.clean,
                    gitRepoData.commit,
                    gitRefType,
                    removePrefix(gitRefName, versionFormatDescription.prefix),
                    valueGroupMap(versionFormatDescription.pattern, gitRefName),
                    null
            );
        }

        Map<String, String> projectVersionDataMap = new HashMap<>();
        projectVersionDataMap.put("version", currentVersion);
        projectVersionDataMap.put("version.release", currentVersion.replaceFirst("-SNAPSHOT$", ""));
        projectVersionDataMap.put("commit", defaultGitVersionDetails.getCommit());
        projectVersionDataMap.put("commit.short", defaultGitVersionDetails.getCommit().substring(0, 7));
        projectVersionDataMap.put(defaultGitVersionDetails.getCommitRefType(), defaultGitVersionDetails.getCommitRefName());
        projectVersionDataMap.putAll(defaultGitVersionDetails.getMetaData());

        String gitVersion = substituteText(versionFormatDescription.versionFormat, projectVersionDataMap);

        return new GitVersionDetails(
                defaultGitVersionDetails.getDirectory(),
                defaultGitVersionDetails.isClean(),
                defaultGitVersionDetails.getCommit(),
                defaultGitVersionDetails.getCommitRefType(),
                defaultGitVersionDetails.getCommitRefName(),
                defaultGitVersionDetails.getMetaData(),
                escapeVersion(gitVersion)
        );
    }

    private GitRepoData getGitRepoData(File directory) {
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder().findGitDir(directory);
        if (repositoryBuilder.getGitDir() == null) {
            throw new IllegalArgumentException(directory + " directory is not a git repository (or any of the parent directories)");
        }
        try (Repository repository = unchecked(repositoryBuilder::build)) {
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
