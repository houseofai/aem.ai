package aem.ai.core.auth;

import java.util.Arrays;
import java.util.Random;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;

import org.apache.commons.codec.binary.Base32;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final public class SecurityUtils {

	private final static Logger log = LoggerFactory.getLogger(SecurityUtils.class);

	public static String updateSecurityKey(Authorizable userId, Session adminSession) throws RepositoryException {
		String key = null;

		try {
			ValueFactory vf = adminSession.getValueFactory();
			String userPath = userId.getPath();
			String userProfilePath = userPath + "/profile";
			key = createSecretKey();

			Value val = vf.createValue(key);
			if (adminSession.itemExists(userProfilePath)) {
				Node profile = adminSession.getNode(userProfilePath);
				profile.setProperty("secretKey", val);
				adminSession.save();

			} else {
				Node user = adminSession.getNode(userPath);
				Node profile = user.addNode("profile", "nt:unstructured");
				profile.setProperty("secretKey", val);
				adminSession.save();
			}

		} catch (Exception e) {
			log.error("[Exception] while creating security key for user: {}", userId, e);

		} finally {
			if (adminSession != null) {
				adminSession.logout();
			}

		}
		return key;
	}

	private static String createSecretKey() {
		byte[] buffer = new byte[30];
		new Random().nextBytes(buffer);

		byte[] secretKey = Arrays.copyOf(buffer, 10);
		String generatedKey = new Base32().encodeToString(secretKey);
		return generatedKey;
	}
}
