package com.palantir.gerrit.gerritci.providers;

import java.io.StringWriter;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.palantir.gerrit.gerritci.constants.JobType;

public class VelocityProviderTest {

    private VelocityProvider provider;
    private VelocityEngine engine;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        provider = new VelocityProvider();

        engine = new VelocityEngine();
        engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
        engine.setProperty("class.resource.loader.class", ClasspathResourceLoader.class.getName());
        engine.init();
    }

    @Test
    public void testFindNonexistentTemplate() {
        thrown.expect(ResourceNotFoundException.class);
        engine.getTemplate("/foo-bar.vm");
    }

    @Test
    public void testFindTemplates() {
        Template verifyTemplate = engine.getTemplate("templates" + JobType.VERIFY.getTemplate());
        Assert.assertNotNull(verifyTemplate);
        Assert.assertTrue(verifyTemplate.getData().toString().contains(
            "<description>Verify build created by Gerrit-CI plugin"));

        Template publishTemplate = engine.getTemplate("templates" + JobType.PUBLISH.getTemplate());
        Assert.assertNotNull(publishTemplate);
        Assert.assertTrue(publishTemplate.getData().toString().contains(
            "<description>Publish build created by Gerrit-CI plugin"));
    }

    @Test
    public void testVelocityContext() {
        VelocityContext context = provider.getVelocityContext();
        Assert.assertEquals(0, context.getKeys().length);
        Assert.assertFalse(context == provider.getVelocityContext());

        context.put("A-Major", "nifty");
        Assert.assertTrue(context.containsKey("A-Major"));

        StringWriter incogni = new StringWriter();
        engine.evaluate(context, incogni, "TEST", "Disguise: $A-Major");
        Assert.assertEquals("Disguise: nifty", incogni.toString());

        context = provider.getVelocityContext();
        Assert.assertNotNull(context);
        Assert.assertEquals(0, context.getKeys().length);
    }
}
