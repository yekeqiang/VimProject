/**
 * Copyright (c) 2012 StubHub Inc. All rights reserved.
 */
package com.stubhub.common.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.LogSF;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.stereotype.Component;

import com.stubhub.common.ErrorConstants;
import com.stubhub.common.business.entity.StubHubFile;
import com.stubhub.common.config.RequestContext;
import com.stubhub.common.config.SpringContextLoader;
import com.stubhub.common.config.StubhubCredentials;
import com.stubhub.common.exceptions.base.StrongAuthException;
import com.stubhub.common.exceptions.base.UnauthorizedUserException;
import com.stubhub.resource.business.entity.UploadFileInfo;
import com.stubhub.ui.common.util.LegacyCookieToken;
import com.stubhub.ui.common.util.LegacyCookieUtils;
import com.stubhub.ui.common.util.PropertiesConstant;
import com.stubhub.user.business.manager.UsersMgr;

/**
 * SecuredResourceUtil class will be the new gateway for all the secured resource service calls via JSON object request(POST method)
 * or HttpClient(GET method) request.
 * 
 */
@Component("securedResourceUtil")
public class SecuredResourceUtil {

    private static final String SKAPING_PATH = "/skaping";

    private static final String CDAPING_PATH = "/cdaping";

    private static final String FILE_INFO = "fileInfo";
    private static final String MOBILE_APP = "mobileapp";

    private static Logger log = Logger.getLogger(SecuredResourceUtil.class);

    private static final String STUBNETUID = "STUBNETUID=";
    private static final String STUB_PERSISTENT = "STUB_PERSISTENT=";
    private static final String STUB_SECR = "STUB_SECR=";
    private static final String STUB_INFO = "STUB_INFO=";
    private static final String STUB_SESSION = "STUB_SESSION=";
    private static final String STUB_SESS = "STUB_SESS=";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String CHARSET = "UTF-8";
    private static final String CREATE_RESOURCE_API_PATH = "/pdf/filePath";
    private static final String DELETE_FILE_API_PATH = "/filePath";
    private static final String FILE_INFO_REQUEST = "FileInfoRequest";
    private static final String FILE_INFO_RESPONSE = "FileInfoResponse";
    private static final String IMAGE_RESPONSE = "ImageResponse";
    private static final String BARCODE_RESPONSE = "BarcodeResponse";
    private static final String DELETE_FILE_FLAG_RESPONSE = "deletedFlag";
    private static final String ENCRYPTED_FILE_PATH = "encryptedFilePath";
    private static final String CONTROL_CODE = "controlCode";
    private static final String CONTROL_CODE_TOKEN = "controlCodeToken";
    private static final String GET_UPLOAD_FILE_API_PATH = "/file/";
    private static final String GET_UPLOAD_FILE_INFO_API_PATH = "/fileInfo/";
    private static final String GET_UPLOAD_IMAGE_API_PATH = "/image/";
    private static final String UPLOAD_FILE_RESPONSE = "UploadFileResponse";
    private static final String UPLOAD_IMAGE_RESPONSE = "UploadImageResponse";
    private static final String UPLOADED_IMAGE_PATH = "uploadedImagePath";
    private static final String UPLOADED_FILE_PATH = "uploadedFilePath";
    private static final String UPLOADED_FILE_RESPONSE_FLAG = "isUploadSuccessful";
    private static final String UPLOADED_FILE_PAGE_COUNT = "pageCount";
    private static final String PDF_FILE_PATH = "pdfFilePath";
    private static final String DELETE_FILE_FLAG = "deleteFileFlag";
    private static final String FILE_INFO_LIST = "fileInfoList";
    private static final String DECRYPTED_RESOURCE = "decryptedResource";
    private static final String FILE_PATH = "filePath";
    private static final String FILE_NAME = "fileName";
    private static final String PDF = "pdf";
    private static final String IMAGE = "image";
    private static final String LISTING_ID = "listingId";
    private static final String EVENT_ID = "eventId";
    private static final String UPLOADED_FILE_NAME = "uploadedFileName";
    private static final String UPLOADED_IMAGE_NAME = "uploadedImageName";
    private static final String UPLOAD_FILE_REQUEST = "UploadFileRequest";
    private static final String UPLOAD_IMAGE_REQUEST = "UploadImageRequest";
    private static final String IS_EXTERNAL_LISTING_REQ = "isExternalLisitngRequest";
    private static final String BARCODE_REQUEST = "BarcodeRequest";
    private static final String IS_STUB_NET_USER = "isStubnetUser";
    private static final String IS_STUBHUB_USER = "isStubhubUser";
    private static final String IS_RESELL_FLOW = "isResellFlow";
    private static final String ORDER_ID = "orderId";
    private static final String BARCODE_PATH = "/barcode/";
    private static final String ENCRYPT_CONTROL_CODE_PATH = "/barcode/controlCode/";
    private static final String BARCODE_TOKEN_PATH = "/barcodeToken/";
    private static final String PDF_PATH = "/pdf/";
    private static final String PDF_FILEPATH="/pdfFile/";
    private static final String PDF_EXTERNAL_LISTING_FILE_PATH = "/externalListingPdf/";
    private static final String IMAGE_PATH = "/image/";
    private static final String SECURED_TICKET_TOKEN_HEADER = StubHubProperties.getProperty(PropertiesConstant.TICKETPROTECTION_SECURED_HEADER_NAME,"secured_ticket_token");
    private static final String SECURED_TICKET_TOKEN_HEADER_VALUE = StubHubProperties.getProperty(PropertiesConstant.TICKETPROTECTION_SECURED_HEADER_VALUE,"$ecureT!cket");
    private static final String FILE_BYTES_RESPONSE = "fileBytes";
    private static final String STUBNET_USER_ID = "stubnetUserId";
    private static final String IMAGE_BYTES_RESPONSE = "imageBytes";
    private static final String CREATE_ENCRYPTED_PDF_API_PATH ="/pdf/pdfFilePath";
    private static final String GET_UPLOAD_SPLIT_FILE_INFO_API_PATH = "/splitFile/";
    private static final String UPLOADED_FILE_SIZE = "uploadedFileSize";
    private static final String ERROR_RESPONSE = "Errors";
    private static final String ERROR_DETAIL_RESPONSE = "ErrorDetails";
    private static final String ERROR_CODE = "StubHubErrorCode";
    private static final String ERROR_MESSAGE = "StubHubErrorMessage";
    private static final String UPLOAD_FILE_SIZE = "uploadedFileSize" ;
    
    private static final String version = StubHubProperties.getProperty(PropertiesConstant.TICKET_PROTECTION_API_VERSION,"1.0");
    private static final String TICKETPROTECTION_API_URL = StubHubProperties.getProperty(PropertiesConstant.TICKETPROTECTION_API);
    private static final int CONNECTION_TIMEOUT = StubHubProperties.getPropertyAsInt(PropertiesConstant.TICKET_PROTECTION_API_TIMEOUT,
                                                                                     60000);
    private static final String ENCRYPTED_FILE_TOKEN = "encryptedFileToken";
    private static final String SYNCHRONOUS_FLAG = "synchFlag";

    // PLATFORM-547
    // Get the value of Switch for Ticket Protection PDF Encryption, defined in MASTER.stubhub.properties
    private static boolean ticketProtectionPdfEncryptionSwitch = StubHubProperties.getPropertyAsBoolean(PropertiesConstant.TICKETPROTECTION_PDF_ENCRYPTION_SWITCH,
                                                                                                        true); // True by default
    
    /**
     * Method used to call the decrypt barcode secured service API with the given control code token
     * 
     * @since 20/09/2012
     * 
     * @param isStubnetUser
     *            -input flag value for StubNet user
     * @param isStubhubUser
     *            -input flag value for StubHub user
     * @param isResell
     *            -input flag value for ReSell flow
     * @param listingId
     *            -input listingId
     * @param orderId
     *            -input orderId
     * @param controlCodeToken
     *            -input control code token to be decrypted
     * @return String - return decrypted Control code
     * @throws StrongAuthException
     */
    public static String decryptBarcode(boolean isStubnetUser, boolean isStubhubUser, boolean isResell, Long listingId,
                                        Long orderId, String controlCodeToken) throws StrongAuthException {
        LogSF.debug(log, "Entering decryptBarcode secured service with controlCodeToken={}", controlCodeToken);
        boolean barcodeResponseAsXml = StubHubProperties.getPropertyAsBoolean(PropertiesConstant.TICKETPROTECTION_BARCODE_SERVICE_RESPONSE_ASXML, false ); 
        String controlCode = null;
        String response = null;
        if (null != controlCodeToken) {
            StringBuilder ticketProtectionServiceUrl = new StringBuilder(TICKETPROTECTION_API_URL).append(Constants.BACKWARD_SLASH).append(version).append(BARCODE_PATH);
            String cookieValue = getStubCookieInformation();
            try {

                int statusCode = 0;

                JSONObject barcodeRequestJSON = new JSONObject();
                barcodeRequestJSON.put(IS_STUB_NET_USER, isStubnetUser);
                barcodeRequestJSON.put(IS_STUBHUB_USER, isStubhubUser);
                barcodeRequestJSON.put(IS_RESELL_FLOW, isResell);
                barcodeRequestJSON.put(LISTING_ID, listingId);
                barcodeRequestJSON.put(ORDER_ID, orderId);
                barcodeRequestJSON.put(CONTROL_CODE_TOKEN, controlCodeToken);
                RequestContext requestContext = StubhubCredentials.getRequestContext();
                Long userId = null;
                Long stubnetUserId = null;
                if (null != requestContext) {
                	stubnetUserId = requestContext.getStubnetUserId();
                    barcodeRequestJSON.put(STUBNET_USER_ID, userId); 
                    userId = (null != requestContext.getUserId()) ? requestContext.getUserId() : getStubhubUserId(requestContext);
                    
                }
                Object[] loggerInputArray = new Object[] {userId,stubnetUserId,listingId,orderId};
                LogSF.info(log,"decryptBarcode invoked by stubhubUserId={} / stubnetUserId={} for listingId={} orderId={}", loggerInputArray);

                JSONObject decryptBarcodeRequestJSON = new JSONObject();
                decryptBarcodeRequestJSON.put(BARCODE_REQUEST, barcodeRequestJSON);
                
                final HttpPost method = new HttpPost(ticketProtectionServiceUrl.toString());
                final StringEntity entity = new StringEntity(decryptBarcodeRequestJSON.toString(), CHARSET);
                entity.setContentType(JSON_CONTENT_TYPE);
                method.setEntity(entity);
                method.addHeader(Constants.COOKIE.toLowerCase(), cookieValue);
                method.getParams().setIntParameter(HttpMethodParams.SO_TIMEOUT, CONNECTION_TIMEOUT);
                if (barcodeResponseAsXml) {
                    method.setHeader("accept", "application/xml; charset=UTF-8");
                } else {
                    method.setHeader("accept", "application/json; charset=UTF-8");
                }                
                long startTime = System.currentTimeMillis();
                HttpClient httpClient = HttpClient4Util.getHttpClient();
                HttpResponse httpResponse = httpClient.execute(method);
                LogSF.info(log,"Invoked decrypt barcode service with duration={}", System.currentTimeMillis()- startTime);
                statusCode = httpResponse.getStatusLine().getStatusCode();
                LogSF.info(log, "Simple HttpResponse StatusCode={}", statusCode);
                if (HttpStatus.SC_OK != statusCode) {
                    Object[] statusArray = new Object[] { statusCode };
                    LogSF.error(log, "Call to decryptBarcode service failed, returned statusCode={}", statusArray);
                } else {
                    if (null != httpResponse) {
                        HttpEntity entityResponse = httpResponse.getEntity();
                        if (null != entityResponse) {
                            response = EntityUtils.toString(entityResponse, "UTF-8");
                            if (barcodeResponseAsXml) {
                                controlCode = getXMLTagValue(response, CONTROL_CODE);
                            } else {
                                JSONObject barcodeResponseObj = new JSONObject(response);
                                if (null != barcodeResponseObj.opt(BARCODE_RESPONSE)
                                    && !StringUtils.isStringNullorEmpty(barcodeResponseObj.opt(BARCODE_RESPONSE).toString())) {
                                    JSONObject barcodeResponse = barcodeResponseObj.getJSONObject(BARCODE_RESPONSE);
                                    if (null != barcodeResponse.opt(CONTROL_CODE)) {
                                        controlCode = barcodeResponse.getString(CONTROL_CODE);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (JSONException jsonException) {
                LogSF.error(log, jsonException, "JSON object creation was not successful for decrypting barcode ", null);
                controlCode = getXMLTagValue(response, CONTROL_CODE);
                if (null == controlCode || controlCode.isEmpty()){
                	throw new StrongAuthException(ErrorConstants.TECHNICAL_ERROR, ErrorConstants.INTERNAL_SERVER_ERROR);
                }
            } catch (UnsupportedEncodingException unsupportedEncodingException) {
                LogSF.error(log, unsupportedEncodingException, "UnsupportedEncodingException occured while decrypting barcode ",
                            null);
            } catch (HttpException httpException) {
                LogSF.error(log, httpException, "Error occured during the call for execute method of http client ", null);
            } catch (IOException ioException) {
                LogSF.error(log, ioException, "Error occured during reading the response as stream ", null);
            }
        }
        return controlCode;
    }

    /**
     * Method to get the control Code based on controlCodeToken using a filter token: will be used for jobs/listeners
     * 
     * @param controlCodeToken
     * @return controlCode
     * @throws StrongAuthException
     */
    public static String decryptBarcode(String controlCodeToken) throws StrongAuthException {
        String controlCode = null;
        HttpGet method = null;
        String barcodeResponseContent = null;
        boolean barcodeResponseAsXml = StubHubProperties.getPropertyAsBoolean(PropertiesConstant.TICKETPROTECTION_BARCODE_SERVICE_RESPONSE_ASXML, false );
        try {
            StringBuilder ticketProtectionServiceUrl = new StringBuilder(TICKETPROTECTION_API_URL)
                .append(Constants.BACKWARD_SLASH).append(version).append(BARCODE_TOKEN_PATH).append(controlCodeToken);
            LogSF.debug(log, "Invoking GetFromUrl of HttpClient4Util with url={}", ticketProtectionServiceUrl);
            method = new HttpGet(ticketProtectionServiceUrl.toString());
            method.setHeader(SECURED_TICKET_TOKEN_HEADER, SECURED_TICKET_TOKEN_HEADER_VALUE);
            if (barcodeResponseAsXml){
            	method.setHeader("accept",
                            "application/xml; charset=UTF-8");
            }else{
            	method.setHeader("accept",
                	"application/json; charset=UTF-8");
            }
            long startTime = System.currentTimeMillis();
            barcodeResponseContent = HttpClient4Util.getFromUrl(method, CONNECTION_TIMEOUT, false, null);
            LogSF.info(log,"Invoked decrypt Barcode service with duration={}", (System.currentTimeMillis()- startTime));
            LogSF.debug(log, "Successfully invoked decryptBarcode api", null);            
            if (null != barcodeResponseContent) {
            	if (barcodeResponseAsXml){
            		controlCode = getXMLTagValue(barcodeResponseContent, CONTROL_CODE);
            		if (controlCode.isEmpty()){
            			throw new StrongAuthException(getXMLTagValue(barcodeResponseContent, ERROR_MESSAGE),
            					getXMLTagValue(barcodeResponseContent, ERROR_CODE));
            		}
            	}else{
            		JSONObject barcodeRessponseObj = new JSONObject(barcodeResponseContent);

            		if (null != barcodeRessponseObj.opt(BARCODE_RESPONSE)
            				&& !StringUtils.isStringNullorEmpty(barcodeRessponseObj.opt(BARCODE_RESPONSE).toString())) {
            			JSONObject barcodeResponse = barcodeRessponseObj.getJSONObject(BARCODE_RESPONSE);

            			if (null != barcodeResponse.opt(CONTROL_CODE)) {
            				controlCode = barcodeResponse.getString(CONTROL_CODE);
            				
            			}
            			else if (null != barcodeRessponseObj.opt(ERROR_RESPONSE)
            					&& !StringUtils.isStringNullorEmpty(barcodeRessponseObj.opt(ERROR_RESPONSE).toString())) {
            				JSONObject errorResponse = barcodeRessponseObj.getJSONObject(ERROR_RESPONSE);
            				if (null != errorResponse.opt(ERROR_DETAIL_RESPONSE)
            						&& !StringUtils.isStringNullorEmpty(errorResponse.opt(ERROR_DETAIL_RESPONSE).toString())) {
            					JSONObject errorDetailResponse = errorResponse.getJSONObject(ERROR_DETAIL_RESPONSE);
            					String errorMsg = errorDetailResponse.getString(ERROR_MESSAGE);
            					String errorCode = errorDetailResponse.getString(ERROR_CODE);
            					throw new StrongAuthException(errorMsg, errorCode);
            				}
            			}
            		}
            	}
            }
        } catch (JSONException jsonException) {
            LogSF.error(log, jsonException, "JSON object creation was not successful for decrypting barcode ", null);
            controlCode = getXMLTagValue(barcodeResponseContent, CONTROL_CODE);
            if (null == controlCode || controlCode.isEmpty()){
            	throw new StrongAuthException(ErrorConstants.TECHNICAL_ERROR, ErrorConstants.INTERNAL_SERVER_ERROR);
            }
        } catch (UnsupportedEncodingException unsupportedEncodingException) {
            LogSF.error(log, unsupportedEncodingException, "UnsupportedEncodingException occured while decrypting barcode ", null);
        } catch (HttpException httpException) {
            LogSF.error(log, httpException, "Error occured during the call for execute method of http client ", null);
        } catch (IOException ioException) {
            LogSF.error(log, ioException, "Error occured during the call for execute method of http client ", null);
        }finally{
            removeSecuredHeader(method);
        }        
        return controlCode;
    }

    

    /**
     * Method used to encrypt barcode with the given control code token via Http client call
     * 
     * @since 01/10/2012
     * @param controlCode
     *            - input control code or barcode number
     * @return String - return control code Token
     * @throws StrongAuthException
     */
    public static String encryptBarcode(String controlCode) throws StrongAuthException {
        String controlCodeToken = null;
        String barcodeResponseContent = null;
        boolean barcodeResponseAsXml = StubHubProperties.getPropertyAsBoolean(PropertiesConstant.TICKETPROTECTION_BARCODE_SERVICE_RESPONSE_ASXML, false );
        if (null != controlCode) {
            StringBuilder ticketProtectionServiceUrl = new StringBuilder(TICKETPROTECTION_API_URL)
            .append(Constants.BACKWARD_SLASH).append(version).append(BARCODE_PATH);
            try {
				ticketProtectionServiceUrl.append(URLEncoder.encode(controlCode.trim(), "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				LogSF.error(log, e, "UnsupportedEncodingException occured while invoking encryptControlCode", null);
				throw new StrongAuthException(ErrorConstants.UNSUPPORTED_ENCODE, ErrorConstants.INTERNAL_SERVER_ERROR);
			}
            String cookieValue = getStubCookieInformation();
            try {
                long startTime = System.currentTimeMillis();
                if (barcodeResponseAsXml){
                	barcodeResponseContent = HttpClient4Util.getFromUrl(ticketProtectionServiceUrl.toString(),
                            CONNECTION_TIMEOUT, false, cookieValue, barcodeResponseAsXml);
                }else{
                	barcodeResponseContent = HttpClient4Util.getFromUrl(ticketProtectionServiceUrl.toString(),
                			CONNECTION_TIMEOUT, false, cookieValue);
                }
                LogSF.info(log,"Invoked encrypt Barcode service with duration={}", (System.currentTimeMillis()- startTime));
                LogSF.info(log, "Invoked encryptBarcode service with response={}",barcodeResponseContent);
                RequestContext requestContext = StubhubCredentials.getRequestContext();
                Long userId = null;
                Long stubnetUserId = null;
                if (null != requestContext) {
                	stubnetUserId = requestContext.getStubnetUserId();
                    userId = (null != requestContext.getUserId()) ? requestContext.getUserId() : getStubhubUserId(requestContext);
                    
                }
                Object[] loggerInputArray = new Object[] {userId,stubnetUserId};
                LogSF.info(log,"encryptBarcode invoked by stubhubUserId={} / stubnetUserId={} ", loggerInputArray);
                LogSF.info(log, "Response : "  +  barcodeResponseContent, null);
                if (null != barcodeResponseContent) {
                	if (barcodeResponseAsXml){
                		controlCodeToken = getXMLTagValue(barcodeResponseContent, CONTROL_CODE_TOKEN);
                	}else{
                		JSONObject barcodeRessponseObj = new JSONObject(barcodeResponseContent);
                		if (null != barcodeRessponseObj.opt(BARCODE_RESPONSE)
                				&& !StringUtils.isStringNullorEmpty(barcodeRessponseObj.opt(BARCODE_RESPONSE).toString())) {
                			JSONObject barcodeResponse = barcodeRessponseObj.getJSONObject(BARCODE_RESPONSE);
                			if (null != barcodeResponse.opt(CONTROL_CODE_TOKEN)) {
                				controlCodeToken = barcodeResponse.getString(CONTROL_CODE_TOKEN);
                			}
                		}
                	}
                }
            } catch (JSONException jsone) {
                LogSF.error(log, jsone, "JSONException occured while invoking encryptBarcode ", null);
                controlCodeToken = getXMLTagValue(barcodeResponseContent, CONTROL_CODE_TOKEN);
                if (null == controlCodeToken || controlCodeToken.isEmpty()){
                	throw new StrongAuthException(ErrorConstants.TECHNICAL_ERROR, ErrorConstants.INTERNAL_SERVER_ERROR);
                }
            } catch (IOException ioe) {
                LogSF.error(log, ioe, "IOException occured while invoking encryptBarcode", null);
            }
        }
        return controlCodeToken;
    }

	private static Long getStubhubUserId(RequestContext requestContext) {
		if (requestContext.getUserGuid() != null) {
			UsersMgr usersMgr = (UsersMgr) SpringContextLoader.getInstance().getBean("usersMgrBiz");
			return usersMgr.getCachedUserIdByGuid(StubhubCredentials.getRequestContext().getUserGuid());
		}
		return null;
	}

    /**
     * Method used to encrypt controlcode with the given control code token via Http client call
     * 
     * @since 01/10/2012
     * @param controlCode
     *            - input control code or barcode number
     * @return String - return control code Token
     * @throws StrongAuthException
     */
    public static String encryptControlCode(String controlCode) throws StrongAuthException {
        String controlCodeToken = null;
        HttpGet method = null;
        String barcodeResponseContent = null;
        boolean barcodeResponseAsXml = StubHubProperties.getPropertyAsBoolean(PropertiesConstant.TICKETPROTECTION_BARCODE_SERVICE_RESPONSE_ASXML, false );
        if (null != controlCode) {
            try {
                StringBuilder ticketProtectionServiceUrl = new StringBuilder(TICKETPROTECTION_API_URL)
                  .append(Constants.BACKWARD_SLASH).append(version).append(ENCRYPT_CONTROL_CODE_PATH);
                try {
    				ticketProtectionServiceUrl.append(URLEncoder.encode(controlCode.trim(), "UTF-8"));
    			} catch (UnsupportedEncodingException e) {
    				LogSF.error(log, e, "UnsupportedEncodingException occured while invoking encryptControlCode", null);
    				throw new StrongAuthException(ErrorConstants.UNSUPPORTED_ENCODE, ErrorConstants.INTERNAL_SERVER_ERROR);
    			}
                LogSF.debug(log, "Invoking encryptControlCode with url={}", ticketProtectionServiceUrl);
                method = new HttpGet(ticketProtectionServiceUrl.toString());
                method.setHeader(SECURED_TICKET_TOKEN_HEADER, SECURED_TICKET_TOKEN_HEADER_VALUE);
                if (barcodeResponseAsXml){
                	method.setHeader("accept",
                    	"application/xml; charset=UTF-8");
                }else{
                	method.setHeader("accept",
                		"application/json; charset=UTF-8");
                }
                LogSF.debug(log, "encryptBarcode secured service with barcodeResponseAsXml={}", barcodeResponseAsXml);
                long startTime = System.currentTimeMillis();
                barcodeResponseContent = HttpClient4Util.getFromUrl(method, CONNECTION_TIMEOUT, false, null);
                LogSF.info(log,"Invoked encryptControlCode service with duration={}", (System.currentTimeMillis()- startTime));
                if (null != barcodeResponseContent) {
                	if (barcodeResponseAsXml){
                		controlCodeToken = getXMLTagValue(barcodeResponseContent, CONTROL_CODE_TOKEN);
                		if (controlCode.isEmpty()){
                			throw new StrongAuthException(getXMLTagValue(barcodeResponseContent, ERROR_MESSAGE),
                					getXMLTagValue(barcodeResponseContent, ERROR_CODE));
                		}
                	}else{
                		JSONObject barcodeRessponseObj = new JSONObject(barcodeResponseContent);

                		if (null != barcodeRessponseObj.opt(BARCODE_RESPONSE)
                				&& !StringUtils.isStringNullorEmpty(barcodeRessponseObj.opt(BARCODE_RESPONSE).toString())) {
                			JSONObject barcodeResponse = barcodeRessponseObj.getJSONObject(BARCODE_RESPONSE);

                			if (null != barcodeResponse.opt(CONTROL_CODE_TOKEN)) {
                				controlCodeToken = barcodeResponse.getString(CONTROL_CODE_TOKEN);
                			}
                		} else if (null != barcodeRessponseObj.opt(ERROR_RESPONSE)
                				&& !StringUtils.isStringNullorEmpty(barcodeRessponseObj.opt(ERROR_RESPONSE).toString())) {
                			JSONObject errorResponse = barcodeRessponseObj.getJSONObject(ERROR_RESPONSE);
                			if (null != errorResponse.opt(ERROR_DETAIL_RESPONSE)
                					&& !StringUtils.isStringNullorEmpty(errorResponse.opt(ERROR_DETAIL_RESPONSE).toString())) {
                				JSONObject errorDetailResponse = errorResponse.getJSONObject(ERROR_DETAIL_RESPONSE);
                				String errorMsg = errorDetailResponse.getString(ERROR_MESSAGE);
                				String errorCode = errorDetailResponse.getString(ERROR_CODE);
                				throw new StrongAuthException(errorMsg, errorCode);
                			}
                		}
                	}
                }
            } catch (JSONException jsone) {
                LogSF.error(log, jsone, "JSONException occured while invoking encryptControlCode", null);
                controlCodeToken = getXMLTagValue(barcodeResponseContent, CONTROL_CODE_TOKEN);
                if (null == controlCodeToken || controlCodeToken.isEmpty()){
                	throw new StrongAuthException(ErrorConstants.TECHNICAL_ERROR, ErrorConstants.INTERNAL_SERVER_ERROR);
                }
            } catch (IOException ioe) {
                LogSF.error(log, ioe, "IOException occured while invoking encryptControlCode", null);
            } finally {
                removeSecuredHeader(method);
            }
        }
        return controlCodeToken;
    }
    
    /**
     * Method used to call the encryptPDF secured service API via JSON Object PostMethod request
     * 
     * @since 20/09/2012
     * @param pdfFilePath
     *            - input unEncrypted pdfFilePath
     * @param deleteFlag
     *            - input delete Status Flag
     * @param synchFlag
     *            - input synchronous Flag
     * @return String encryptedFilePath
     * @throws StrongAuthException
     */
    public static String encryptPdfDoc(String pdfFilePath, boolean deleteFlag, boolean synchFlag) throws StrongAuthException {

        String encryptedFilePath = null;

        if (null != pdfFilePath) {

            // PLATFORM-547, Service call is made only if the ticketProtectionPdfEncryptionSwitch switch is enabled
            if (ticketProtectionPdfEncryptionSwitch) {
                StringBuilder createResourceServiceUrl = new StringBuilder(TICKETPROTECTION_API_URL).append(Constants.BACKWARD_SLASH)
                                                                                                    .append(version)
                                                                                                    .append(CREATE_ENCRYPTED_PDF_API_PATH);

                LogSF.debug(log, "Migration Job CreateResourceServiceUrl={}", createResourceServiceUrl);

                int statusCode = 0;
                String responseXML = null;
                try {

                    JSONObject pdfFilePathJSON = new JSONObject();
                    pdfFilePathJSON.put(PDF_FILE_PATH, pdfFilePath);
                    pdfFilePathJSON.put(DELETE_FILE_FLAG, deleteFlag);
                    pdfFilePathJSON.put(SYNCHRONOUS_FLAG, synchFlag);

                    JSONObject fileInfoRequest = new JSONObject();
                    fileInfoRequest.put(FILE_INFO_REQUEST, pdfFilePathJSON);

                    LogSF.debug(log, "JSON Object requestEntity={}", fileInfoRequest.toString());

                    final HttpPost method = new HttpPost(createResourceServiceUrl.toString());
                    final StringEntity entity = new StringEntity(fileInfoRequest.toString(), CHARSET);
                    entity.setContentType(JSON_CONTENT_TYPE);
                    method.setEntity(entity);
                    method.setHeader(SECURED_TICKET_TOKEN_HEADER, SECURED_TICKET_TOKEN_HEADER_VALUE);
                    method.getParams().setIntParameter(HttpMethodParams.SO_TIMEOUT, CONNECTION_TIMEOUT);
                    long startTime = System.currentTimeMillis();
                    HttpClient httpClient = HttpClient4Util.getHttpClient();
                    HttpResponse httpResponse = httpClient.execute(method);
                    LogSF.info(log, "Invoked encryptPdfDoc service with duration={}", (System.currentTimeMillis()- startTime));
                    statusCode = httpResponse.getStatusLine().getStatusCode();
                    LogSF.info(log, "Invoked encryptPdfDoc service with HttpResponse StatusCode={}", statusCode);

                    if (HttpStatus.SC_OK != statusCode) {
                        throw new HttpResponseException(statusCode, "http response statusCode=" + statusCode);
                    } else {
                        HttpEntity entityResponse = httpResponse.getEntity();
                        if (null != entityResponse)
                            responseXML = EntityUtils.toString(entityResponse, "UTF-8");
                    }

                    LogSF.info(log, "Simple HttpResponse XML Content, getEntity() of httpResponse={}", httpResponse.getEntity());

                    LogSF.info(log, "Simple HttpResponse XML Content, responseXML={}", responseXML);

                    if (HttpStatus.SC_OK != statusCode) {
                        Object[] loggerArgumentArray = new Object[] { createResourceServiceUrl.toString(), statusCode };
                        LogSF.error(log, "HTTP call to ServiceURL={} failed, returned statusCode={}", loggerArgumentArray);
                    } else {
                        JSONObject fileInfoResponseObj = null;

                        if (null != responseXML) {
                            LogSF.debug(log, "encryptPdfDoc Service Response={}", responseXML);
                            fileInfoResponseObj = new JSONObject(responseXML);

                            if (null != fileInfoResponseObj.opt(FILE_INFO_RESPONSE)
                                && !StringUtils.isStringNullorEmpty(fileInfoResponseObj.opt(FILE_INFO_RESPONSE).toString())) {
                                JSONObject fileInfoResponse = fileInfoResponseObj.getJSONObject(FILE_INFO_RESPONSE);

                                if (null != fileInfoResponse.opt(ENCRYPTED_FILE_PATH)) {
                                    encryptedFilePath = fileInfoResponse.getString(ENCRYPTED_FILE_PATH);
                                }
                            }
                        }
                    }
                } catch (JSONException jsone) {
                    Object[] loggerInputArray = new Object[] { "JSONException occured while invoking createResource ", deleteFlag };
                    LogSF.error(log, jsone, "{} deleteFileFlag={}", loggerInputArray);
                } catch (UnsupportedEncodingException uee) {
                    Object[] loggerInputArray = new Object[] { "UnsupportedEncodingException occured while invoking createResource" };
                    LogSF.error(log, uee, "{}", loggerInputArray);
                } catch (HttpException htte) {
                    Object[] loggerInputArray = new Object[] { "IOException occured while invoking createResource", responseXML };
                    LogSF.error(log, htte, "{} Response Content={}", loggerInputArray);
                } catch (IOException ioe) {
                    Object[] loggerInputArray = new Object[] { "IOException occured while invoking createResource" };
                    LogSF.error(log, ioe, "{}", loggerInputArray);
                }

            } else {
                // set UnEncrypted pdfFilePath as encryptedFilePath
                encryptedFilePath = pdfFilePath;
            }
        }
        return encryptedFilePath;
    }

    /**
     * Method used to call the encryptPDF secured service API via JSON Object PostMethod request
     * 
     * @since 20/09/2012
     * @param pdfFilePath
     *            - input unEncrypted pdfFilePath
     * @param deleteFlag
     *            - input delete Status Flag
     * @return String encryptedFilePath
     * @throws StrongAuthException
     */
    public static String encryptDoc(String pdfFilePath, boolean deleteFlag) throws StrongAuthException {

        String encryptedFilePath = null;

        if (null != pdfFilePath) {

            // PLATFORM-547, Service call is made only if the ticketProtectionPdfEncryptionSwitch switch is enabled
            if (ticketProtectionPdfEncryptionSwitch) {

                StringBuilder createResourceServiceUrl = new StringBuilder(TICKETPROTECTION_API_URL).append(Constants.BACKWARD_SLASH)
                                                                                                    .append(version)
                                                                                                    .append(CREATE_RESOURCE_API_PATH);

                LogSF.debug(log, "CreateResourceServiceUrl={}", createResourceServiceUrl);

                int statusCode = 0;
                String responseXML = null;
                try {

                    Long userId = null;
                    Long stubnetUserId = null;
                    RequestContext requestContext = StubhubCredentials.getRequestContext();
                    if (null != requestContext) {
                        stubnetUserId = requestContext.getStubnetUserId();
                    	userId = (null != requestContext.getUserId()) ? requestContext.getUserId() : getStubhubUserId(requestContext);                    
                    }
                    Object[] loggerInputArray = new Object[] { userId, stubnetUserId };
                    LogSF.info(log, "encryptDoc invoked by stubhubUserId={} / stubnetUserId={} ", loggerInputArray);

                    boolean cdaSynchCallFlag = StubHubProperties.getPropertyAsBoolean(PropertiesConstant.TICKETPROTECTION_CDA_ENCRYPT_PDF_SYNCHRONOUS_FLAG,
                                                                                      false);

                    JSONObject pdfFilePathJSON = new JSONObject();
                    pdfFilePathJSON.put(PDF_FILE_PATH, pdfFilePath);
                    pdfFilePathJSON.put(DELETE_FILE_FLAG, deleteFlag);
                    pdfFilePathJSON.put(SYNCHRONOUS_FLAG, cdaSynchCallFlag);

                    JSONObject fileInfoRequest = new JSONObject();
                    fileInfoRequest.put(FILE_INFO_REQUEST, pdfFilePathJSON);

                    RequestEntity requestEntity = new StringRequestEntity(fileInfoRequest.toString(), JSON_CONTENT_TYPE, CHARSET);

                    LogSF.debug(log, "JSON Object requestEntity={}", requestEntity.toString());

                    final HttpPost method = new HttpPost(createResourceServiceUrl.toString());
                    final StringEntity entity = new StringEntity(fileInfoRequest.toString(), CHARSET);
                    entity.setContentType(JSON_CONTENT_TYPE);
                    method.setEntity(entity);
                    method.addHeader(Constants.COOKIE.toLowerCase(), getStubCookieInformation());
                    method.getParams().setIntParameter(HttpMethodParams.SO_TIMEOUT, CONNECTION_TIMEOUT);
                    long startTime = System.currentTimeMillis();
                    HttpClient httpClient = HttpClient4Util.getHttpClient();
                    HttpResponse httpResponse = httpClient.execute(method);
                    LogSF.info(log, "Invoked encryptDoc service with duration=" + (System.currentTimeMillis() - startTime), null);
                    statusCode = httpResponse.getStatusLine().getStatusCode();
                    LogSF.info(log, "Simple HttpResponse StatusCode={}", statusCode);

                    if (HttpStatus.SC_OK != statusCode) {
                        throw new HttpResponseException(statusCode, "http response statusCode=" + statusCode);
                    } else {
                        HttpEntity entityResponse = httpResponse.getEntity();
                        if (null != entityResponse) {
                            responseXML = EntityUtils.toString(entityResponse, "UTF-8");
                            JSONObject barcodeRessponseObj = new JSONObject(responseXML);
                            if (null != barcodeRessponseObj.opt(FILE_INFO_RESPONSE)) {
                                JSONObject fileInfoResponse = barcodeRessponseObj.getJSONObject(FILE_INFO_RESPONSE);
                                if (null != fileInfoResponse.opt(ERROR_RESPONSE)
                                    && !StringUtils.isStringNullorEmpty(fileInfoResponse.opt(ERROR_RESPONSE).toString())) {
                                    JSONObject errorResponse = fileInfoResponse.getJSONObject(ERROR_RESPONSE);
                                    if (null != errorResponse.opt(ERROR_DETAIL_RESPONSE)
                                        && !StringUtils.isStringNullorEmpty(errorResponse.opt(ERROR_DETAIL_RESPONSE).toString())) {
                                        JSONObject errorDetailResponse = errorResponse.getJSONObject(ERROR_DETAIL_RESPONSE);
                                        String errorMsg = errorDetailResponse.getString(ERROR_MESSAGE);
                                        String errorCode = errorDetailResponse.getString(ERROR_CODE);
                                        Object[] loggerArgumentArray = new Object[] { errorMsg, errorCode };
                                        LogSF.debug(log, "StubHubErrorMessage={}, StubHubErrorCode={}" , loggerArgumentArray);
                                    }
                                }
                            }
                        }
                    }

                    LogSF.info(log, "Simple HttpResponse XML Content, getEntity() of httpResponse={}", httpResponse.getEntity());

                    LogSF.info(log, "Simple HttpResponse XML Content, responseXML={}", responseXML);

                    if (HttpStatus.SC_OK != statusCode) {
                        Object[] loggerArgumentArray = new Object[] { createResourceServiceUrl.toString(), statusCode };
                        LogSF.error(log, "HTTP call to ServiceURL={} failed, returned statusCode={}", loggerArgumentArray);
                    } else {
                        JSONObject fileInfoResponseObj = null;

                        if (null != responseXML) {
                            LogSF.debug(log, "Secured Resource Service Response={}", responseXML);
                            fileInfoResponseObj = new JSONObject(responseXML);

                            if (null != fileInfoResponseObj.opt(FILE_INFO_RESPONSE)
                                && !StringUtils.isStringNullorEmpty(fileInfoResponseObj.opt(FILE_INFO_RESPONSE).toString())) {
                                JSONObject fileInfoResponse = fileInfoResponseObj.getJSONObject(FILE_INFO_RESPONSE);

                                if (null != fileInfoResponse.opt(ENCRYPTED_FILE_PATH)) {
                                    encryptedFilePath = fileInfoResponse.getString(ENCRYPTED_FILE_PATH);
                                }
                            }
                        }
                    }
                } catch (JSONException jsone) {
                    Object[] loggerInputArray = new Object[] { "JSONException occured while invoking createResource ", deleteFlag };
                    LogSF.error(log, jsone, "{} deleteFileFlag={}", loggerInputArray);
                } catch (UnsupportedEncodingException uee) {
                    Object[] loggerInputArray = new Object[] { "UnsupportedEncodingException occured while invoking createResource" };
                    LogSF.error(log, uee, "{}", loggerInputArray);
                } catch (HttpException htte) {
                    Object[] loggerInputArray = new Object[] { "IOException occured while invoking createResource", responseXML };
                    LogSF.error(log, htte, "{} Response Content={}", loggerInputArray);
                } catch (IOException ioe) {
                    Object[] loggerInputArray = new Object[] { "IOException occured while invoking createResource" };
                    LogSF.error(log, ioe, "{}", loggerInputArray);
                }
            } else {
                // set UnEncrypted pdfFilePath as encryptedFilePath
                encryptedFilePath = pdfFilePath;
            }
        }
        return encryptedFilePath;
    }

    /**
     * Method used to call the getpdf secured service API via HTTP client call
     * 
     * @since 27/09/2012
     * @param listingId
     *            - input Listing Id
     * @param eventId
     *            - input event Id
     * @param pdfEncryptedFilePath
     *            - input PDF Encrypted File Path
     * @return InputStream - return InputStream Object as response
     * @throws StrongAuthException
     */
    public static InputStream readResource(Long listingId, Long eventId, String pdfFilePath) throws StrongAuthException {

        InputStream pdfFileInputStreamObj = null;
        String cookieValue = null;
        HttpGet method = null ;
        if (null != listingId && null != eventId && null != pdfFilePath) {
            try {
                cookieValue = getStubCookieInformation();
                String responseXml = null;
                if(StringUtils.isStringNullorEmpty(cookieValue)){
                	StringBuilder getPdfAPIUrl = new StringBuilder(TICKETPROTECTION_API_URL).append(Constants.BACKWARD_SLASH).append(version)
                    .append(PDF_FILEPATH).append(listingId)
                    .append(Constants.BACKWARD_SLASH)
                    .append(eventId)
                    .append(Constants.BACKWARD_SLASH)
                    .append(URLEncoder.encode(pdfFilePath, "UTF-8"));
                    method = new HttpGet(getPdfAPIUrl.toString());
                    method.setHeader(SECURED_TICKET_TOKEN_HEADER, SECURED_TICKET_TOKEN_HEADER_VALUE);
                    long startTime = System.currentTimeMillis();
                    responseXml = HttpClient4Util.getFromUrl(method, CONNECTION_TIMEOUT, false, null);
                    LogSF.info(log,"Invoked readResource service with duration={}", (System.currentTimeMillis()- startTime));                	
                } else{
                	StringBuilder getPdfFileAPIUrl = new StringBuilder(TICKETPROTECTION_API_URL).append(Constants.BACKWARD_SLASH).append(version)
                    .append(PDF_PATH).append(listingId)
                    .append(Constants.BACKWARD_SLASH)
                    .append(eventId)
                    .append(Constants.BACKWARD_SLASH)
                    .append(URLEncoder.encode(pdfFilePath, "UTF-8"));                	
                	long startTime = System.currentTimeMillis();
                	responseXml = HttpClient4Util.getFromUrl(getPdfFileAPIUrl.toString(), CONNECTION_TIMEOUT, false,
                            cookieValue);
                	LogSF.info(log,"Invoked readResource service with duration={}", (System.currentTimeMillis()- startTime));
                }
                
                RequestContext requestContext = StubhubCredentials.getRequestContext();
                Long userId = null;
                Long stubnetUserId = null;
                if (null != requestContext) {
                    stubnetUserId = requestContext.getStubnetUserId();
                    userId = (null != requestContext.getUserId()) ? requestContext.getUserId() : getStubhubUserId(requestContext);
                    
                }
                Object[] loggerInputArray = new Object[] {userId,stubnetUserId,listingId ,eventId};
                LogSF.info(log,"getPdf service invoked by stubhubUserId={} / stubnetUserId={} for listingId={} eventId={}", loggerInputArray);
                
                if (null != responseXml) {
                    JSONObject fileInfoResponseObj = new JSONObject(responseXml);

                    if (null != fileInfoResponseObj.opt(FILE_INFO_RESPONSE)
                        && !StringUtils.isStringNullorEmpty(fileInfoResponseObj.opt(FILE_INFO_RESPONSE).toString())) {

                        JSONObject fileInfoResponse = fileInfoResponseObj.getJSONObject(FILE_INFO_RESPONSE);

                        if (null != fileInfoResponse.opt(FILE_BYTES_RESPONSE)) {
                            String fileContent = fileInfoResponse.getString(FILE_BYTES_RESPONSE);                            
                            byte[] fileBytes = fileContent.getBytes();
                            pdfFileInputStreamObj = new ByteArrayInputStream(Base64.decodeBase64(fileBytes));
                        } else if (null != fileInfoResponse.opt(ERROR_RESPONSE)
                            && !StringUtils.isStringNullorEmpty(fileInfoResponse.opt(ERROR_RESPONSE).toString())) {
                            JSONObject errorResponse = fileInfoResponse.getJSONObject(ERROR_RESPONSE);
                            if (null != errorResponse.opt(ERROR_DETAIL_RESPONSE)
                                && !StringUtils.isStringNullorEmpty(errorResponse.opt(ERROR_DETAIL_RESPONSE).toString())) {
                                JSONObject errorDetailResponse = errorResponse.getJSONObject(ERROR_DETAIL_RESPONSE);
                                String errorMsg = errorDetailResponse.getString(ERROR_MESSAGE);
                                String errorCode = errorDetailResponse.getString(ERROR_CODE);
                                throw new StrongAuthException(errorMsg, errorCode);
                            }
                        }
                    }
                }
            } catch (JSONException jsonException) {
                Object[] loggerInputArray = new Object[] { "JSONException occured while invoking readResource", listingId, eventId,
                                                          pdfFilePath };
                LogSF.error(log, jsonException, "{} listingId={} eventId={} pdfFilePath={}", loggerInputArray);
            } catch (IOException ioException) {
                Object[] loggerInputArray = new Object[] { "IOException occured while invoking readResource", listingId, eventId,
                                                          pdfFilePath };
                LogSF.error(log, ioException, "{} listingId={} eventId={} pdfFilePath={}", loggerInputArray);
            } finally {
                if(StringUtils.isStringNullorEmpty(cookieValue)){
                    removeSecuredHeader(method);
                }
            }
            
        }
        return pdfFileInputStreamObj;
    }
    
    /**
     * Method used to call the getpdf secured service API via HTTP client call
     * 
     * @since 26/11/2012
     * @param listingId
     *            - input Listing Id
     * @param eventId
     *            - input event Id
     * @param pdfFileName
     *            - input PDF Encrypted File Name
     * @return InputStream - return InputStream Object as response
     * @throws StrongAuthException
     */
    public static InputStream getPdf(String listingId, Long eventId, String pdfFileName) {

        InputStream pdfFileInputStreamObj = null;
        HttpGet method =  null;
        if (null != listingId && null != eventId && null != pdfFileName) {
            try {                
                String responseXml = null;
            	StringBuilder getPdfAPIUrl = new StringBuilder(TICKETPROTECTION_API_URL).append(Constants.BACKWARD_SLASH).append(version)
                .append(PDF_EXTERNAL_LISTING_FILE_PATH).append(listingId)
                .append(Constants.BACKWARD_SLASH)
                .append(eventId)
                .append(Constants.BACKWARD_SLASH)
                .append(URLEncoder.encode(pdfFileName, "UTF-8"));
            	method = new HttpGet(getPdfAPIUrl.toString());
                method.setHeader(SECURED_TICKET_TOKEN_HEADER, SECURED_TICKET_TOKEN_HEADER_VALUE);
                responseXml = HttpClient4Util.getFromUrl(method, CONNECTION_TIMEOUT, false, null);              	
                RequestContext requestContext = StubhubCredentials.getRequestContext();
                Long userId = null;
                Long stubnetUserId = null;
                if (null != requestContext) {
                    stubnetUserId = requestContext.getStubnetUserId();
                    userId = (null != requestContext.getUserId()) ? requestContext.getUserId() : getStubhubUserId(requestContext);
                    
                }
                Object[] loggerInputArray = new Object[] {userId,stubnetUserId,listingId ,eventId};
                LogSF.info(log,"getPdf service invoked by stubhubUserId={} / stubnetUserId={} for listingId={} eventId={}", loggerInputArray);
                
                if (null != responseXml) {
                    JSONObject fileInfoResponseObj = new JSONObject(responseXml);

                    if (null != fileInfoResponseObj.opt(FILE_INFO_RESPONSE)
                        && !StringUtils.isStringNullorEmpty(fileInfoResponseObj.opt(FILE_INFO_RESPONSE).toString())) {

                        JSONObject fileInfoResponse = fileInfoResponseObj.getJSONObject(FILE_INFO_RESPONSE);

                        if (null != fileInfoResponse.opt(FILE_BYTES_RESPONSE)) {
                            String fileContent = fileInfoResponse.getString(FILE_BYTES_RESPONSE);                            
                            byte[] fileBytes = fileContent.getBytes();
                            pdfFileInputStreamObj = new ByteArrayInputStream(Base64.decodeBase64(fileBytes));
                        }
                    }
                }
            } catch (JSONException jsonException) {
                Object[] loggerInputArray = new Object[] { "JSONException occured while invoking readResource", listingId, eventId,
                		pdfFileName };
                LogSF.error(log, jsonException, "{} listingId={} eventId={} pdfFilePath={}", loggerInputArray);
            } catch (IOException ioException) {
                Object[] loggerInputArray = new Object[] { "IOException occured while invoking readResource", listingId, eventId,
                		pdfFileName };
                LogSF.error(log, ioException, "{} listingId={} eventId={} pdfFilePath={}", loggerInputArray);
            } finally {
                removeSecuredHeader(method);
            }
        }
        return pdfFileInputStreamObj;
    }

    /**
     * Method retrieves the FileInfo records based on given fileInfoIds, by GET call
     * 
     * @since 26/09/2012
     * @param fileInfoIds
     * @return List<StubHubFile>
     * @throws StrongAuthException 
     * @throws UnauthorizedUserException 
     */
    public static List<StubHubFile> getFileInfoByIds(String fileInfoIds) throws StrongAuthException, UnauthorizedUserException {

        List<StubHubFile> stubHubFileList = null;

        if (null != fileInfoIds) {        	
        	Long userId = null;
            Long stubnetUserId = null;
            RequestContext requestContext = StubhubCredentials.getRequestContext();
            if (null != requestContext) {
            	stubnetUserId = requestContext.getStubnetUserId(); 
                userId = (null != requestContext.getUserId()) ? requestContext.getUserId() : getStubhubUserId(requestContext);                    
            }                
            Object[] loggerArray = new Object[] {userId,stubnetUserId};
            LogSF.info(log,"getFileInfoByIds invoked by stubhubUserId={} / stubnetUserId={} ", loggerArray);
            LogSF.debug(log, "Inside getFileInfoByIds fileInfoIds={} ", fileInfoIds);
            
            StringBuffer sbBNUrl = new StringBuffer();

            sbBNUrl.append(TICKETPROTECTION_API_URL).append(Constants.BACKWARD_SLASH).append(version).append(Constants.BACKWARD_SLASH).append(FILE_INFO)
                   .append(Constants.BACKWARD_SLASH).append(fileInfoIds);
            String cookieValue = getStubCookieInformation();
            try {
                long startTime = System.currentTimeMillis();
                String respXml = HttpClient4Util.getFromUrl(sbBNUrl.toString(), CONNECTION_TIMEOUT, false, cookieValue);
                LogSF.info(log,"Invoked geFileInfoById service with duration={}", (System.currentTimeMillis()- startTime));
                if (null != respXml) {
                    JSONObject fileInfoResponseObj = new JSONObject(respXml);

                    if (null != fileInfoResponseObj.opt(FILE_INFO_RESPONSE)
                        && !StringUtils.isStringNullorEmpty(fileInfoResponseObj.opt(FILE_INFO_RESPONSE).toString())) {
                        JSONObject fileInfoResponse = null;
                        fileInfoResponse = fileInfoResponseObj.getJSONObject(FILE_INFO_RESPONSE);
                        
                        if (null != fileInfoResponse.opt(FILE_INFO_LIST)
                            && !StringUtils.isStringNullorEmpty(fileInfoResponse.opt(FILE_INFO_LIST).toString())) {
                            List<Long> fileInfoIdList = CommonUtil.convertStringToLongList(fileInfoIds, ",");
                            if (null != fileInfoIdList && fileInfoIdList.size() > 1) {
                                JSONArray jsonArray = null;
                                jsonArray = fileInfoResponse.getJSONArray(FILE_INFO_LIST);
                                if (null != jsonArray) {
                                    stubHubFileList = new ArrayList<StubHubFile>();
                                    for (int index = 0; index < jsonArray.length(); index++) {
                                        JSONObject fileInfoObject = jsonArray.getJSONObject(index);
                                        if (null != fileInfoObject) {
                                            StubHubFile stubHubFile = new StubHubFile();
                                            
                                            if (null != fileInfoObject.opt(FILE_BYTES_RESPONSE)) {
                                                String fileContent = fileInfoObject.getString(FILE_BYTES_RESPONSE);                            
                                                byte[] fileBytes = fileContent.getBytes();
                                                stubHubFile.setDecryptedResource(new ByteArrayInputStream(Base64.decodeBase64(fileBytes)));
                                            }
                                            if (null != fileInfoObject.opt(FILE_PATH)
                                                && !StringUtils.isStringNullorEmpty(fileInfoObject.opt(FILE_PATH).toString())) {
                                                stubHubFile.setFilePath(fileInfoObject.getString(FILE_PATH));
                                            }
                                            if (null != fileInfoObject.opt(FILE_NAME)
                                                && !StringUtils.isStringNullorEmpty(fileInfoObject.opt(FILE_NAME).toString())) {
                                                stubHubFile.setFileName(fileInfoObject.getString(FILE_NAME));

                                            }
                                            stubHubFileList.add(stubHubFile);
                                        }
                                    }
                                }
                            } else {
                                JSONObject fileInfoObject = fileInfoResponse.getJSONObject(FILE_INFO_LIST);
                                if (null != fileInfoObject) {
                                    stubHubFileList = new ArrayList<StubHubFile>();
                                    StubHubFile stubHubFile = new StubHubFile();
                                    
                                    if (null != fileInfoObject.opt(FILE_BYTES_RESPONSE)) {
                                        String fileContent = fileInfoObject.getString(FILE_BYTES_RESPONSE);                            
                                        byte[] fileBytes = fileContent.getBytes();
                                        stubHubFile.setDecryptedResource(new ByteArrayInputStream(Base64.decodeBase64(fileBytes)));
                                    }
                                    if (null != fileInfoObject.opt(FILE_PATH)
                                        && !StringUtils.isStringNullorEmpty(fileInfoObject.opt(FILE_PATH).toString())) {
                                        stubHubFile.setFilePath(fileInfoObject.getString(FILE_PATH));
                                    }
                                    if (null != fileInfoObject.opt(FILE_NAME)
                                        && !StringUtils.isStringNullorEmpty(fileInfoObject.opt(FILE_NAME).toString())) {
                                        stubHubFile.setFileName(fileInfoObject.getString(FILE_NAME));

                                    }
                                    stubHubFileList.add(stubHubFile);
                                }
                            }
                        } else if (null != fileInfoResponse.opt(ERROR_RESPONSE)
                                && !StringUtils.isStringNullorEmpty(fileInfoResponse.opt(ERROR_RESPONSE).toString())) {
                            JSONObject errorResponse = fileInfoResponse.getJSONObject(ERROR_RESPONSE);
                            if (null != errorResponse.opt(ERROR_DETAIL_RESPONSE)
                                && !StringUtils.isStringNullorEmpty(errorResponse.opt(ERROR_DETAIL_RESPONSE).toString())) {
                                JSONObject errorDetailResponse = errorResponse.getJSONObject(ERROR_DETAIL_RESPONSE);
                                String errorMsg = errorDetailResponse.getString(ERROR_MESSAGE);
                                String errorCode = errorDetailResponse.getString(ERROR_CODE);
                                if(errorCode.contains("UNAUTHORIZED USER")) {
                                    throw new UnauthorizedUserException("UNAUTHORIZED USER");
                                } else {
                                    throw new StrongAuthException(errorMsg, errorCode);
                                }
                            }
                        }
                    }
                }
            } catch (HttpException httpe) {
                Object[] loggerInputArray = new Object[] { "HttpException occured while invoking getFileInfoByIds" };
                LogSF.error(log, httpe, "{}", loggerInputArray);
            } catch (JSONException jsone) {
                Object[] loggerInputArray = new Object[] { "JSONException occured while invoking getFileInfoByIds ", fileInfoIds };
                LogSF.error(log, jsone, "{} FileInfoIds={}", loggerInputArray);
            } catch (IOException ioe) {
                Object[] loggerInputArray = new Object[] { "IOException occured while invoking getFileInfoByIds" };
                LogSF.error(log, ioe, "{}", loggerInputArray);
            }
        }
        return stubHubFileList;
    }
    
    /**
     * Method retrieves the FileInfo records based on given fileInfoIds, by GET call
     * 
     * @since 26/09/2012
     * @param fileInfoIds
     * @return List<StubHubFile>
     * @throws StrongAuthException 
     * @throws UnauthorizedUserException 
     */
    public static List<StubHubFile> getFileInfoByIdsForMobileApp(String fileInfoIds) throws StrongAuthException, UnauthorizedUserException {

    	List<StubHubFile> stubHubFileList = null;
    	HttpGet method = null;
    	try {
    		if (null != fileInfoIds) {
    			StringBuilder ticketProtectionServiceUrl = new StringBuilder(TICKETPROTECTION_API_URL)
    			.append(Constants.BACKWARD_SLASH).append(version).append(Constants.BACKWARD_SLASH).append(FILE_INFO).
    			append(Constants.BACKWARD_SLASH).append(MOBILE_APP).append(Constants.BACKWARD_SLASH).append(fileInfoIds);

    			LogSF.info(log, "Invoking getFileInfoByIdsForMobileApp with url={}", ticketProtectionServiceUrl);
    			long startTime = System.currentTimeMillis();
    			method = new HttpGet(ticketProtectionServiceUrl.toString());
    			method.setHeader(SECURED_TICKET_TOKEN_HEADER, SECURED_TICKET_TOKEN_HEADER_VALUE);
    			String respXml = HttpClient4Util.getFromUrl(method, CONNECTION_TIMEOUT, false, null);
    			LogSF.info(log,"Invoked getFileInfoByIdsForMobileApp service with duration={}", (System.currentTimeMillis()- startTime));
    			LogSF.debug(log, "Inside getFileInfoByIdsForMobileApp fileInfoIds={} ", fileInfoIds);
    			if (null != respXml) {
    				JSONObject fileInfoResponseObj = new JSONObject(respXml);

    				if (null != fileInfoResponseObj.opt(FILE_INFO_RESPONSE)
    						&& !StringUtils.isStringNullorEmpty(fileInfoResponseObj.opt(FILE_INFO_RESPONSE).toString())) {
    					JSONObject fileInfoResponse = null;
    					fileInfoResponse = fileInfoResponseObj.getJSONObject(FILE_INFO_RESPONSE);

    					if (null != fileInfoResponse.opt(FILE_INFO_LIST)
    							&& !StringUtils.isStringNullorEmpty(fileInfoResponse.opt(FILE_INFO_LIST).toString())) {
    						List<Long> fileInfoIdList = CommonUtil.convertStringToLongList(fileInfoIds, ",");
    						if (null != fileInfoIdList && fileInfoIdList.size() > 1) {
    							JSONArray jsonArray = null;
    							jsonArray = fileInfoResponse.getJSONArray(FILE_INFO_LIST);
    							if (null != jsonArray) {
    								stubHubFileList = new ArrayList<StubHubFile>();
    								for (int index = 0; index < jsonArray.length(); index++) {
    									JSONObject fileInfoObject = jsonArray.getJSONObject(index);
    									if (null != fileInfoObject) {
    										StubHubFile stubHubFile = new StubHubFile();

    										if (null != fileInfoObject.opt(FILE_BYTES_RESPONSE)) {
    											String fileContent = fileInfoObject.getString(FILE_BYTES_RESPONSE);                            
    											byte[] fileBytes = fileContent.getBytes();
    											stubHubFile.setDecryptedResource(new ByteArrayInputStream(Base64.decodeBase64(fileBytes)));
    										}
    										if (null != fileInfoObject.opt(FILE_PATH)
    												&& !StringUtils.isStringNullorEmpty(fileInfoObject.opt(FILE_PATH).toString())) {
    											stubHubFile.setFilePath(fileInfoObject.getString(FILE_PATH));
    										}
    										if (null != fileInfoObject.opt(FILE_NAME)
    												&& !StringUtils.isStringNullorEmpty(fileInfoObject.opt(FILE_NAME).toString())) {
    											stubHubFile.setFileName(fileInfoObject.getString(FILE_NAME));

    										}
    										stubHubFileList.add(stubHubFile);
    									}
    								}
    							}
    						} else {
    							JSONObject fileInfoObject = fileInfoResponse.getJSONObject(FILE_INFO_LIST);
    							if (null != fileInfoObject) {
    								stubHubFileList = new ArrayList<StubHubFile>();
    								StubHubFile stubHubFile = new StubHubFile();

    								if (null != fileInfoObject.opt(FILE_BYTES_RESPONSE)) {
    									String fileContent = fileInfoObject.getString(FILE_BYTES_RESPONSE);                            
    									byte[] fileBytes = fileContent.getBytes();
    									stubHubFile.setDecryptedResource(new ByteArrayInputStream(Base64.decodeBase64(fileBytes)));
    								}
    								if (null != fileInfoObject.opt(FILE_PATH)
    										&& !StringUtils.isStringNullorEmpty(fileInfoObject.opt(FILE_PATH).toString())) {
    									stubHubFile.setFilePath(fileInfoObject.getString(FILE_PATH));
    								}
    								if (null != fileInfoObject.opt(FILE_NAME)
    										&& !StringUtils.isStringNullorEmpty(fileInfoObject.opt(FILE_NAME).toString())) {
    									stubHubFile.setFileName(fileInfoObject.getString(FILE_NAME));

    								}
    								stubHubFileList.add(stubHubFile);
    							}
    						}
    					} else if (null != fileInfoResponse.opt(ERROR_RESPONSE)
    							&& !StringUtils.isStringNullorEmpty(fileInfoResponse.opt(ERROR_RESPONSE).toString())) {
    						JSONObject errorResponse = fileInfoResponse.getJSONObject(ERROR_RESPONSE);
    						if (null != errorResponse.opt(ERROR_DETAIL_RESPONSE)
    								&& !StringUtils.isStringNullorEmpty(errorResponse.opt(ERROR_DETAIL_RESPONSE).toString())) {
    							JSONObject errorDetailResponse = errorResponse.getJSONObject(ERROR_DETAIL_RESPONSE);
    							String errorMsg = errorDetailResponse.getString(ERROR_MESSAGE);
    							String errorCode = errorDetailResponse.getString(ERROR_CODE);
    							if(errorCode.contains("UNAUTHORIZED USER")) {
    								throw new UnauthorizedUserException("UNAUTHORIZED USER");
    							} else {
    								throw new StrongAuthException(errorMsg, errorCode);
    							}
    						}
    					}
    				}
    			}
    		} 
    	}catch (HttpException httpe) {
    			Object[] loggerInputArray = new Object[] { "HttpException occured while invoking getFileInfoByIds" };
    			LogSF.error(log, httpe, "{}", loggerInputArray);
    		} catch (JSONException jsone) {
    			Object[] loggerInputArray = new Object[] { "JSONException occured while invoking getFileInfoByIds ", fileInfoIds };
    			LogSF.error(log, jsone, "{} FileInfoIds={}", loggerInputArray);
    		} catch (IOException ioe) {
    			Object[] loggerInputArray = new Object[] { "IOException occured while invoking getFileInfoByIds" };
    			LogSF.error(log, ioe, "{}", loggerInputArray);
    		}finally{
    			removeSecuredHeader(method);
    		}

    		return stubHubFileList;
    }

    /**
     * Method used to upload the passed input stream as file at repository path
     * 
     * @since 10/10/2012
     * 
     * @param listingId
     *            -input listingId
     * @param eventId
     *            -input eventId
     * @param uploadedFileName
     *            -input filename of the uploaded file
     * @param inputStream
     *            -input InputStream of the file to be uploaded
     * @return UploadFileInfo - Returns filePath, uploadFlag and pageCount as UploadFileInfo
     */
    public static UploadFileInfo uploadFile(Long listingId, Long eventId, String uploadedFileName, InputStream inputStream) {
        return upload(listingId, eventId, uploadedFileName, inputStream, false, true);
    }

    /**
     * Method used to upload the passed input buffered png image at nas repository path
     * 
     * @since 15/10/2012
     * 
     * @param listingId
     *            -input listingId
     * @param eventId
     *            -input eventId
     * @param imageFileName
     *            -input filename of the uploaded image file
     * @param inputImage
     *            -input InputImage of the file to be uploaded
     * @return String -Returns path of the uploaded image
     */
    public static String uploadImage(Long listingId, Long eventId, String imageFileName, BufferedImage inputImage) {

        String uploadedImagePath = null;

        if (null != listingId && null != eventId && !StringUtils.isStringNullorEmpty(imageFileName) && null != inputImage) {

            StringBuilder uploadImageServiceUrl = new StringBuilder(TICKETPROTECTION_API_URL)
            .append(Constants.BACKWARD_SLASH).append(version).append(GET_UPLOAD_IMAGE_API_PATH);
            LogSF.debug(log, "Invoking uploadImage service with url={}", uploadImageServiceUrl);
            int statusCode = 0;
            String response = null;
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ImageIO.write(inputImage, "png", outputStream);
                byte[] imageBytes = outputStream.toByteArray();

                String imageInString = new String(Base64.encodeBase64(imageBytes));

                JSONObject imageInputStreamJSON = new JSONObject();
                imageInputStreamJSON.put(IMAGE, imageInString);
                imageInputStreamJSON.put(LISTING_ID, listingId);
                imageInputStreamJSON.put(EVENT_ID, eventId);
                imageInputStreamJSON.put(UPLOADED_IMAGE_NAME, imageFileName);

                JSONObject uploadImageRequestJSON = new JSONObject();
                uploadImageRequestJSON.put(UPLOAD_IMAGE_REQUEST, imageInputStreamJSON);

                final HttpPost method = new HttpPost(uploadImageServiceUrl.toString());
                final StringEntity entity = new StringEntity(uploadImageRequestJSON.toString(), CHARSET);
                entity.setContentType(JSON_CONTENT_TYPE);
                method.setEntity(entity);
                method.addHeader(Constants.COOKIE.toLowerCase(), getStubCookieInformation());
                method.getParams().setIntParameter(HttpMethodParams.SO_TIMEOUT, CONNECTION_TIMEOUT);
                long startTime = System.currentTimeMillis();
                HttpClient httpClient = HttpClient4Util.getHttpClient();
                HttpResponse httpResponse = httpClient.execute(method);
                LogSF.info(log,"Invoked uploadImage  service with duration={}", (System.currentTimeMillis()- startTime));
                statusCode = httpResponse.getStatusLine().getStatusCode();
                LogSF.info(log, "Simple HttpResponse StatusCode={}", statusCode);

                if (HttpStatus.SC_OK != statusCode) {
                    Object[] statusArray = new Object[] { statusCode };
                    LogSF.error(log, "Call to uploadImage service failed, returned statuscode={}", statusArray);
                } else {
                    if (null != httpResponse) {
                        HttpEntity entityResponse = httpResponse.getEntity();
                        if (null != entityResponse) {
                            response = EntityUtils.toString(entityResponse, "UTF-8");
                            JSONObject uploadImageResponseObj = new JSONObject(response);
                            if (null != uploadImageResponseObj.opt(UPLOAD_IMAGE_RESPONSE)
                                && !StringUtils.isStringNullorEmpty(uploadImageResponseObj.opt(UPLOAD_IMAGE_RESPONSE).toString())) {

                                JSONObject uploadImageResponse = uploadImageResponseObj.getJSONObject(UPLOAD_IMAGE_RESPONSE);
                                if (null != uploadImageResponse.opt(UPLOADED_IMAGE_PATH)) {
                                    uploadedImagePath = uploadImageResponse.getString(UPLOADED_IMAGE_PATH);
                                }
                            }
                        }
                    }
                }
            } catch (JSONException jsonException) {
                LogSF.error(log, jsonException, "JSONException occured while uploading image at nas repository", null);
            } catch (UnsupportedEncodingException unsupportedEncodingException) {
                LogSF.error(log, unsupportedEncodingException,
                            "UnsupportedEncodingException occured while uploading image at nas repository", null);
            } catch (HttpException httpException) {
                LogSF.error(log, httpException, "HttpException occured while uploading image at nas repository", null);
            } catch (IOException ioException) {
                LogSF.error(log, ioException, "IOException occured while uploading image at nas repository", null);
            }
        }
        LogSF.debug(log, "After uploadImage service call, uploadedImagePath={}", uploadedImagePath);
        return uploadedImagePath;
    }

    /**
     * Method used to call the deleteFile secured service API via JSON Object PostMethod request
     * 
     * @since 12/10/2012
     * @param filePath
     *            - input filePath
     * @return boolean - returns true if and only if the file is successfully deleted; false otherwise
     */
    public static boolean deleteFile(String filePath) {

        boolean isFileDeleted = false;

        if (!StringUtils.isStringNullorEmpty(filePath)) {

            StringBuilder deleteFileServiceUrl = new StringBuilder(TICKETPROTECTION_API_URL).append(Constants.BACKWARD_SLASH)
                                                                                            .append(version)
                                                                                            .append(DELETE_FILE_API_PATH);

            LogSF.debug(log, "deleteFileServiceUrl={}", deleteFileServiceUrl);

            int statusCode = 0;
            String response = null;
            try {

                JSONObject filePathJSON = new JSONObject();
                filePathJSON.put(FILE_PATH, filePath);

                JSONObject fileInfoRequest = new JSONObject();
                fileInfoRequest.put(FILE_INFO_REQUEST, filePathJSON);
                final HttpPost method = new HttpPost(deleteFileServiceUrl.toString());
                final StringEntity entity = new StringEntity(fileInfoRequest.toString(), "UTF-8");
                entity.setContentType("application/json");
                method.setEntity(entity);
                method.setHeader(SECURED_TICKET_TOKEN_HEADER, SECURED_TICKET_TOKEN_HEADER_VALUE);
                method.getParams().setIntParameter(HttpMethodParams.SO_TIMEOUT, CONNECTION_TIMEOUT);
                HttpClient httpClient = HttpClient4Util.getHttpClient();
                HttpResponse httpResponse = httpClient.execute(method);
                
                statusCode = httpResponse.getStatusLine().getStatusCode();

                if (HttpStatus.SC_OK != statusCode) {
                    Object[] statusArray = new Object[] { statusCode };
                    LogSF.error(log, "Invoked deleteFile service with statusCode={}", statusArray);
                    throw new HttpResponseException(statusCode, "http response statusCode=" + statusCode);
                } else {
                    HttpEntity entityResponse = httpResponse.getEntity();
                    if (null != entityResponse)
                        response = EntityUtils.toString(entityResponse, "UTF-8");

                    JSONObject fileInfoResponseObj = null;

                    if (null != response) {

                        fileInfoResponseObj = new JSONObject(response);
                        if (null != fileInfoResponseObj.opt(FILE_INFO_RESPONSE)
                            && !StringUtils.isStringNullorEmpty(fileInfoResponseObj.opt(FILE_INFO_RESPONSE).toString())) {
                            JSONObject fileInfoResponse = fileInfoResponseObj.getJSONObject(FILE_INFO_RESPONSE);

                            if (null != fileInfoResponse.opt(DELETE_FILE_FLAG_RESPONSE)) {
                                isFileDeleted = fileInfoResponse.getBoolean(DELETE_FILE_FLAG_RESPONSE);
                                LogSF.debug(log, "isFileDeleted={} for deleteFile api call with filePath={}", isFileDeleted,
                                            filePath);
                            }
                        }
                    }
                }
            } catch (JSONException jsone) {
                LogSF.error(log, jsone, "JSONException occured while invoking deleteFile", null);
            } catch (UnsupportedEncodingException uee) {
                LogSF.error(log, uee, "UnsupportedEncodingException occured while invoking deleteFile", null);
            } catch (HttpException htte) {
                Object[] loggerInputArray = new Object[] { response };
                LogSF.error(log, htte, "HttpException occured while invoking deleteFile responseContent={}", loggerInputArray);
            } catch (IOException ioe) {
                LogSF.error(log, ioe, "IOException occured while invoking deleteFile", null);
            }
        }
        return isFileDeleted;
    }

    /**
     * Method used to upload the passed input stream as file at repository path
     * 
     * @since 23/10/2012
     * 
     * @param listingId
     *            -input listingId
     * @param eventId
     *            -input eventId
     * @param uploadedFileName
     *            -input filename of the uploaded file
     * @param inputStream
     *            -input InputStream of the file to be uploaded
     * @param isExternalLisitngRequest
     *            - input to identify the external listing request           
     * @return UploadFileInfo - Returns filePath, uploadFlag and pageCount as UploadFileInfo
     */
    public static UploadFileInfo uploadFileInfo(Long listingId, Long eventId, String uploadedFileName, InputStream inputStream, boolean isExternalLisitngRequest) {
        return upload(listingId, eventId, uploadedFileName, inputStream, isExternalLisitngRequest, false);
    }

    /**
     * Method used to upload the passed input stream as file at repository path
     * 
     * @param listingId
     *            -input listingId
     * @param eventId
     *            -input eventId
     * @param uploadedFileName
     *            -input filename of the uploaded file
     * @param inputStream
     *            -input InputStream of the file to be uploaded
     * @param isCookiesAvailable
     *            - input boolean to check if cookies available or not
     * @param isExternalLisitngRequest
     *            - input to identify the external listing request           
     * @return UploadFileInfo - Returns filePath, uploadFlag and pageCount as UploadFileInfo
     */
    private static UploadFileInfo upload(Long listingId, Long eventId, String uploadedFileName, InputStream inputStream,
                                         boolean isExternalLisitngRequest, Boolean isCookiesAvailable) {
        UploadFileInfo uploadFileInfo = new UploadFileInfo();
        if (null != listingId && null != eventId && !StringUtils.isStringNullorEmpty(uploadedFileName) && null != inputStream) {
            StringBuilder uploadFileServiceUrl = null;
            if (isCookiesAvailable) {
                uploadFileServiceUrl = new StringBuilder(TICKETPROTECTION_API_URL)
                            .append(Constants.BACKWARD_SLASH).append(version).append(GET_UPLOAD_FILE_API_PATH);
            } else {
                uploadFileServiceUrl = new StringBuilder(TICKETPROTECTION_API_URL)
                            .append(Constants.BACKWARD_SLASH).append(version).append(GET_UPLOAD_FILE_INFO_API_PATH);
            }
            LogSF.debug(log, "Invoking uploadFile service with url={}", uploadFileServiceUrl);
            int statusCode = 0;
            String response = null;
            try {
                JSONObject fileInputStreamJSON = new JSONObject();
                byte[] fileBytes = IOUtils.toByteArray(inputStream);

                String fileInString = new String(Base64.encodeBase64(fileBytes));
                fileInputStreamJSON.put(PDF, fileInString);
                fileInputStreamJSON.put(LISTING_ID, listingId);
                fileInputStreamJSON.put(EVENT_ID, eventId);
                fileInputStreamJSON.put(UPLOADED_FILE_NAME, uploadedFileName);
                fileInputStreamJSON.put(IS_EXTERNAL_LISTING_REQ, isExternalLisitngRequest);

                JSONObject uploadFileRequestJSON = new JSONObject();
                uploadFileRequestJSON.put(UPLOAD_FILE_REQUEST, fileInputStreamJSON);
                
                final HttpPost method = new HttpPost(uploadFileServiceUrl.toString());
                final StringEntity entity = new StringEntity(uploadFileRequestJSON.toString(), CHARSET);
                entity.setContentType(JSON_CONTENT_TYPE);
                method.setEntity(entity);
                if (isCookiesAvailable) {
                    method.addHeader(Constants.COOKIE.toLowerCase(), getStubCookieInformation());
                } else {
                    method.addHeader(SECURED_TICKET_TOKEN_HEADER, SECURED_TICKET_TOKEN_HEADER_VALUE);
                }
                method.getParams().setIntParameter(HttpMethodParams.SO_TIMEOUT, CONNECTION_TIMEOUT);
                long startTime = System.currentTimeMillis();
                HttpClient httpClient = HttpClient4Util.getHttpClient();
                HttpResponse httpResponse = httpClient.execute(method);
                LogSF.info(log,"Invoked UploadFileInfo service with duration={}", (System.currentTimeMillis()- startTime));
                statusCode = httpResponse.getStatusLine().getStatusCode();
                LogSF.info(log, "Simple HttpResponse StatusCode={}", statusCode);
                
                if (HttpStatus.SC_OK != statusCode) {
                    Object[] statusArray = new Object[] { statusCode };
                    LogSF.error(log, "Call to uploadFile service failed, returned statusCode={}", statusArray);
                } else {
                    if (null != httpResponse) {
                        HttpEntity entityResponse = httpResponse.getEntity();
                        if (null != entityResponse) {
                            response = EntityUtils.toString(entityResponse, "UTF-8");
                            JSONObject uploadFileResponseObj = new JSONObject(response);
                            if (null != uploadFileResponseObj.opt(UPLOAD_FILE_RESPONSE)
                                && !StringUtils.isStringNullorEmpty(uploadFileResponseObj.opt(UPLOAD_FILE_RESPONSE).toString())) {
                                JSONObject uploadFileJSONResponse = uploadFileResponseObj.getJSONObject(UPLOAD_FILE_RESPONSE);

                                if (null != uploadFileJSONResponse.opt(UPLOADED_FILE_PATH)) {
                                    uploadFileInfo.setUploadedFilePath(uploadFileJSONResponse.getString(UPLOADED_FILE_PATH));
                                }
                                if (null != uploadFileJSONResponse.opt(UPLOADED_FILE_RESPONSE_FLAG)) {
                                    uploadFileInfo.setUploadSuccessful(uploadFileJSONResponse.getBoolean(UPLOADED_FILE_RESPONSE_FLAG));
                                }
                                if (null != uploadFileJSONResponse.opt(UPLOADED_FILE_PAGE_COUNT)) {
                                    uploadFileInfo.setPageCount(uploadFileJSONResponse.getInt(UPLOADED_FILE_PAGE_COUNT));
                                }
                                if (null != uploadFileJSONResponse.opt(UPLOAD_FILE_SIZE)) {
                                    uploadFileInfo.setUploadedFileSize(uploadFileJSONResponse.getLong(UPLOAD_FILE_SIZE));
                                }
                            }
                        }
                    }
                }
            } catch (JSONException jsonException) {
                LogSF.error(log, jsonException, "JSONException occured while uploading file at nas repository", null);
            } catch (UnsupportedEncodingException unsupportedEncodingException) {
                LogSF.error(log, unsupportedEncodingException,
                            "UnsupportedEncodingException occured while uploading file at nas repository", null);
            } catch (HttpException httpException) {
                LogSF.error(log, httpException, "HttpException occured while uploading file at nas repository", null);
            } catch (IOException ioException) {
                LogSF.error(log, ioException, "IOException occured while uploading file at nas repository", null);
            }
        }
        return uploadFileInfo;
    }

    /**
     * Method used to call the getImage secured service API via HTTP client call
     * 
     * @since 29/10/2012
     * @param listingId
     *            - input Listing Id
     * @param eventId
     *            - input event Id
     * @param imageFilePath
     *            - input Image File Path
     * @return InputStream - return Image InputStream Object as response
     */
    public static InputStream readImage(Long listingId, Long eventId, String imageFilePath) {

        InputStream imageInputStreamObj = null;
        if (null != listingId && null != eventId && null != imageFilePath) {
            try {

                StringBuilder readImageURI = new StringBuilder(TICKETPROTECTION_API_URL)
                                                                                        .append(Constants.BACKWARD_SLASH).append(version)
                                                                                        .append(IMAGE_PATH).append(listingId)
                                                                                        .append(Constants.BACKWARD_SLASH)
                                                                                        .append(eventId)
                                                                                        .append(Constants.BACKWARD_SLASH)
                                                                                        .append(imageFilePath);
                String cookieValue = getStubCookieInformation();
                String responseXml = HttpClient4Util.getFromUrl(readImageURI.toString(), CONNECTION_TIMEOUT, false, cookieValue);
                RequestContext requestContext = StubhubCredentials.getRequestContext();
                Long userId = null;
                Long stubnetUserId = null;
                if (null != requestContext) {
                    stubnetUserId = requestContext.getStubnetUserId();
                    userId = (null != requestContext.getUserId()) ? requestContext.getUserId() : getStubhubUserId(requestContext);
                    
                }
                Object[] loggerInputArray = new Object[] {userId,stubnetUserId,listingId ,eventId};
                LogSF.info(log,"getImage service invoked by stubhubUserId={} / stubnetUserId={} for listingId={} eventId={}", loggerInputArray);
                if (null != responseXml) {
                    JSONObject imageResponseObj = new JSONObject(responseXml);

                    if (null != imageResponseObj.opt(IMAGE_RESPONSE)
                        && !StringUtils.isStringNullorEmpty(imageResponseObj.opt(IMAGE_RESPONSE).toString())) {

                        JSONObject imageResponse = imageResponseObj.getJSONObject(IMAGE_RESPONSE);

                        if (null != imageResponse.opt(IMAGE_BYTES_RESPONSE)) {
                            String imageContent = imageResponse.getString(IMAGE_BYTES_RESPONSE);
                            byte[] imageBytes = imageContent.getBytes();
                            imageInputStreamObj = new ByteArrayInputStream(Base64.decodeBase64(imageBytes));
                        }
                    }
                }
            } catch (FileNotFoundException fileNotFoundException) {
                Object[] loggerInputArray = new Object[] { "FileNotFoundException occured while invoking readImage", listingId,
                                                          eventId, imageFilePath };
                LogSF.error(log, fileNotFoundException, "{} listingId={} eventId={} imageFilePath={}", loggerInputArray);
            } catch (JSONException jsonException) {
                Object[] loggerInputArray = new Object[] { "JSONException occured while invoking readImage", listingId, eventId,
                                                          imageFilePath };
                LogSF.error(log, jsonException, "{} listingId={} eventId={} imageFilePath={}", loggerInputArray);
            } catch (IOException ioException) {
                Object[] loggerInputArray = new Object[] { "IOException occured while invoking readImage", listingId, eventId,
                                                          imageFilePath };
                LogSF.error(log, ioException, "{} listingId={} eventId={} imageFilePath={}", loggerInputArray);
            }
        }
        return imageInputStreamObj;
    }

    /**
     * This method is used to ping the PDF Encryption service (CDA)
     * 
     * @since 23/10/2012
     * 
     * @return pingResponse
     * 
     */
    public static String pingCDA() {
        StringBuilder pingCDAUrl = new StringBuilder(TICKETPROTECTION_API_URL)
                        .append(Constants.BACKWARD_SLASH).append(version).append(CDAPING_PATH);
        String pingResponse = "";
        try {
        	String responseXml = HttpClient4Util.getFromUrl(pingCDAUrl.toString(), CONNECTION_TIMEOUT, false, null);
        	if (null != responseXml) {
                JSONObject pingRessponseObj;
					pingRessponseObj = new JSONObject(responseXml);

                if (null != pingRessponseObj.opt("CDAPingResponse")
                    && !StringUtils.isStringNullorEmpty(pingRessponseObj.opt("CDAPingResponse").toString())) {
                    JSONObject pingResponseObj = pingRessponseObj.getJSONObject("CDAPingResponse");

                    if (null != pingResponseObj.opt("cdaResponse")) {
                    	pingResponse = pingResponseObj.opt("cdaResponse").toString();
                    }
                }
            }
        }catch (JSONException je) {
            LogSF.error(log, je, "JSONException occured while invoking pingCDA", null);
        }catch (IOException ioe) {
            LogSF.error(log, ioe, "IOException occured while invoking pingCDA", null);
        }
        return pingResponse;
    }

    /**
     * This method is used to ping the PDF Encryption service (CDA)
     * 
     * @since 23/10/2012
     * 
     * @return
     * 
     */
    public static void pingSKA() {
        LogSF.info(log, "Ping SKA, Start", null);
        StringBuilder pingSKAUrl = new StringBuilder(TICKETPROTECTION_API_URL)
                    .append(Constants.BACKWARD_SLASH).append(version).append(SKAPING_PATH);
        String cookieValue = getStubCookieInformation();
        try {
            HttpClient4Util.getFromUrl(pingSKAUrl.toString(), CONNECTION_TIMEOUT, false, cookieValue);
            LogSF.info(log, "Ping SKA, Success ", null);
        } catch (IOException ioe) {
            LogSF.error(log, ioe, "IOException occured while invoking pingSKA", null);
        }
    }
    
    /**
     * Method used to upload the file for the given page index
     * 
     * @param listingId
     *            -input listingId
     * @param eventId
     *            -input eventId
     * @param uploadedFileName
     *            -input filename of the uploaded file
     * @param pageIndex
     *            -input pageIndex
     * @return UploadFileInfo - Returns filePath, uploadFlag as UploadFileInfo
     */
    public static UploadFileInfo uploadSplitFile(Long listingId, Long eventId, String uploadedFileName, int pageIndex) {
        UploadFileInfo uploadFileInfo = new UploadFileInfo();
        if (null != listingId && null != eventId && !StringUtils.isStringNullorEmpty(uploadedFileName)) {
            StringBuilder uploadSplitFileServiceUrl = null;
			uploadSplitFileServiceUrl = new StringBuilder(
					TICKETPROTECTION_API_URL).append(Constants.BACKWARD_SLASH).append(version)
					.append(GET_UPLOAD_SPLIT_FILE_INFO_API_PATH)
					.append(listingId).append(Constants.BACKWARD_SLASH)
					.append(eventId).append(Constants.BACKWARD_SLASH)
					.append(uploadedFileName).append(Constants.BACKWARD_SLASH)
					.append(pageIndex);
            LogSF.debug(log, "Invoking uploadSplitFile service with url={}", uploadSplitFileServiceUrl);
            int statusCode = 0;
            String response = null;
            try {
                final HttpPost method = new HttpPost(uploadSplitFileServiceUrl.toString());
                method.addHeader(SECURED_TICKET_TOKEN_HEADER, SECURED_TICKET_TOKEN_HEADER_VALUE);
                method.getParams().setIntParameter(HttpMethodParams.SO_TIMEOUT, CONNECTION_TIMEOUT);
                long startTime = System.currentTimeMillis();
                HttpClient httpClient = HttpClient4Util.getHttpClient();
                HttpResponse httpResponse = httpClient.execute(method);
                LogSF.info(log,"Invoked uploadSplitFile service with duration={}", (System.currentTimeMillis()- startTime));
                statusCode = httpResponse.getStatusLine().getStatusCode();
                LogSF.info(log, "Simple HttpResponse StatusCode={}", statusCode);

                if (HttpStatus.SC_OK != statusCode) {
                    Object[] statusArray = new Object[] { statusCode };
                    LogSF.error(log, "Call to uploadSplitFile service failed, returned statusCode={}", statusArray);
                } else {
                    if (null != httpResponse) {
                        HttpEntity entityResponse = httpResponse.getEntity();
                        if (null != entityResponse) {
                            response = EntityUtils.toString(entityResponse, "UTF-8");
                            JSONObject uploadFileResponseObj = new JSONObject(response);
                            if (null != uploadFileResponseObj.opt(UPLOAD_FILE_RESPONSE)
                                && !StringUtils.isStringNullorEmpty(uploadFileResponseObj.opt(UPLOAD_FILE_RESPONSE).toString())) {
                                JSONObject uploadFileJSONResponse = uploadFileResponseObj.getJSONObject(UPLOAD_FILE_RESPONSE);

                                if (null != uploadFileJSONResponse.opt(UPLOADED_FILE_PATH)) {
                                    uploadFileInfo.setUploadedFilePath(uploadFileJSONResponse.getString(UPLOADED_FILE_PATH));
                                }
                                if (null != uploadFileJSONResponse.opt(UPLOADED_FILE_RESPONSE_FLAG)) {
                                    uploadFileInfo.setUploadSuccessful(uploadFileJSONResponse.getBoolean(UPLOADED_FILE_RESPONSE_FLAG));
                                }
                                if (null != uploadFileJSONResponse.opt(ENCRYPTED_FILE_PATH)) {
                                    uploadFileInfo.setEncryptedFilePath(uploadFileJSONResponse.getString(ENCRYPTED_FILE_PATH));
                                }
                                if (null != uploadFileJSONResponse.opt(ENCRYPTED_FILE_TOKEN)) {
                                    uploadFileInfo.setEncryptedFileToken(uploadFileJSONResponse.getString(ENCRYPTED_FILE_TOKEN));
                                }
                                if (null != uploadFileJSONResponse.opt(UPLOADED_FILE_SIZE)) {
                                    uploadFileInfo.setUploadedFileSize(uploadFileJSONResponse.getLong(UPLOADED_FILE_SIZE));
                                }
                            }
                        }
                    }
                }
            } catch (JSONException jsonException) {
                LogSF.error(log, jsonException, "JSONException occured while uploading split file at nas repository", null);
            } catch (UnsupportedEncodingException unsupportedEncodingException) {
                LogSF.error(log, unsupportedEncodingException,
                            "UnsupportedEncodingException occured while uploading split file at nas repository", null);
            } catch (HttpException httpException) {
                LogSF.error(log, httpException, "HttpException occured while uploading split file at nas repository", null);
            } catch (IOException ioException) {
                LogSF.error(log, ioException, "IOException occured while uploading split file at nas repository", null);
            }
        }
        return uploadFileInfo;
    }
      
    

    /**
     * Method used to create the current session cookie contents
     * 
     * @since 03/10/2012
     * @return String - return cookie content as string
     */
    private static String getStubCookieInformation() {
        StringBuffer cookieInfo = new StringBuffer();
        String sessCookieValue = null;
        String sessionCookieValue = null;
        String stubInfoCookieValue = null;
        String secrCookieValue = null;
        String stubPersistentCookieValue = null;
        Long stubnetUserId = null;
        RequestContext requestContext = StubhubCredentials.getRequestContext();

        if (null != requestContext) {
            Map<String, LegacyCookieToken> stubPersistentCookieTokens = requestContext.getStubPersistentCookieTokens();
            Map<String, LegacyCookieToken> stubSessCookieTokens = requestContext.getStubSessCookieTokens();
            Map<String, LegacyCookieToken> stubSessionCookieTokens = requestContext.getStubSessionCookieTokens();
            Map<String, LegacyCookieToken> stubInfoCookieTokens = requestContext.getStubInfoCookieTokens();
            Map<String, LegacyCookieToken> stubSecrCookieTokens = requestContext.getStubSecrCookieTokens();
            stubnetUserId = requestContext.getStubnetUserId();
            try {
                if (null != stubSessCookieTokens && !stubSessCookieTokens.isEmpty()) {
                    sessCookieValue = LegacyCookieUtils.getSessionCookieValue(stubSessCookieTokens.values());
                    if (!StringUtils.isStringNullorEmpty(sessCookieValue)) {
                        cookieInfo.append(STUB_SESS).append(sessCookieValue).append(Constants.SEMICOLON);
                    }
                }
                if (null != stubSessionCookieTokens && !stubSessionCookieTokens.isEmpty()) {
                    sessionCookieValue = LegacyCookieUtils.getSessionCookieValue(stubSessionCookieTokens.values());
                    if (!StringUtils.isStringNullorEmpty(sessionCookieValue)) {
                        cookieInfo.append(STUB_SESSION).append(sessionCookieValue).append(Constants.SEMICOLON);
                    }
                }
                if (null != stubInfoCookieTokens && !stubInfoCookieTokens.isEmpty()) {                    
                    stubInfoCookieValue = LegacyCookieUtils.getSessionCookieValue(stubInfoCookieTokens.values());
                    if (!StringUtils.isStringNullorEmpty(stubInfoCookieValue)) {
                        cookieInfo.append(STUB_INFO).append(stubInfoCookieValue).append(Constants.SEMICOLON);
                    }
                }
                if (null != stubSecrCookieTokens && !stubSecrCookieTokens.isEmpty()) {                    
                    secrCookieValue = LegacyCookieUtils.getSessionCookieValue(stubSecrCookieTokens.values());
                    if (!StringUtils.isStringNullorEmpty(secrCookieValue)) {
                        cookieInfo.append(STUB_SECR).append(secrCookieValue).append(Constants.SEMICOLON);
                    }
                }
                if (null != stubPersistentCookieTokens && !stubPersistentCookieTokens.isEmpty()) {                    
                    stubPersistentCookieValue = LegacyCookieUtils.getPersistenceCookieValue(stubPersistentCookieTokens.values());
                    if (!StringUtils.isStringNullorEmpty(stubPersistentCookieValue)) {
                        cookieInfo.append(STUB_PERSISTENT).append(stubPersistentCookieValue).append(Constants.SEMICOLON);
                    }
                }
                if (null != stubnetUserId) {
                    cookieInfo.append(STUBNETUID).append(stubnetUserId);
                }
            } catch (Exception exception) {
                Object[] loggerInputArray = new Object[] { "Exception occured while getting stub cookie information",
                                                          cookieInfo.toString() };
                LogSF.error(log, exception, "{} cookie={}", loggerInputArray);
            }
            LogSF.debug(log, "Inside getStubCookieInformation cookie={}", cookieInfo.toString());
        }
        return cookieInfo.toString();
    }
    
    /**
     * Method to remove secured header from the Method headers
     * @param method input
     */
    private static void removeSecuredHeader(final HttpGet method) {
        if(null != method && null != method.getFirstHeader(SECURED_TICKET_TOKEN_HEADER)){
            LogSF.info(log, "Removing the header", null);
            method.removeHeader(method.getFirstHeader(SECURED_TICKET_TOKEN_HEADER));
        }
    } 
    
    private static String getXMLTagValue(String xml, String tag) {
    	if (null != xml && null != tag){
    		Matcher m = Pattern.compile("<"+tag+">(.*?)</"+tag+">").matcher(xml);
    		if(m.find()) {
    			return(m.group(1));
    		}
    	}
		return "";
	}

}
