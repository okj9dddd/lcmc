package lcmc.gui.resources.drbd;

import java.util.Arrays;
import java.util.LinkedHashSet;
import lcmc.data.Host;
import lcmc.gui.ClusterBrowser;
import lcmc.testutils.TestUtils;
import lcmc.testutils.annotation.type.IntegrationTest;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(IntegrationTest.class)
public final class ResourceInfoITest {

    private final TestUtils testSuite = new TestUtils();

    @Before
    public void setUp() {
        testSuite.initTestCluster();
    }

    @Test
    public void testNotEqualNames() {

        final ClusterBrowser b =
                testSuite.getHosts().get(0).getBrowser().getClusterBrowser();
        final ResourceInfo r1 = new ResourceInfo("name1", null, b);
        final ResourceInfo r2 = new ResourceInfo("name2", null, b);
        assertFalse("not equal names", r1.equals(r2));
    }

    @Test
    public void testEqualNames() {

        final ClusterBrowser b =
                testSuite.getHosts().get(0).getBrowser().getClusterBrowser();
        final ResourceInfo r1 = new ResourceInfo("name", null, b);
        final ResourceInfo r2 = new ResourceInfo("name", null, b);
        assertTrue("equal names", r1.equals(r2));
    }

    @Test
    public void testNameNull() {

        final ClusterBrowser b =
                testSuite.getHosts().get(0).getBrowser().getClusterBrowser();
        final ResourceInfo r1 = new ResourceInfo("name", null, b);
        assertFalse("equal name null", r1.getName() == null);
    }

    @Test
    public void testEqualNamesNotEqualsHosts() {

        final ClusterBrowser b =
                testSuite.getHosts().get(0).getBrowser().getClusterBrowser();
        
        final ResourceInfo r1 =
                new ResourceInfo("name", new LinkedHashSet<Host>(
                                                 Arrays.asList(new Host())), b);
        final ResourceInfo r2 = new ResourceInfo("name", null, b);
        assertTrue("equal names", r1.equals(r2));
    }
}