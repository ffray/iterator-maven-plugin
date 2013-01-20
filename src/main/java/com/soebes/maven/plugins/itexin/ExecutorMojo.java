package com.soebes.maven.plugins.itexin;


import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.twdata.maven.mojoexecutor.MojoExecutor;
import org.twdata.maven.mojoexecutor.PlexusConfigurationUtils;

/**
 * Executor will execute a given plugin by iterating throught the given items.
 * 
 * @author Karl-Heinz Marbaise <a href="mailto:kama@soebes.de">kama@soebes.de</a>
 *
 */
@Mojo(name = "executor", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true)
public class ExecutorMojo extends AbstractItExInMojo {

    /**
     * The plugin to be executed.
     * <pre>{@code
     * <plugin>
     *   <groupId>..</groupId>
     *   <artifactId>..</artifactId>
     *   <version>..</version>
     * </plugin>
     * }</pre>
     */
    @Parameter(required = true)
    private Plugin plugin;

    /**
     * The plugin goal to be executed.
     * 
     */
    @Parameter(required = true)
    private String goal;

    /**
     * Plugin configuration to use in the execution.
     * <pre>{@code
     * <configuration>
     *   Plugin Configuration
     * </configuration>
     * }</pre>
     */
    @Parameter
    private XmlPlexusConfiguration configuration;

    /**
     * The project currently being build.
     * 
     */
    @Parameter(required = true, readonly = true, defaultValue = "${project}")
    private MavenProject mavenProject;

    /**
     * The current Maven session.
     * 
     */
    @Parameter(required = true, readonly = true, defaultValue = "${session}")
    private MavenSession mavenSession;

    /**
     * The Maven BuildPluginManager component.
     * 
     */
    @Component
    private BuildPluginManager pluginManager;

    @Component
    // for Maven 3 only
    private PluginDescriptor pluginDescriptor;

    /**
     * This will copy the configuration from src to the result whereas the
     * placeholder will be replaced with the current value.
     * 
     */
    private PlexusConfiguration copyConfiguration(PlexusConfiguration src, String iteratorName, String value) {

	XmlPlexusConfiguration dom = new XmlPlexusConfiguration(src.getName());

	if (src.getValue() != null) {
	    if (src.getValue().contains(iteratorName)) {
		dom.setValue(src.getValue().replaceAll(iteratorName, value));
	    } else {
		dom.setValue(src.getValue());
	    }
	} else {
	    dom.setValue(src.getValue(null));
	}

	for (String attributeName : src.getAttributeNames()) {
	    dom.setAttribute(attributeName, src.getAttribute(attributeName, null));
	}

	for (PlexusConfiguration child : src.getChildren()) {
	    dom.addChild(copyConfiguration(child, iteratorName, value));
	}

	return dom;
    }

    public void execute() throws MojoExecutionException {
	if (isItemsNull() && isContentNull()) {
	    throw new MojoExecutionException("You have to use at least one. Either items element or content element!");
	}

	if (isItemsSet() && isContentSet()) {
	    throw new MojoExecutionException("You can use only one element. Either items element or content element but not both!");
	}

	for (String item : getItems()) {
	    getLog().debug("Configuration(before): " + configuration.toString());
	    PlexusConfiguration plexusConfiguration = copyConfiguration(configuration, getBeginToken() + getIteratorName() + getEndToken(), item);
	    getLog().debug("plexusConfiguration(after): " + plexusConfiguration.toString());
	    
	    StringBuilder sb = new StringBuilder("]] ");
	    // --- maven-jar-plugin:2.3.2:jar (default-jar) @ basic-test ---
	    sb.append(plugin.getKey());
	    sb.append(":");
	    sb.append(plugin.getVersion());
	    
	    getLog().info(sb.toString());
	    
	    // Put the value of the current iteration into the properties
	    mavenProject.getProperties().put(getIteratorName(), item);
	    
	    MojoExecutor.executeMojo(plugin, goal, PlexusConfigurationUtils.toXpp3Dom(plexusConfiguration),
		    MojoExecutor.executionEnvironment(mavenProject, mavenSession, pluginManager));
	    
	}
    }

    public MavenProject getMavenProject() {
        return mavenProject;
    }

    public void setMavenProject(MavenProject mavenProject) {
        this.mavenProject = mavenProject;
    }

    public MavenSession getMavenSession() {
        return mavenSession;
    }

    public void setMavenSession(MavenSession mavenSession) {
        this.mavenSession = mavenSession;
    }
    
}