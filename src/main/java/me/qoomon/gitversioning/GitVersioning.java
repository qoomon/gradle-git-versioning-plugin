package me.qoomon.gitversioning;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import javax.annotation.Nonnull;
import java.util.*;

import static java.util.Comparator.comparing;
import static me.qoomon.gitversioning.StringUtil.*;

public class GitVersioning {

    final private GitVersionDetails commonGitVersionDetails;
    final private String versionFormat;

    public GitVersioning(final GitVersionDetails defaultGitVersionDetails,
                         final String versionFormat) {
        this.commonGitVersionDetails = defaultGitVersionDetails;
        this.versionFormat = versionFormat;
    }

    public static GitVersioning build(final GitRepoData gitRepoData,
                                      final VersionDescription commitVersionDescription,
                                      final List<VersionDescription> branchVersionDescriptions,
                                      final List<VersionDescription> tagVersionDescriptions) {
        Objects.requireNonNull(gitRepoData);
        Objects.requireNonNull(commitVersionDescription);
        Objects.requireNonNull(branchVersionDescriptions);
        Objects.requireNonNull(tagVersionDescriptions);

        // default versioning
        String gitRefType = "commit";
        String gitRefName = gitRepoData.getCommit();
        VersionDescription versionDescription = commitVersionDescription;

        if (gitRepoData.getBranch() != null) {
            // branch versioning
            for (final VersionDescription branchVersionDescription : branchVersionDescriptions) {
                Optional<String> versionBranch = Optional.of(gitRepoData.getBranch())
                        .filter(branch -> branch.matches(branchVersionDescription.getPattern()));
                if (versionBranch.isPresent()) {
                    gitRefType = "branch";
                    gitRefName = versionBranch.get();
                    versionDescription = branchVersionDescription;
                    break;
                }
            }
        } else if (!gitRepoData.getTags().isEmpty()) {
            // tag versioning
            for (final VersionDescription tagVersionDescription : tagVersionDescriptions) {
                Optional<String> versionTag = gitRepoData.getTags().stream()
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
                gitRepoData.isClean(),
                gitRepoData.getCommit(),
                gitRefType,
                removePrefix(gitRefName, versionDescription.getPrefix()),
                valueGroupMap(versionDescription.getPattern(), gitRefName),
                null
        );

        return new GitVersioning(defaultGitVersionDetails, versionDescription.getVersionFormat());
    }

    @Nonnull
    public GitVersionDetails determineVersion(String currentVersion) {

        Map<String, String> projectVersionDataMap = new HashMap<>();
        projectVersionDataMap.put("version", currentVersion);
        projectVersionDataMap.put("version.release", currentVersion.replaceFirst("-SNAPSHOT$", ""));
        projectVersionDataMap.put("commit", commonGitVersionDetails.getCommit());
        projectVersionDataMap.put("commit.short", commonGitVersionDetails.getCommit().substring(0, 7));
        projectVersionDataMap.put(commonGitVersionDetails.getCommitRefType(), commonGitVersionDetails.getCommitRefName());
        projectVersionDataMap.putAll(commonGitVersionDetails.getMetaData());

        String gitVersion = substituteText(versionFormat, projectVersionDataMap);

        return new GitVersionDetails(
                commonGitVersionDetails.isClean(),
                commonGitVersionDetails.getCommit(),
                commonGitVersionDetails.getCommitRefType(),
                commonGitVersionDetails.getCommitRefName(),
                commonGitVersionDetails.getMetaData(),
                normalizeVersionCharacters(gitVersion)
        );
    }

    private static String normalizeVersionCharacters(String version) {
        return version.replace("/", "-");
    }
}
