package org.odyssee.aem.ai.core.auth;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.jcr.AccessDeniedException;
import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base32;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.auth.Authenticator;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.auth.core.AuthUtil;
import org.apache.sling.auth.core.spi.AuthenticationFeedbackHandler;
import org.apache.sling.auth.core.spi.AuthenticationHandler;
import org.apache.sling.auth.core.spi.AuthenticationInfo;
import org.apache.sling.auth.core.spi.DefaultAuthenticationFeedbackHandler;
import org.apache.sling.jcr.api.SlingRepository;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.propertytypes.ServiceDescription;
import org.osgi.service.component.propertytypes.ServiceRanking;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.crx.security.token.TokenUtil;

@Component(service = AuthenticationHandler.class, immediate = true, property = { "path=/489" })
@ServiceRanking(60000)
@ServiceDescription("Face ID Authentication Handler")
@Designate(ocd = FaceIDAuthHandler.class)
public class FaceIDAuthenticationHandler extends DefaultAuthenticationFeedbackHandler
implements AuthenticationHandler, AuthenticationFeedbackHandler {

	 
	    private static final String REQUEST_METHOD = "POST";
	    private static final String USER_NAME = "j_username";
	    private static final String PASSWORD = "j_password";
	    private static final String OTPCODE = "j_otpcode";
	    private static final String SECRET_KEY = "secretKey";
	    private static boolean isNewKey = false;
	    private static final String HMAC_HASH_FUNCTION = "HmacSHA1";
	    private static long timeStepSizeInMillis = TimeUnit.SECONDS.toMillis(30L);
	    private static int keyModulus = (int) Math.pow(10.0D, 6.0D);
	    static final String REQUEST_URL_SUFFIX = "/j_security_check";
	    private static final String PAR_LOOP_PROTECT = "$$login$$";
	 
	    @Reference
	    private ResourceResolverFactory resourceResolverFactory;
	 
	    @Activate
	    private FaceIDAuthHandler config;
	 
	    @Reference
	    private SlingRepository repository;
	 
	    private final Logger log = LoggerFactory.getLogger(FaceIDAuthenticationHandler.class);
	 
	    public AuthenticationInfo extractCredentials(HttpServletRequest request, HttpServletResponse response) {
	        log.info("OTPBasedAuthenticationHandler : Inside extractCredentials {}", request.getRequestURI());
	 
	        if ((REQUEST_METHOD.equals(request.getMethod())) && (request.getRequestURI().endsWith(REQUEST_URL_SUFFIX))
	                && (request.getParameter(USER_NAME) != null)) {
	            try {
	                SimpleCredentials creds = new SimpleCredentials(request.getParameter(USER_NAME),
	                        request.getParameter(PASSWORD).toCharArray());
	                Session session = this.repository.login(creds);
	 
	                if (session != null) {
	                    boolean is2StepAuthEnabled = check2StepAuthPreference(request.getParameter("j_username"), session);
	 
	                    // Authenticate with OTP
	                    if (is2StepAuthEnabled) {
	                        return twoFactorAuthentication(is2StepAuthEnabled, request, response, session);
	 
	                    } else {
	                        return createAuthenticationInfo(request, response, request.getParameter("j_username"));
	 
	                    }
	                }
	            }
	 
	            catch (LoginException e) {
	                log.error("[LoginException] in extractCredentials while processing the request {}", e);
	 
	            } catch (RepositoryException e) {
	                log.error("[RepositoryException] in extractCredentials while processing the request {}", e);
	 
	            } catch (Exception e) {
	                log.error("[RepositoryException] in extractCredentials while processing the request {}", e);
	 
	            }
	        }
	        return null;
	    }
	 
	    private boolean check2StepAuthPreference(String userId, Session session1)
	            throws AccessDeniedException, RepositoryException, LoginException {
	        log.info("OTPBasedAuthenticationHandler : Inside check2StepAuthPreference ");
	        Session adminSession = null;
	        boolean is2StepEnabled = false;
	        try {
	            adminSession = this.resourceResolverFactory.getAdministrativeResourceResolver(null).adaptTo(Session.class);
	 
	            UserManager um = ((JackrabbitSession) session1).getUserManager();
	            org.apache.jackrabbit.api.security.user.Authorizable authorizable = um.getAuthorizable(userId);
	 
	            if (adminSession.itemExists(authorizable.getPath() + "/preferences")) {
	                Node pref = adminSession.getNode(authorizable.getPath() + "/preferences");
	                if (pref.hasProperty("twostep")) {
	                    Property references = pref.getProperty("twostep");
	                    String value = references.getValue().getString();
	                    if ((value != null) && (value.equals("yes"))) {
	                        is2StepEnabled = true;
	                    } else {
	                        is2StepEnabled = false;
	                    }
	                }
	            }
	        } catch (org.apache.sling.api.resource.LoginException e) {
	            log.error("Error while checking user 2 factor authentication preference, logging out user session {}", e);
	            throw new LoginException("failed to retrieve user preference, logging out user session");
	 
	        } catch (NullPointerException e) {
	            throw new NullPointerException("failed to retrieve user preference, logging out user session");
	 
	        } finally {
	            if (adminSession != null) {
	                adminSession.logout();
	            }
	 
	        }
	        return is2StepEnabled;
	 
	    }
	 
	    private AuthenticationInfo createAuthenticationInfo(HttpServletRequest request, HttpServletResponse response,
	            String userId) throws RepositoryException {
	        log.info("OTPBasedAuthenticationHandler : Inside createAuthenticationInfo ");
	        AuthenticationInfo authinfo = TokenUtil.createCredentials(request, response, this.repository, userId, true);
	 
	        return authinfo;
	    }
	 
	    public void dropCredentials(HttpServletRequest arg0, HttpServletResponse arg1) throws IOException {
	 
	    }
	 
	    public boolean requestCredentials(HttpServletRequest request, HttpServletResponse arg1) throws IOException {
	        // use a linked hash map to guarantee order of parameters
	        log.info("OTPBasedAuthenticationHandler : INside CREDS ");
	        String reason = getReason(request, FAILURE_REASON);
	        String reasonCode = getReason(request, FAILURE_REASON_CODE);
	 
	        try {
	            String path = rewrite(request, this.resourceResolverFactory.getResourceResolver(null),
	                    config.loginPageValue());
	            LinkedHashMap<String, String> params = new LinkedHashMap<String, String>();
	            params.put(Authenticator.LOGIN_RESOURCE, path);
	            params.put(PAR_LOOP_PROTECT, PAR_LOOP_PROTECT);
	 
	            // append indication of previous login failure
	            params.put(FAILURE_REASON, reason);
	            params.put(FAILURE_REASON_CODE, reasonCode);
	            AuthUtil.sendRedirect(request, arg1, path, params);
	 
	        } catch (IOException e) {
	            log.error("[IOException] Failed to redirect to the login / change password form [{}]", e);
	 
	        } catch (org.apache.sling.api.resource.LoginException e) {
	            log.error("[LoginException] Failed to redirect to the login / change password form [{}]", e);
	 
	        }
	 
	        return true;
	    }
	 
	    private String checkOrCreateSecurityKey(String userId, Session session)
	            throws AccessDeniedException, UnsupportedRepositoryOperationException, RepositoryException {
	        log.info("OTPBasedAuthenticationHandler : INside checkOrCreateSecurityKey ");
	        Session adminSession;
	        Authorizable authorizable = null;
	        String key = null;
	        try {
	            adminSession = this.resourceResolverFactory.getAdministrativeResourceResolver(null).adaptTo(Session.class);
	            UserManager um = ((JackrabbitSession) adminSession).getUserManager();
	            authorizable = um.getAuthorizable(userId);
	            String profilePath = authorizable.getPath() + "/profile";
	            Node node = adminSession.getNode(profilePath);
	            if (node.hasProperty("secretKey")) {
	                Property references = node.getProperty("secretKey");
	                String secretKey = references.getValue().getString();
	                if ((secretKey != null) && (secretKey.length() > 0)) {
	                    key = secretKey;
	                }
	 
	            } else {
	                key = SecurityUtils.updateSecurityKey(authorizable, session);
	            }
	 
	        } catch (Exception e) {
	            log.error("Error while retrieving or creating new security key {}", e);
	        }
	 
	        return key;
	    }
	 
	    /*----------- Private Uility Methods below -------------*/
	 
	    // Attemp to authenticate user with OTP
	    private AuthenticationInfo twoFactorAuthentication(boolean is2StepAuthEnabled, HttpServletRequest request,
	            HttpServletResponse response, Session session) throws RepositoryException {
	 
	        String needNewKey = checkOrCreateSecurityKey(request.getParameter("j_username"), session);
	        if ((needNewKey != null) && (needNewKey.length() > 0)) {
	            // 2FA enabled but no OTP provided by user, logout the session.
	            if (request.getParameter("j_otpcode").length() <= 0) {
	                log.info("2FA enabled but no OTP provided by user: {}, logout the session.",
	                        request.getParameter(USER_NAME));
	                request.setAttribute("j_reason", "invalid_otp");
	                return AuthenticationInfo.FAIL_AUTH;
	            }
	 
	            // Check if current OTP matches with users last used token in same browser
	            boolean isSameCode = getCookie(request);
	 
	            if (!isSameCode) {
	                if (checkCode(response, needNewKey, Long.parseLong(request.getParameter("j_otpcode")),
	                        new Date().getTime(), 23)) {
	                    return createAuthenticationInfo(request, response, request.getParameter("j_username"));
	                }
	                request.setAttribute("j_reason", "invalid_otp");
	                return AuthenticationInfo.FAIL_AUTH;
	            }
	 
	            log.info("Last used OTP is similar current OTP, logout the session.", request.getParameter(USER_NAME));
	            request.setAttribute("j_reason", "invalid_otp");
	            session.logout();
	            return AuthenticationInfo.FAIL_AUTH;
	        }
	        return null;
	 
	    }
	 
	    private String rewrite(final HttpServletRequest request, final ResourceResolver resolver, final String path) {
	        return StringUtils.endsWith(path, ".html") ? path : path.concat(".html");
	    }
	 
	    private static String getReason(HttpServletRequest request, String parameter) {
	        Object reason = request.getAttribute(parameter);
	        if (null == reason) {
	            reason = request.getParameter(parameter);
	        }
	        if (null == reason) {
	            reason = FAILURE_REASON_CODES.UNKNOWN;
	        }
	        return (reason instanceof Enum) ? ((Enum) reason).name().toLowerCase() : reason.toString();
	    }
	 
	    // Verify the user token with set of tokens in given window.
	    private boolean checkCode(HttpServletResponse response, String secret, long code, long timestamp, int window) {
	        log.info("OTPBasedAuthenticationHandler : INside checkCode ");
	        Base32 codec32 = new Base32();
	        byte[] decodedKey = codec32.decode(secret);
	 
	        long timeWindow = timestamp / timeStepSizeInMillis;
	 
	        for (int i = -((window - 1) / 2); i <= window / 2; i++) {
	            long hash;
	            try {
	                hash = verify_code(decodedKey, timeWindow + i);
	                log.info("HASH {}, code {}", hash, code);
	                if (hash == code) {
	                    createCookie(response, code);
	                    return true;
	                }
	            } catch (InvalidKeyException e) {
	                log.error("[InvalidKeyException] while checking security code for user {}", e);
	            } catch (NoSuchAlgorithmException e) {
	                log.error("[NoSuchAlgorithmException] while checking security code for user {}", e);
	            }
	 
	        }
	        return false;
	    }
	 
	    /*
	     * Create a cooke with current valid token in browser after successfulle login.
	     * Every login will be compared with last used key and will be denied if those
	     * are same.
	     */
	    private void createCookie(HttpServletResponse response, long code) {
	        log.info("OTPBasedAuthenticationHandler : INside createCookie ");
	        Cookie cookie = new Cookie("validtoken", String.valueOf(code));
	        cookie.setMaxAge(600);
	        cookie.setPath("/");
	        response.addCookie(cookie);
	    }
	 
	    private static int verify_code(byte[] key, long t) throws NoSuchAlgorithmException, InvalidKeyException {
	        byte[] data = new byte[8];
	        long value = t;
	        for (int i = 8; i-- > 0; value >>>= 8) {
	            data[i] = (byte) value;
	        }
	 
	        SecretKeySpec signKey = new SecretKeySpec(key, "HmacSHA1");
	        Mac mac = Mac.getInstance("HmacSHA1");
	        mac.init(signKey);
	        byte[] hash = mac.doFinal(data);
	 
	        int offset = hash[20 - 1] & 0xF;
	 
	        // We're using a long because Java hasn't got unsigned int.
	        long truncatedHash = 0;
	        for (int i = 0; i < 4; ++i) {
	            truncatedHash <<= 8;
	            truncatedHash |= (hash[offset + i] & 0xFF);
	        }
	 
	        truncatedHash &= 0x7FFFFFFF;
	        truncatedHash %= 1000000;
	 
	        return (int) truncatedHash;
	    }
	 
	    private boolean getCookie(HttpServletRequest request) {
	        boolean foundCookie = false;
	        if (request.getCookies() != null) {
	            Cookie[] cookies = request.getCookies();
	            String otp = request.getParameter("j_otpcode");
	            for (int i = 0; i < cookies.length; i++) {
	                Cookie cookie1 = cookies[i];
	                if ((cookie1.getName().equals("validtoken")) && (cookie1.getValue().equals(otp))) {
	                    foundCookie = true;
	                }
	            }
	 
	        }
	        return foundCookie;
	 
	    }
	 
	    protected void bindRepository(SlingRepository paramSlingRepository) {
	        this.repository = paramSlingRepository;
	    }
	 
	    protected void unbindRepository(SlingRepository paramSlingRepository) {
	        if (this.repository == paramSlingRepository) {
	            this.repository = null;
	        }
	    }
	 
	}