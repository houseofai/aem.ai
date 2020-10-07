package aem.ai.cg.kis.filter;

import java.nio.charset.StandardCharsets;

import javax.jcr.Session;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.Asset;

import aem.ai.cg.kis.services.KISCredentialsService;

@Component(service = WorkflowProcess.class, property = { "process.label=Cap Gemini KIS - Natural Language Search" })
public class SearchWorkflow implements WorkflowProcess {
	

	private Logger log = LoggerFactory.getLogger(this.getClass());

	@Reference
	private KISCredentialsService kisCredentialsService;
	
	private CloseableHttpClient client = HttpClientBuilder.create().build();

	
	@Override
	public void execute(WorkItem item, WorkflowSession wkfSsession, MetaDataMap args) throws WorkflowException {
		
		ResourceResolver resourceResolver = wkfSsession.adaptTo(ResourceResolver.class);
		Session session = resourceResolver.adaptTo(Session.class);

		String path = item.getWorkflowData().getPayload().toString();
		
		Resource resource = resourceResolver.getResource(path);
		Asset asset = resource.adaptTo(Asset.class);


		log.info("Getting the Asset: "+asset.getPath());
		
		String url = kisCredentialsService.getServerURL();
		
		HttpPost post = new HttpPost(url);

		try {
			JSONObject params = new JSONObject();
			params.put("query_text", "malnourished children in Chad");
			params.put("loginId", "cpatil13");
			
			post.setEntity(new StringEntity(params.toString(), StandardCharsets.UTF_8));
	
			HttpResponse response = client.execute(post);
			
			HttpEntity entity = response.getEntity();
			
			String content = EntityUtils.toString(entity);
			JSONObject json_response = new JSONObject(content);
			JSONObject doc_result = json_response.getJSONObject("doc_result");
			JSONObject doc_hits = doc_result.getJSONObject("hits");
			JSONArray hits = doc_hits.getJSONArray("hits");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

	}
}