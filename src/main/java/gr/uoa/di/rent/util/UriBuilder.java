package gr.uoa.di.rent.util;


import gr.uoa.di.rent.models.Hotel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.URI;

public class UriBuilder {

    private static final Logger logger = LoggerFactory.getLogger(UriBuilder.class);

    public static String baseUrl = null;

    public UriBuilder(Environment environment)
    {
        baseUrl = "http";

        String sslEnabled = environment.getProperty("server.ssl.enabled");
        if ( sslEnabled == null ) { // It's expected to not exist if there is no SSL-configuration.
            logger.warn("No property \"server.ssl.enabled\" was found in \"application.properties\"!");
            sslEnabled = "false";
        }
        baseUrl += sslEnabled.equals("true") ? "s" : "";

        baseUrl += "://";

        String hostName = InetAddress.getLoopbackAddress().getHostName();   // Non-null.
        baseUrl += hostName;

        String serverPort = environment.getProperty("server.port");
        if ( serverPort == null ) { // This is unacceptable!
            logger.error("No property \"server.port\" was found in \"application.properties\"!");
            System.exit(-1);    // Well, I guess the Spring Boot would not start in this case anyway.
        }
        baseUrl += ":" + serverPort;

        String baseInternalPath = environment.getProperty("server.servlet.context-path");
        if ( baseInternalPath != null ) {
            if ( !baseInternalPath.startsWith("/") )
                baseUrl += "/";
            baseUrl += baseInternalPath;
            if ( !baseInternalPath.endsWith("/") )
                baseUrl += "/";
        } else {
            logger.warn("No property \"server.servlet.context-path\" was found in \"application.properties\"!");    // Yes it's expected.
            baseUrl += "/";
        }

        logger.debug("ServerBaseURL: " + baseUrl);
    }

    public static String getBaseUrl() {
        return baseUrl;
    }

    public static void setBaseUrl(String baseUrl) {
        UriBuilder.baseUrl = baseUrl;
    }

    public static URI constructUri(Hotel hotel, String innerPath)
    {
        URI uri;
        String uriStr = UriBuilder.getBaseUrl();
        if ( uriStr == null ) {
            logger.error("The baseURL was NULL!");
            return null;
        }

        if ( hotel != null )
            uriStr += "hotels/" + hotel.getId();

        if ( innerPath != null )
            uriStr += innerPath;

        logger.debug("Created URI: " + uriStr);

        try {
            uri = new URI(uriStr);
        } catch (Exception e) {
            logger.error("", e);
            return null;
        }
        return uri;
    }

}
