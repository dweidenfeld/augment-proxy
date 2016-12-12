package bithazard.adaptor.augement;

import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.google.enterprise.adaptor.Acl;
import com.google.enterprise.adaptor.DocId;
import com.google.enterprise.adaptor.GroupPrincipal;
import com.google.enterprise.adaptor.Principal;
import com.google.enterprise.adaptor.Request;
import com.google.enterprise.adaptor.Response;
import com.google.enterprise.adaptor.UserPrincipal;
import com.sun.net.httpserver.HttpExchange;
import org.apache.http.Header;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class HttpHeaderHelper {
    private static final Logger LOGGER = Logger.getLogger(HttpHeaderHelper.class.getName());
    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");
    private static final ThreadLocal<DateFormat> DATE_FORMAT_RFC_1123 = new ThreadLocal<DateFormat>() {
        @Override
        public DateFormat get() {
            return new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
        }
    };
    private static final ThreadLocal<DateFormat> DATE_FORMAT_RFC_850 = new ThreadLocal<DateFormat>() {
        @Override
        public DateFormat get() {
            return new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss zzz", Locale.ENGLISH);
        }
    };
    private static final ThreadLocal<DateFormat> DATE_FORMAT_ASCTIME = new ThreadLocal<DateFormat>() {
        @Override
        public DateFormat get() {
            DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy", Locale.ENGLISH);
            dateFormat.setTimeZone(GMT);
            return dateFormat;
        }
    };

    private HttpHeaderHelper() {
        throw new AssertionError("Instantiating utility class...");
    }

    public static void setHeaders(Response response, boolean noFollow, boolean crawlOnce, boolean noArchive, boolean noIndex) {
        if (noFollow) {
            response.setNoFollow(true);
        }
        if (crawlOnce) {
            response.setCrawlOnce(true);
        }
        if (noArchive) {
            response.setNoArchive(true);
        }
        if (noIndex) {
            response.setNoIndex(true);
        }
    }

    public static Map<String, List<String>> convertToHeaderMap(List<NameValuePair> headers) {
        Map<String, List<String>> headerMap = new LinkedHashMap<>();
        for (NameValuePair header : headers) {
            String headerName = header.getName();
            String headerValue = header.getValue();
            addToMap(headerMap, headerName, headerValue);
        }
        return headerMap;
    }

    public static Map<String, List<String>> convertToHeaderMap(Header[] headers) {
        Map<String, List<String>> headerMap = new LinkedHashMap<>();
        for (Header header : headers) {
            String headerName = header.getName();
            String headerValue = header.getValue();
            addToMap(headerMap, headerName, headerValue);
        }
        return headerMap;
    }

    private static void addToMap(Map<String, List<String>> headerMap, String headerName, String headerValue) {
        List<String> existingHeaderValues = headerMap.get(headerName);
        if (existingHeaderValues == null) {
            existingHeaderValues = new ArrayList<>();
            headerMap.put(headerName, existingHeaderValues);
        }
        existingHeaderValues.add(headerValue);
    }

    public static void convertHeadersToMetadata(Response response, Map<String, List<String>> headers, Map<String, String> headersToMetadata) {
        for (Map.Entry<String, String> headerToMetadata : headersToMetadata.entrySet()) {
            List<String> existingHeaderValues = headers.get(headerToMetadata.getKey());
            if (existingHeaderValues != null) {
                for (String existingHeaderValue : existingHeaderValues) {
                    if ("Content-Disposition".equalsIgnoreCase(headerToMetadata.getKey())) {
                        existingHeaderValue = new String(
                                existingHeaderValue.getBytes(StandardCharsets.ISO_8859_1),
                                StandardCharsets.UTF_8);
                    }
                    response.addMetadata(headerToMetadata.getValue(), existingHeaderValue);
                }
            }
        }
    }

    public static String getRequestCookieValue(Request request) {
        HttpExchange httpExchange = getHttpExchange(request);
        if (httpExchange != null) {
            return httpExchange.getRequestHeaders().getFirst("Cookie");
        }
        return null;
    }

    public static void passResponseCookies(Map<String, List<String>> headers, Response response) {
        List<String> cookieValues = headers.get("Set-Cookie");
        if (cookieValues == null) {
            return;
        }
        HttpExchange httpExchange = getHttpExchange(response);
        if (httpExchange != null) {
            for (String cookieValue : cookieValues) {
                httpExchange.getResponseHeaders().add("Set-Cookie", cookieValue);
            }
        }
    }

    public static void transferDefaultHeadersToResponse(Map<String, List<String>> headers, Response response) {
        for (Map.Entry<String, List<String>> headerEntry : headers.entrySet()) {
            String headerName = headerEntry.getKey();
            if (headerName.equalsIgnoreCase("Content-Type")) {
                String headerValue = headerEntry.getValue().get(0);
                response.setContentType(headerValue);
            } else if (headerName.equalsIgnoreCase("Last-Modified")) {
                String headerValue = headerEntry.getValue().get(0);
                response.setLastModified(parseHttpDate(headerValue));
            }
        }
    }

    public static void transferGsaHeadersToResponse(Map<String, List<String>> headers, Response response) {
        for (Map.Entry<String, List<String>> headerEntry : headers.entrySet()) {
            String headerName = headerEntry.getKey();
            switch (headerName) {
                case "X-GSA-External-Metadata": {
                    String[] headerValues = headerEntry.getValue().get(0).split(",");
                    for (String headerValue : headerValues) {
                        String[] headerValueSplit = headerValue.split("=");
                        String metadataKeyDecoded = decodeUrl(headerValueSplit[0]);
                        String metadataValueDecoded = decodeUrl(headerValueSplit[1]);
                        response.addMetadata(metadataKeyDecoded, metadataValueDecoded);
                    }
                    break;
                }
                case "X-gsa-external-anchor": {
                    String[] headerValues = headerEntry.getValue().get(0).split(",");
                    for (String headerValue : headerValues) {
                        String[] headerValueSplit = headerValue.split("=");
                        if (headerValueSplit.length == 1) {
                            String anchorDecoded = decodeUrl(headerValueSplit[0]);
                            response.addAnchor(URI.create(anchorDecoded), null);
                        } else if (headerValueSplit.length == 2) {
                            String anchorTextDecoded = decodeUrl(headerValueSplit[0]);
                            String anchorDecoded = decodeUrl(headerValueSplit[1]);
                            response.addAnchor(URI.create(anchorDecoded), anchorTextDecoded);
                        }
                    }
                    break;
                }
                case "X-gsa-serve-security": {
                    String headerValue = headerEntry.getValue().get(0);
                    response.setSecure(headerValue.equals("secure"));
                    break;
                }
                case "X-gsa-doc-controls": {
                    for (String headerValue : headerEntry.getValue()) {
                        String[] headerValueSplit = headerValue.split("=");
                        switch (headerValueSplit[0]) {
                            case "crawl_once":
                                response.setCrawlOnce(Boolean.parseBoolean(headerValueSplit[1]));
                                break;
                            case "lock":
                                response.setLock(Boolean.parseBoolean(headerValueSplit[1]));
                                break;
                            case "display_url":
                                String displayUrlDecoded = decodeUrl(headerValueSplit[1]);
                                response.setDisplayUrl(URI.create(displayUrlDecoded));
                                break;
                            case "acl":
                                String aclUrlDecoded = decodeUrl(headerValueSplit[1]);
                                response.setAcl(jsonToAcl(aclUrlDecoded));
                                break;
                        }
                    }
                    break;
                }
                case "X-Robots-Tag": {
                    for (String headerValue : headerEntry.getValue()) {
                        switch (headerValue) {
                            case "noindex":
                                response.setNoIndex(true);
                                break;
                            case "nofollow":
                                response.setNoFollow(true);
                                break;
                            case "noarchive":
                                response.setNoArchive(true);
                                break;
                        }
                    }
                    break;
                }
            }
        }
    }

    private static Acl jsonToAcl(String json) {
        JSONObject aclJsonObject = (JSONObject) JSONValue.parse(json);
        if (aclJsonObject == null) {
            return null;
        }
        Acl.Builder aclBuilder = new Acl.Builder();
        List<UserPrincipal> permitUsers = new ArrayList<>();
        List<GroupPrincipal> permitGroups = new ArrayList<>();
        List<UserPrincipal> denyUsers = new ArrayList<>();
        List<GroupPrincipal> denyGroups = new ArrayList<>();
        for (Object currentAclObject : aclJsonObject.entrySet()) {
            if (currentAclObject instanceof Map.Entry) {
                Map.Entry aclObject = (Map.Entry) currentAclObject;
                String key = (String) aclObject.getKey();
                Object value = aclObject.getValue();
                if (value instanceof String) {
                    String stringValue = (String) value;
                    if (key.equals("inheritance_type")) {
                        aclBuilder.setInheritanceType(Acl.InheritanceType.valueOf(stringValue));
                    } else if (key.equals("inherit_from")) {
                        aclBuilder.setInheritFrom(new DocId(stringValue));
                    }
                } else if (value instanceof JSONArray) {
                    JSONArray jsonArray = (JSONArray) value;
                    for (Object currentAclEntries : jsonArray) {
                        if (currentAclEntries instanceof JSONObject) {
                            JSONObject aclEntries = (JSONObject) currentAclEntries;
                            Object caseSensitivityType = aclEntries.get("case_sensitivity_type");
                            if (caseSensitivityType.equals("everything_case_insensitive")) {
                                aclBuilder.setEverythingCaseInsensitive();
                            }
                            Object scope = aclEntries.get("scope");
                            Object access = aclEntries.get("access");
                            Object name = aclEntries.get("name");
                            Object namespace = aclEntries.get("namespace");
                            if (namespace == null) {
                                namespace = Principal.DEFAULT_NAMESPACE;
                            }
                            if (scope.equals("user")) {
                                if (access.equals("permit")) {
                                    permitUsers.add(new UserPrincipal(name.toString(), namespace.toString()));
                                } else if (access.equals("deny")) {
                                    denyUsers.add(new UserPrincipal(name.toString(), namespace.toString()));
                                }
                            } else if (scope.equals("group")) {
                                if (access.equals("permit")) {
                                    permitGroups.add(new GroupPrincipal(name.toString(), namespace.toString()));
                                } else if (access.equals("deny")) {
                                    denyGroups.add(new GroupPrincipal(name.toString(), namespace.toString()));
                                }
                            }
                        }
                    }
                }
            }
        }
        aclBuilder.setPermitUsers(permitUsers);
        aclBuilder.setPermitGroups(permitGroups);
        aclBuilder.setDenyUsers(denyUsers);
        aclBuilder.setDenyGroups(denyGroups);
        return aclBuilder.build();
    }

    private static String decodeUrl(String string) {
        try {
            return URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AugmentProxyException("Encoding UTF-8 is not supported. This should not be possible on a standard"
                    + " compliant java platform.", e);
        }
    }

    private static HttpExchange getHttpExchange(Request request) {
        if (request.getClass().getName().equals("com.google.enterprise.adaptor.DocumentHandler$DocumentRequest")) {
            try {
                Field ex = request.getClass().getDeclaredField("ex");
                ex.setAccessible(true);
                return  (HttpExchange) ex.get(request);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                LOGGER.log(Level.WARNING, "Exception getting HttpExchange.", e);
                return null;
            }
        } else {
            LOGGER.warning("Request is not of type DocumentRequest. Count not get HttpExchange.");
            return null;
        }
    }

    private static HttpExchange getHttpExchange(Response response) {
        if (response.getClass().getName().equals("com.google.enterprise.adaptor.DocumentHandler$DocumentResponse")) {
            try {
                Field ex = response.getClass().getDeclaredField("ex");
                ex.setAccessible(true);
                return  (HttpExchange) ex.get(response);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                LOGGER.log(Level.WARNING, "Exception getting HttpExchange.", e);
            }
        } else {
            LOGGER.warning("Response is not of type DocumentResponse. Count not get HttpExchange.");
        }
        return null;
    }

    private static Date parseHttpDate(String dateString) {
        try {
            return DATE_FORMAT_RFC_1123.get().parse(dateString);
        } catch (ParseException e) {
            LOGGER.fine("Parsing of date in format RFC 1123 failed.");
        }
        try {
            return DATE_FORMAT_RFC_850.get().parse(dateString);
        } catch (ParseException e) {
            LOGGER.fine("Parsing of date in format RFC 850 failed.");
        }
        try {
            return DATE_FORMAT_ASCTIME.get().parse(dateString);
        } catch (ParseException e) {
            LOGGER.fine("Parsing of date in ANSI C's asctime() format failed.");
            throw new AugmentProxyException(e);
        }
    }
}
