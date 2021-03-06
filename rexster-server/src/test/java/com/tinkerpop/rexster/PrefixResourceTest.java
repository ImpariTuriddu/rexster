package com.tinkerpop.rexster;

import com.tinkerpop.rexster.server.RexsterApplication;
import com.tinkerpop.blueprints.impls.sail.SailGraph;
import com.tinkerpop.blueprints.impls.sail.SailGraphFactory;
import com.tinkerpop.blueprints.impls.sail.impls.MemoryStoreSailGraph;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.HashMap;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * @author Stephen Mallette (http://stephen.genoprime.com)
 */
public class PrefixResourceTest extends BaseTest {
    @Test
    public void getPrefixesValid() {

        final ResourceHolder<PrefixResource> holder = this.constructPrefixResource();
        final Response response = holder.getResource().getPrefixes(graphName);

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final JSONObject jsonObject = (JSONObject) response.getEntity();
        Assert.assertNotNull(jsonObject);

        Assert.assertTrue(jsonObject.has(Tokens.RESULTS));
        final JSONArray results = jsonObject.optJSONArray(Tokens.RESULTS);
        Assert.assertNotNull(results);
        Assert.assertEquals(1, results.length());

        Assert.assertTrue(jsonObject.has(Tokens.QUERY_TIME));
    }

    @Test
    public void getSinglePrefixValid() {
        final ResourceHolder<PrefixResource> holder = this.constructPrefixResource();
        final PrefixResource resource = holder.getResource();
        final Response response = resource.getSinglePrefix(graphName, "tg");

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final JSONObject jsonObject = (JSONObject) response.getEntity();
        Assert.assertNotNull(jsonObject);

        Assert.assertTrue(jsonObject.has(Tokens.RESULTS));
        Assert.assertEquals("http://tinkerpop.com#", jsonObject.optString(Tokens.RESULTS));

        Assert.assertTrue(jsonObject.has(Tokens.QUERY_TIME));
    }

    @Test
    public void deleteSinglePrefixValid() {
        final ResourceHolder<PrefixResource> holder = this.constructPrefixResource();
        final PrefixResource resource = holder.getResource();
        Response response = resource.deleteSinglePrefix(graphName, "tg");

        Assert.assertNotNull(response);
        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final JSONObject jsonObject = (JSONObject) response.getEntity();
        Assert.assertNotNull(jsonObject);

        Assert.assertTrue(jsonObject.has(Tokens.QUERY_TIME));
    }
}
