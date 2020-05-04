package org.odyssee.aem.ai.core.auth;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Face ID Authentication Handler Interface", description = "Face ID Authentication Handler Interface Configuration")
public @interface FaceIDAuthHandler {

	@AttributeDefinition(name = "Path", description = "Repository path for which this authentication handler should be used by Sling. If this is empty, the authentication handler will be disabled. (path)")
	String pathValue() default "/";

	@AttributeDefinition(name = "Default Login Page", description = "If no mappings are defined, nor no mapping matches the request, this is the default login page being redirected to. This can be overridden in the content page configuration")
	String loginPageValue() default "/libs/granite/core/content/login.html";
}
