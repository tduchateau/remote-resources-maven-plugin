package com.github.keub.maven.plugin.service;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.keub.maven.plugin.exception.InvalidSourceException;
import com.github.keub.maven.plugin.model.Resource;
import com.github.keub.maven.plugin.resources.CopyResourcesMojo;
import com.github.keub.maven.plugin.service.FileService;
import com.github.keub.maven.plugin.service.IncludeService;

public class GitServiceTest {

	private static final File gitSandboxDirectory = new File("src/test/resources/git_sandbox");
	private static final File pjtSandboxDirectory = new File("src/test/resources/pjt_sandbox");
	private static final File sandboxDirectory = new File("src/test/resources/sandbox");
	private static final String subOutputDirectory = "/src/main/filters";
	private static Git git;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession session;

	@BeforeClass
	public static void init_test() throws GitAPIException, IOException {
		// init git repo
		git = Git.init().setDirectory(gitSandboxDirectory).call();

		FileUtils.copyDirectory(sandboxDirectory, gitSandboxDirectory);
	}

	@Test
	public void test_copy_local_git_files_into_sandbox() throws InvalidSourceException, IOException {
		CopyResourcesMojo copyResourcesMojo = new CopyResourcesMojo();

		copyResourcesMojo.setProject(project);
		copyResourcesMojo.setSession(session);

		File outputDirectory = new File(pjtSandboxDirectory.getAbsolutePath().concat(subOutputDirectory));
		// raz outputDirectory
		FileUtils.cleanDirectory(outputDirectory);

		FileService.copyFilesIntoOutputDirectory(copyResourcesMojo, gitSandboxDirectory, outputDirectory,
				new Resource());

		Set<String> sourceFiles = FileService.findFiles(gitSandboxDirectory);
		Set<String> destinationFiles = FileService.findFiles(outputDirectory);

		Assert.assertEquals(sourceFiles.size(), destinationFiles.size());
		FileUtils.cleanDirectory(outputDirectory);
	}

	@Test
	public void test_copy_local_git_only_includes_files_into_sandbox() throws InvalidSourceException, IOException {
		CopyResourcesMojo copyResourcesMojo = new CopyResourcesMojo();

		copyResourcesMojo.setProject(project);
		copyResourcesMojo.setSession(session);

		File outputDirectory = new File(pjtSandboxDirectory.getAbsolutePath().concat(subOutputDirectory));
		// raz outputDirectory
		FileUtils.cleanDirectory(outputDirectory);

		Resource resource = new Resource();
		Set<String> includes = new HashSet<String>();
		includes.add("**/foo.txt");
		resource.setIncludes(includes);

		FileService.copyFilesIntoOutputDirectory(copyResourcesMojo, gitSandboxDirectory, outputDirectory, resource);

		Set<String> sourceFiles = FileService.findFiles(gitSandboxDirectory);
		sourceFiles = IncludeService.process(includes, sourceFiles);
		Set<String> destinationFiles = FileService.findFiles(outputDirectory);

		Assert.assertEquals(2, destinationFiles.size());
		FileUtils.cleanDirectory(outputDirectory);
	}

	@AfterClass
	public static void clean_test() throws IOException, NoWorkTreeException, GitAPIException {
		// clean local git repo
		FileUtils.cleanDirectory(gitSandboxDirectory);
		Assert.assertNull(git.getRepository().getRef("HEAD"));
	}
}
