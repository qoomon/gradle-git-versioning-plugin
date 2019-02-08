package me.qoomon.gitversioning;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.util.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;

import static org.eclipse.jgit.lib.Constants.HEAD;

import static me.qoomon.UncheckedExceptions.unchecked;
import static me.qoomon.gitversioning.StringUtil.*;

import javax.annotation.Nonnull;

public class GitVersioning {

    final private GitVersionDetails defaultGitVersionDetails;
    final private VersionDescription versionDescription;

    public GitVersioning(final GitVersionDetails defaultGitVersionDetails,
                         final VersionDescription versionDescription) {
        this.defaultGitVersionDetails = defaultGitVersionDetails;
        this.versionDescription = versionDescription;
    }

    public static GitVersioning build(File directory,
                                      final VersionDescription commitVersionDescription,
                                      final List<VersionDescription> branchVersionDescriptions,
                                      final List<VersionDescription> tagVersionDescriptions) {
        Objects.requireNonNull(directory);
        Objects.requireNonNull(commitVersionDescription);
        Objects.requireNonNull(branchVersionDescriptions);
        Objects.requireNonNull(tagVersionDescriptions);

        Boolean providedClean = null; // TODO
        String providedCommit = null; // TODO
        String providedBranch = null; // TODO
        String providedTag = null; // TODO

        GitRepoData gitRepoData = getGitRepoData(directory);
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
        VersionDescription versionDescription = commitVersionDescription;

        if (gitRepoData.branch != null) {
            // branch versioning
            for (final VersionDescription branchVersionDescription : branchVersionDescriptions) {
                Optional<String> versionBranch = Optional.of(gitRepoData.branch)
                        .filter(branch -> branch.matches(branchVersionDescription.getPattern()));
                if (versionBranch.isPresent()) {
                    gitRefType = "branch";
                    gitRefName = versionBranch.get();
                    versionDescription = branchVersionDescription;
                    break;
                }
            }
        } else if (!gitRepoData.tags.isEmpty()) {
            // tag versioning
            for (final VersionDescription tagVersionDescription : tagVersionDescriptions) {
                Optional<String> versionTag = gitRepoData.tags.stream()
                        .filter(tag -> tag.matches(tagVersionDescription.getPattern()))
                        .max(comparing(DefaultArtifactVersion::new));
                if (versionTag.isPresent()) {
                    gitRefType = "tag";
                    gitRefName = versionTag.get();
                    versionDescription = tagVersionDescription;
                    break;
                }
            }
        }

        GitVersionDetails defaultGitVersionDetails = new GitVersionDetails(
                gitRepoData.directory,
                gitRepoData.clean,
                gitRepoData.commit,
                gitRefType,
                removePrefix(gitRefName, versionDescription.getPrefix()),
                valueGroupMap(versionDescription.getPattern(), gitRefName),
                null
        );

        return new GitVersioning(defaultGitVersionDetails, versionDescription);
    }

    @Nonnull
    public GitVersionDetails determineVersion(String currentVersion) {

        Map<String, String> projectVersionDataMap = new HashMap<>();
        projectVersionDataMap.put("version", currentVersion);
        projectVersionDataMap.put("version.release", currentVersion.replaceFirst("-SNAPSHOT$", ""));
        projectVersionDataMap.put("commit", defaultGitVersionDetails.getCommit());
        projectVersionDataMap.put("commit.short", defaultGitVersionDetails.getCommit().substring(0, 7));
        projectVersionDataMap.put(defaultGitVersionDetails.getCommitRefType(), defaultGitVersionDetails.getCommitRefName());
        projectVersionDataMap.putAll(defaultGitVersionDetails.getMetaData());

        String gitVersion = substituteText(versionDescription.getVersionFormat(), projectVersionDataMap);

        return new GitVersionDetails(
                defaultGitVersionDetails.getDirectory(),
                defaultGitVersionDetails.isClean(),
                defaultGitVersionDetails.getCommit(),
                defaultGitVersionDetails.getCommitRefType(),
                defaultGitVersionDetails.getCommitRefName(),
                defaultGitVersionDetails.getMetaData(),
                normalizeVersionCharacters(gitVersion)
        );
    }

    private static GitRepoData getGitRepoData(File directory) {
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder().findGitDir(directory);
        if (repositoryBuilder.getGitDir() == null) {
            throw new IllegalArgumentException(
                    directory + " directory is not a git repository (or any of the parent directories)");
        }
        try (Repository repository = unchecked(repositoryBuilder::build)) {
            boolean headClean = GitUtil.status(repository).isClean();
            String headCommit = GitUtil.revParse(repository, HEAD);
            String headBranch = GitUtil.branch(repository);
            List<String> headTags = GitUtil.tag_pointsAt(repository, HEAD);
            return new GitRepoData(repository.getDirectory(), headClean, headCommit, headBranch, headTags);
        }
    }

    private static String normalizeVersionCharacters(String version) {
        return version.replace("/", "-");
    }

    private static class GitRepoData {

        private final File directory;
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
