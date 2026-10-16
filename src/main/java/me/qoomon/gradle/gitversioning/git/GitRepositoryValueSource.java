package me.qoomon.gradle.gitversioning.git;

import me.qoomon.gitversioning.commons.JGitRepositoryReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.Describable;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.provider.ValueSource;
import org.gradle.api.provider.ValueSourceParameters;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Base for value sources reading from the git repository containing {@link Parameters#getProjectDirectory()}.
 * <p>
 * All JGit access during configuration has to happen within a {@link ValueSource}: the configuration cache
 * neither tracks the files JGit reads nor allows the external processes JGit may start (e.g. to discover the
 * system git config). Within a value source, Gradle re-obtains the value on each build instead and reuses
 * the cache entry as long as the value stays the same.
 * <p>
 * Obtains {@code null} if the project directory is not part of a git repository.
 */
public abstract class GitRepositoryValueSource<T, P extends GitRepositoryValueSource.Parameters>
        implements ValueSource<T, P>, Describable {

    public interface Parameters extends ValueSourceParameters {
        DirectoryProperty getProjectDirectory();
    }

    @Override
    public @Nullable T obtain() {
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder().findGitDir(getProjectDirectory());
        if (repositoryBuilder.getGitDir() == null) {
            return null;
        }
        try (Repository repository = repositoryBuilder.build()) {
            return read(new JGitRepositoryReader(repository));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    protected abstract T read(JGitRepositoryReader reader) throws IOException;

    /**
     * @return what is read, used in configuration cache invalidation messages
     */
    protected abstract String describeValue();

    @Override
    public String getDisplayName() {
        return describeValue() + " of " + getProjectDirectory();
    }

    private File getProjectDirectory() {
        return getParameters().getProjectDirectory().get().getAsFile();
    }
}
