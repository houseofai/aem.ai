/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package aem.ai.cg.kis.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.engine.EngineConstants;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.component.propertytypes.ServiceVendor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aem.ai.cg.kis.services.KISCredentialsService;

/**
 * Simple servlet filter component that logs incoming requests.
 */
@Component(service = Filter.class,
           property = {
                   EngineConstants.SLING_FILTER_SCOPE + "=" + EngineConstants.FILTER_SCOPE_REQUEST,
           })
@ServiceDescription("Demo to filter incoming requests")
@ServiceRanking(-700)
@ServiceVendor("Adobe")
public class SearchFilter implements Filter {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
	@Reference
	private KISCredentialsService kisCredentialsService;
	
	private CloseableHttpClient client = HttpClientBuilder.create().build();

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response,
                         final FilterChain filterChain) throws IOException, ServletException {

        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        log.error("request for {}, with selector {}", slingRequest
                .getRequestPathInfo().getResourcePath(), slingRequest
                .getRequestPathInfo().getSelectorString());

		
		String url = kisCredentialsService.getServerURL();
		
		HttpPost post = new HttpPost(url);

		try {
			JSONObject params = new JSONObject();
			params.put("query_text", "malnourished children in Chad");
			params.put("loginId", "cpatil13");
			
			post.setEntity(new StringEntity(params.toString(), StandardCharsets.UTF_8));
	
			HttpResponse kis_response = client.execute(post);
			
			HttpEntity entity = kis_response.getEntity();
			
			String content = EntityUtils.toString(entity);
			JSONObject json_response = new JSONObject(content);
			JSONObject doc_result = json_response.getJSONObject("doc_result");
			JSONObject doc_hits = doc_result.getJSONObject("hits");
			JSONArray hits = doc_hits.getJSONArray("hits");
			
			// TODO Add to "Suggestions"
			//response.
			/**{
				"suggestions": [
					{
						"value": "Woman and man runners training jogging outdoors in mountain nature landscape on snaefellsnes\\, iceland",
						"suggestion": "Woman and man runners training jogging outdoors in mountain nature landscape on snaefellsnes, iceland"
					},
					{
						"value": "Running desert woman",
						"suggestion": "Running desert woman"
					},
					{
						"value": "Running woods woman",
						"suggestion": "Running woods woman"
					},
					{
						"value": "Climber woman wearing in safety harness with equipment holding rope and preparing to climb",
						"suggestion": "Climber woman wearing in safety harness with equipment holding rope and preparing to climb"
					},
					{
						"value": "Fitness woman",
						"suggestion": "Fitness woman"
					},
					{
						"value": "Woman winter and autumn running in down jacket on mountain trail",
						"suggestion": "Woman winter and autumn running in down jacket on mountain trail"
					},
					{
						"value": "Woman running in wooded forest area\\, training and exercising for trail run marathon endurance",
						"suggestion": "Woman running in wooded forest area, training and exercising for trail run marathon endurance"
					},
					{
						"value": "Healthy lifestyle fitness sporty woman running early in the morning in forest area",
						"suggestion": "Healthy lifestyle fitness sporty woman running early in the morning in forest area"
					}
				]
			}**/
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        
        filterChain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }

}