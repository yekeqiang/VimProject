package com.stubhub.security.authentication;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import com.stubhub.common.AuthenticatedSession;
import com.stubhub.common.business.util.DomainUtils;
import com.stubhub.common.cache.manager.EntityCacheManager;
import com.stubhub.common.cache.store.memcached.MemcachedStore;
import com.stubhub.common.config.RequestContext;
import com.stubhub.common.config.StubhubCredentials;
import com.stubhub.common.exceptions.base.MissingRequiredArgumentException;
import com.stubhub.common.exceptions.base.RecordNotFoundForIdException;
import com.stubhub.common.exceptions.base.StubHubBizException;
import com.stubhub.common.exceptions.base.StubHubSystemException;
import com.stubhub.common.exceptions.derived.FraudDeactivatedUserException;
import com.stubhub.common.util.Constants;
import com.stubhub.common.util.StringUtils;
import com.stubhub.common.util.StubHubProperties;
import com.stubhub.common.util.StubhubCookieEncryptor;
import com.stubhub.common.util.UserSessionUtil;
import com.stubhub.common.webservice.WebServiceUtil;
import com.stubhub.ui.common.util.CookieConstant;
import com.stubhub.user.business.entity.Agreement;
import com.stubhub.user.business.entity.UserContact;
import com.stubhub.user.business.entity.UserSession;
import com.stubhub.user.business.facade.UserContactsFacade;
import com.stubhub.user.business.facade.UserFacade;
import com.stubhub.user.business.facade.UserSessionFacade;

/**
 * This filter creates a user session (in SESSIONS table), (if the user is
 * authenticated) and sets the SUB_SECR cookie in the response
 * 
 * @author sganapathy
 * 
 */

@Component("stubhubUserSessionFilter")
public class StubhubUserSessionFilter extends GenericFilterBean {
	
	private final static Logger log = Logger.getLogger(StubhubUserSessionFilter.class);
	private MemcachedStore store = new MemcachedStore();

	@Autowired
	private EntityCacheManager entityCacheManager;
	@Autowired
	private UserSessionFacade userSessionFacade;
	@Autowired
	private UserFacade userFacade;	
	@Autowired
	private UserContactsFacade userContactsFacade;
	@Autowired
	private StubHubSecurityValidator securityValidator;
	
	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain filterChain) throws IOException, ServletException {

		HttpServletRequest request = null;
		HttpServletResponse response = null;
		String username = "";
		String password = "";
		String isTempSession = "";
		String ipAddress = "";
		String httpAccept = "";
		String referer = "";
		StringBuffer failedLoginIpAttemptKey = new StringBuffer("failedloginip:");
		StringBuffer failedLoginAttemptKey = new StringBuffer("failedlogin:");
		RequestContext requestContext = StubhubCredentials.getRequestContext();
		AuthenticatedSession session = null;
		UserContact userContact = null;
		UserSession lastSession = null;
		UserSession userSession = null;
		String sessionGuid = "";
		
		try {

			if (log.isDebugEnabled()) {
				log.debug("StubhubUserSessionFilter.doFilter method ...START");
			}
			
			request = (HttpServletRequest) req;
			response = (HttpServletResponse) resp;
			
			String contentType = req.getContentType();
			Map<String, String> headers = null;
			if (req instanceof HttpServletRequest) {
				HttpServletRequest hReq = (HttpServletRequest) req;
				headers = new HashMap<String, String>();
				Enumeration hEnum = hReq.getHeaderNames();
				while (hEnum != null && hEnum.hasMoreElements()) {
					String k = (String) hEnum.nextElement();
					headers.put(k, hReq.getHeader(k));
					if (log.isDebugEnabled()) {
						log.debug("Header name,value:" + k + "," + hReq.getHeader(k));
					}									
				}
			}
			if (WebServiceUtil.isSoap(contentType, headers)) {
				if (log.isDebugEnabled()) {
					log.debug("StubhubUserSessionFilter found a request that looks like SOAP, so we're skipping the filter. "
								+ "content-type="+ contentType+ "; header="+ headers);
				}
				filterChain.doFilter(req, resp);
				return;
			} else {
			
				if (!"POST".equalsIgnoreCase(request.getMethod())) {
					if (log.isDebugEnabled()) {
						log.debug("Only POST requests are accepted");
					}
					response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
					return;			
				}
		
				httpAccept = request.getHeader("Accept");
				if (!(StringUtils.containsIgnoreCase(httpAccept, "application/xml") || StringUtils.containsIgnoreCase(httpAccept, "application/json"))) {
					if (log.isDebugEnabled()) {
						log.debug("Only Accept:application/xml or application/json requests are accepted");
					}
					response.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return;							
				}
				
				isTempSession = "true";
				referer = request.getHeader("referer");
				if (StringUtils.containsIgnoreCase(referer, "checkout/mobile/Signin")) {
					if (log.isDebugEnabled())
						log.debug("Request coming from Mobile, isTempSession=false");
					isTempSession = "false";
				}
				if (StringUtils.containsIgnoreCase(referer, "pro") && StringUtils.containsIgnoreCase(referer, "simweb/user/login")) {
					if (log.isDebugEnabled())
						log.debug("Request coming from Stubhub Pro, isTempSession=false");
					isTempSession = "false";
				}

				//PLATFORM-265: LoginAPI must reject POST requests with URL Params
				String queryString = request.getQueryString();
				if (StringUtils.contains(queryString, "username=") || StringUtils.contains(queryString, "password=")) {
					if (log.isDebugEnabled()) {
						log.debug("Username and/or password are sent by query string");
					}
					response.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return;				
				}				
				
				// Retrieve Request Body parameters
				username = request.getParameter("username");
				password = request.getParameter("password");
				// domain forwarded on from other Gen3 Page logins
				String forwardedDomain = request.getHeader("forwardedDomain");
	
				if (StringUtils.isBlank(username) || StringUtils.isBlank(password)) {
					if (log.isDebugEnabled()) {
						log.debug("Username and/or password missing in Body");
					}
					response.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return;				
				}
				
				if (StringUtils.isStringNullorEmpty(username) ) {
					// user input didn't pass security validation
					HttpServletResponse httpResponse = (HttpServletResponse) response;
					httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return;
				} else if(username.contains("@")){
					if(!validateEmailAddress(username)) {
						// user input didn't pass security validation
						HttpServletResponse httpResponse = (HttpServletResponse) response;
						httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
						return;
					}
				}
				
				if (!securityValidator.isValidPassword(username, password)) {
					// user input didn't pass security validation
					HttpServletResponse httpResponse = (HttpServletResponse) response;
					httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST);
					return;
				}
				
				// determine client IP address
				// If X-Forwarded-For in header use first IP, otherwise use java getRemoteAddr()
				Enumeration en = request.getHeaders("X-Forwarded-For");
				if (en != null && en.hasMoreElements()) {
					ipAddress = (String) en.nextElement();
					int commaPos = ipAddress.indexOf(','); 
					if (commaPos != -1)
						ipAddress = ipAddress.substring(0, commaPos);
				} else  {
					ipAddress = request.getRemoteAddr();
				}			
				log.info("Login Request=" + username + ",ipAddress=" + ipAddress);
		
				// Login attack checks
				failedLoginIpAttemptKey.append(username).append(":").append(ipAddress);
				Boolean loginAttempt = (Boolean) store.get(failedLoginIpAttemptKey.toString());
				if (loginAttempt != null) {				
					// current attempt within FIVE_SECOND_DELAY seconds of previous attempt, decline login
					Integer delaySeconds = StubHubProperties.getPropertyAsInt("loginapi.multiple.attempt.delay.seconds", 5);
					log.info("UserName="+username+" had login attempted within " + delaySeconds + " seconds by same IP="+ipAddress+" address combination");
					if (log.isDebugEnabled()) {
						log.debug("Failed Login attempt within 5 seconds by same user/IP address combination in Memcache=" + failedLoginIpAttemptKey.toString());
					}
					HttpServletResponse httpResponse = (HttpServletResponse) response;					
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "code#repeat");
					return;
				}
				
				failedLoginAttemptKey.append(username);
				Integer failedAttemptCount = (Integer) store.get(failedLoginAttemptKey.toString());
				Integer maxFailedAttempts = StubHubProperties.getPropertyAsInt("loginapi.maximum.failed.attempt.allowed", 6);
				
				if (failedAttemptCount != null && failedAttemptCount > maxFailedAttempts && !whiteListCheckPassed(request, response)) {				
					// login failure for more than 7 attempts within an hour
					log.info("UserName="+username+" had more than " + maxFailedAttempts + " consecutive failed login attempts and incurred into a 1 hour penalty");
					if (log.isDebugEnabled())
						log.debug("Found Failed Login attempt in Memcache=" + failedLoginAttemptKey.toString());
					HttpServletResponse httpResponse = (HttpServletResponse) response;
					httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "code#ipfail");					 
					return;
				}
				
				// start the authentication process
				requestContext.setSecure(request.isSecure());
				requestContext.setIp(ipAddress);
				requestContext.setServerName(request.getServerName());
				// Below default the cobrand to "www"/47
				requestContext.setCobrand(SecurityConstant.DEFUALT_COBRAND);
				requestContext.setCobrandId(SecurityConstant.DEFUALT_COBRAND_ID);
				if (forwardedDomain != null && !forwardedDomain.equals(""))
					requestContext.setDomainName(forwardedDomain);
				else
					requestContext.setDomainName(DomainUtils.getStubhubDomainFromUrl(request.getRequestURL().toString()));
				UserSessionUtil.parseAndPopulateBrowserFields(request.getHeader(HttpHeaders.USER_AGENT), requestContext);
				
				// authenticate and create Authenticated Session
				session = userSessionFacade.createAuthenticatedSessionWithUserLoginName(username, password);
				sessionGuid = session.getSessionId().getSessionGuid();
				// Fetch User Contact for first name, last name
				userContact = userContactsFacade.getDefaultContact(session.getUserGuid());
				// Fetch Last Logged in Session
				// change the way of fetching userId, JIRA : https://jira.stubcorp.dev/browse/BRS-1892
				Long userId = userFacade.getCachedUserIdByGuid(requestContext.getUserGuid());
				lastSession = userSessionFacade.getLastLoggedInSession(userId);
				
				if (isTempSession.equals("true")) {
					List<Agreement> unacceptedAgreements = userFacade.getUnacceptedAgreements(userId , 						
							com.stubhub.common.util.Constants.GENERIC_TERMS_AND_CONDITIONS,
							DomainUtils.getCurrentDomain().getDomainName());
	
					// there should be only one Unaccepted Agreement per userId, agreement type and domain
					if (unacceptedAgreements != null && !unacceptedAgreements.isEmpty()) {
						// User agreement not signed, so create a temporary session
						int timeout = Integer.valueOf(StubHubProperties.getProperty(
								"usersession.temporary.timeout", "7200"));
						//put temporary flag into memcache
						entityCacheManager.putCrossModuleValue(generateUserTempLoginFlagKey(session.getUserGuid().getGuid(), request), "temploginflag", Boolean.TRUE, timeout);
					} else {
						// user agreement already checked so this is not a Temp Session
						isTempSession = "false";
						// temp session not needed, remove any temploginflag set for the userGuid
						//remove temporary flag into memcache
						entityCacheManager.removeCrossModuleValue(generateUserTempLoginFlagKey(session.getUserGuid().getGuid(), request), "temploginflag");
					}
				} else {
					// temp session not needed, remove any temploginflag set for the userGuid
					//remove temporary flag into memcache
					entityCacheManager.removeCrossModuleValue(generateUserTempLoginFlagKey(session.getUserGuid().getGuid(), request), "temploginflag");					
				}
			}
			
		} catch (RecordNotFoundForIdException e) {
			recordFailedAttempt(username, ipAddress);
			log.error("User cannot be found" + e.getMessage());
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "code#notfound");
			return;
		} catch (MissingRequiredArgumentException e) {
			recordFailedAttempt(username, ipAddress);
			log.error("User or password missing" + e.getMessage());
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "code#misarg");
			return;
		} catch (StubHubBizException e) {
			recordFailedAttempt(username, ipAddress);
			log.error("Authentication failed", e);
			//any fraud deactivated user login attempt, page will be forwarded to user deactivation page 
			if (e instanceof FraudDeactivatedUserException)
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "code#fraud");
			else
				response.sendError(HttpServletResponse.SC_FORBIDDEN, "code#bizex"); 
			response.getOutputStream().flush();
						
			return;
		} catch (Exception e) {
			recordFailedAttempt(username, ipAddress);
			log.error("Authentication failed", e);
			response.sendError(HttpServletResponse.SC_FORBIDDEN, "code#ex");			
			return;
		}

		// login success, clear up all failed attempts
		log.info("Login Success="+ username + ",ipAddress=" + ipAddress);
		if (log.isDebugEnabled()) {
			log.debug("Removing all Prev Failed LoginIP attempts (if any) from Memcache=" + failedLoginIpAttemptKey.toString());
		}
		store.remove(failedLoginIpAttemptKey.toString());
		
		if (log.isDebugEnabled()) {
			log.debug("Removing all Prev Failed Login attempts (if any) from Memcache=" + failedLoginAttemptKey.toString());
		}
		store.remove(failedLoginAttemptKey.toString());
		
		// Construct response body and send it back			
		try {
			// send back Cookies
			String cookieKey = generateCookieKey(requestContext.getSessionId());
			// gen2 cookie STUB_SESSION's stub_sid token and 
			// gen3 cookie STUB_SESS's guid token and
			// gen3 cookie STUB_MYACT_INFO are populated by 
			// gen3 cookie STUB_INFO's guid token is populated by
			// gen3 cookie STUB_SECR is populated by 
			// UserSessionFacadeImpl.createAuthenticateSession from createAuthenticatedSessionWithUserLoginName()
			// ONLY set remaining tokens of gen2 cookie STUB_SESSION and gen3 cookie STUB_SESS here

			// TODO - are the following lines needed?
			/*requestContext.setStubSessionCookieTokenValue(CookieConstant.IP_ZIP_CODE, "", null);
			requestContext.setStubSessionCookieTokenValue(CookieConstant.USER_TYPE, "", null);
			requestContext.setStubSessionCookieTokenValue(CookieConstant.IP_GEOGRAPHY_ID, "", null);
			requestContext.setStubSessionCookieTokenValue(CookieConstant.U_TYPE, "", null);
			requestContext.setStubSessionCookieTokenValue(CookieConstant.IP_CITY, "", null);
			requestContext.setStubSessionCookieTokenValue(CookieConstant.ORDER_TO_CONFIRM, "", null);
			requestContext.setStubSessionCookieTokenValue(CookieConstant.ERROR_EVENT_ID, "", null);*/
			requestContext.setStubSessionCookieTokenValue(CookieConstant.LAST_NAME, 
					StubhubCookieEncryptor.encryptCookie(userContact.getLastName(), cookieKey), null);
			requestContext.setStubSessionCookieTokenValue(CookieConstant.EMAIL, 
					StubhubCookieEncryptor.encryptCookie(userContact.getEmail(), cookieKey), null);
			requestContext.setStubSessionCookieTokenValue(CookieConstant.LOADED_IP_NUMBER, 
					getIPNumber(requestContext.getIp()).toString(), null);
			requestContext.setStubSessionCookieTokenValue(CookieConstant.COBRAND_ID, requestContext.getCobrandId().toString(), null);
			requestContext.setStubSessionCookieTokenValue(CookieConstant.COBRAND, requestContext.getCobrand(), null);
			requestContext.setStubSessionCookieTokenValue(CookieConstant.ZIP_CODE, 
					StubhubCookieEncryptor.encryptCookie(userContact.getZip(), cookieKey), null);
			// change the way of fetching userId, JIRA : https://jira.stubcorp.dev/browse/BRS-1892
			Long userId = userFacade.getCachedUserIdByGuid(requestContext.getUserGuid());
			requestContext.setStubSessionCookieTokenValue(CookieConstant.LOADED_STUB_UID, userId.toString(), null);
			requestContext.setStubSessionCookieTokenValue(CookieConstant.FIRST_NAME, 
					StubhubCookieEncryptor.encryptCookie(userContact.getFirstName(), cookieKey), null);
			requestContext.setStubSessionCookieTokenValue(CookieConstant.SESSION_ID, 
					"" + requestContext.getSessionId(), null);

			// temporary session and user agreement not signed, so remove STUB_SECR
			// pass all other Cookies
			if (isTempSession != null && isTempSession.equals("true")) 
				UserSessionUtil.putCookieRemoveInstructions(StubhubCredentials.getRequestContext(), CookieConstant.STUB_SECR);

			requestContext.writeCookies(response);
		} catch (Exception e) {
			log.error("Unable to set Cookies", e);
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "code#cookies");		
			return;			
		}

		// Construct response body and send it back
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		
		StringBuffer retStrBuf = new StringBuffer();
		String lastLoggedInTime = "";
		if (lastSession != null)
			lastLoggedInTime = dateFormat.format(lastSession.getBeginDate().getTime());
		if (httpAccept != null && httpAccept.equals("application/xml")) {
			// XML Response type
			retStrBuf.append("<user><userId>"); 
	    	retStrBuf.append(session.getUserGuid()); retStrBuf.append("</userId>");
	    	retStrBuf.append("<sessionId>"); retStrBuf.append(sessionGuid); retStrBuf.append("</sessionId>");
	    	retStrBuf.append("<firstName>"); retStrBuf.append(userContact.getFirstName()); retStrBuf.append("</firstName>");
	    	retStrBuf.append("<lastName>"); retStrBuf.append(userContact.getLastName()); retStrBuf.append("</lastName>");
	    	retStrBuf.append("<email>"); retStrBuf.append(userContact.getEmail()); retStrBuf.append("</email>");
	    	retStrBuf.append("<lastLoggedInTime>"); retStrBuf.append(lastLoggedInTime); retStrBuf.append("</lastLoggedInTime>");
	    	retStrBuf.append("</user>");
	    	response.setContentType("application/xml");
		} else {
			// JSON Response type
			retStrBuf.append("{\"user\":{\"userId\":\""); 
			retStrBuf.append(session.getUserGuid()); 
			retStrBuf.append("\",\"sessionId\":\""); retStrBuf.append(sessionGuid); 
			retStrBuf.append("\",\"firstName\":\""); retStrBuf.append(userContact.getFirstName()); 
			retStrBuf.append("\",\"lastName\":\""); retStrBuf.append(userContact.getLastName()); 
			retStrBuf.append("\",\"email\":\""); retStrBuf.append(userContact.getEmail()); 
			retStrBuf.append("\",\"lastLoggedInTime\":\""); retStrBuf.append(lastLoggedInTime); 
			retStrBuf.append("\"}}");
			response.setContentType("application/json");
		}
		
		response.getOutputStream().print(retStrBuf.toString());
    	response.getOutputStream().flush();
			

		try {
			StubhubCredentials.removeRequestContext();
		} catch (StubHubSystemException e) {
			log.error("StubHubSystemException happened", e);
		}
		log.debug("StubhubUserSessionFilter.doFilter method ...END");

		filterChain.doFilter(req, resp);

		SecurityContextHolder.clearContext();
	}
	
	/**
	 * related ticket : https://jira.stubcorp.dev/browse/REG-71 <br/>
	 * Add one whiteList to let the login function work after user was blocked.
	 * @param request
	 * @param response
	 * @return
	 */
	private boolean whiteListCheckPassed(HttpServletRequest request, HttpServletResponse response) {
		if(request.getParameter(Constants.FORCE_LOGIN)==null || !request.getParameter(Constants.FORCE_LOGIN).equals(Constants.FORCE_LOGIN_TRUE)){
			return false;
		}
		String referer = request.getHeader(Constants.REFERER);
		log.info("Check the white list with referer : " + referer);
		String whiteListStr = StubHubProperties.getProperty(Constants.FAILEDLOGIN_WHITELIST_REFERER_KEY, Constants.FAILEDLOGIN_WHITELIST_DEFAULT);
		for(String str : whiteListStr.split(Constants.COMMA)){
			if(StringUtils.containsIgnoreCase(referer, str)){
				return true;
			}
		}
		return false;
	}

	private String generateCookieKey(Long sessionId){
		if(sessionId == null){
			return null;
		}
		String key = StubHubProperties.getProperty(StubhubCookieEncryptor.ENCRYPT_COOKIE_KEY, null);
		String cookieKey = StubhubCookieEncryptor.getCookieKey(sessionId.toString(), key);
		return cookieKey;
	}
	
	/**
	 * for example: 201.202.203.204 transformed to:
	 * 201*256^3+202*256^2+203*256+204
	 * 
	 * @param address
	 * @return
	 */
	private Long getIPNumber(String address) {
		String[] addr = address.split("\\.");
		if (addr.length != 4) {
			log.warn("wrong ip address:" + address);
			return null;
		}
		long[] num = new long[4];
		for (int i = 0; i < 4; i++) {
			try {
				num[i] = Long.parseLong(addr[i]);
			} catch (Exception e) {
				log.warn("wrong ip address:" + address);
				return null;
			}
		}
		long ip_number = 0;
		long q = 1;
		for (int i = 3; i >= 0; i--) {
			ip_number += num[i] * q;
			q *= 256l;
		}
		return ip_number;
	}

	// store in memcache that this username had one more failed login attempt
	private void recordFailedAttempt(String username, String ipAddress) {
		StringBuffer failedLoginAttemptKey = new StringBuffer("failedlogin:").append(username);
		Integer failedAttemptCount = (Integer) store.get(failedLoginAttemptKey.toString());
		if (failedAttemptCount == null) 
			failedAttemptCount = 0;
		
		// default 1 hour = 3600 secs account locked
		Integer lockedOutSeconds = StubHubProperties.getPropertyAsInt("loginapi.failed.login.delay.seconds", 3600);
		store.put(failedLoginAttemptKey.toString(), failedAttemptCount + 1, lockedOutSeconds);

		StringBuffer failedLoginIpAttemptKey = new StringBuffer("failedloginip:");
		failedLoginIpAttemptKey.append(username).append(":").append(ipAddress);
		// failedLoginIpAttemptKey is null, so first time	
		if (log.isDebugEnabled()) {
			log.debug("Login Failed:" + username + ",ipAddress:" + ipAddress);
			log.debug("Storing in Memcache: " + failedLoginIpAttemptKey.toString());
		}
		// default 5 secs between failed login attempts
		Integer delaySeconds = StubHubProperties.getPropertyAsInt("loginapi.multiple.attempt.delay.seconds", 5);
		store.put(failedLoginIpAttemptKey.toString(), Boolean.TRUE, delaySeconds);
	}
	
	private String generateUserTempLoginFlagKey(String userGuid, HttpServletRequest request) throws Exception {
		StringBuilder sb=new StringBuilder(userGuid);
		sb.append("_");
		sb.append(DomainUtils.getCurrentDomain().getDomainName());
		return sb.toString();
	}
	
    /**
     * whether the input value is an email address
     * 
     * @param value
     * @return
     */
    private static boolean validateEmailAddress(String value) {
        if (value == null) {
            return true;
        }
        String regex = "[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?";
        // old one: String regex =
        // "^((([a-z]|\\d|[!#\\$%&'\\*\\+\\-\\/=\\?\\^_`{\\|}~]|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])+(\\.([a-z]|\\d|[!#\\$%&'\\*\\+\\-\\/=\\?\\^_`{\\|}~]|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])+)*)|((\\x22)((((\\x20|\\x09)*(\\x0d\\x0a))?(\\x20|\\x09)+)?(([\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x7f]|\\x21|[\\x23-\\x5b]|[\\x5d-\\x7e]|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])|(\\\\([\\x01-\\x09\\x0b\\x0c\\x0d-\\x7f]|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF]))))*(((\\x20|\\x09)*(\\x0d\\x0a))?(\\x20|\\x09)+)?(\\x22)))@((([a-z]|\\d|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])|(([a-z]|\\d|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])([a-z]|\\d|-|\\.|_|~|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])*([a-z]|\\d|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])))\\.)+(([a-z]|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])|(([a-z]|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])([a-z]|\\d|-|\\.|_|~|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])*([a-z]|[\\u00A0-\\uD7FF\\uF900-\\uFDCF\\uFDF0-\\uFFEF])))\\.?$";
        boolean theResult = value.toLowerCase().matches(regex);
        log.debug("ValidationUtil.. validEmailAddress(" + value + ") returning: " + theResult);
        return theResult;
    }
}
