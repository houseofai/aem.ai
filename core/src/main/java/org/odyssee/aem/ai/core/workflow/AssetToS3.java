package org.odyssee.aem.ai.core.workflow;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.Asset;

@Component(service = WorkflowProcess.class, property = { "process.label=Asset To Aws S3" })
public class AssetToS3 implements WorkflowProcess {

    private static final Logger log = LoggerFactory.getLogger(AssetToS3.class);

    
    @Override
    public final void execute(WorkItem item, WorkflowSession session, MetaDataMap args) throws WorkflowException {


		String path = item.getWorkflowData().getPayload().toString();
		log.info("payload: " + path);
		ResourceResolver resourceResolver = session.adaptTo(ResourceResolver.class);
		//AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);
		
		Resource resource = resourceResolver.getResource(path);
		Asset asset = resource.adaptTo(Asset.class);
		
//		try {
//			s3.addAssets(asset);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}

    }
}
