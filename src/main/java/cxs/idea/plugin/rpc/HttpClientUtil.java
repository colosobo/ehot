package cxs.idea.plugin.rpc;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author gwk_2
 * @date 2022/5/12 16:12
 */
public class HttpClientUtil {

    private static Logger logger = LoggerFactory.getLogger(HttpClientUtil.class);

    /**
     * post request
     */
    public static byte[] postRequest(String reqURL, byte[] date) throws Exception {
        byte[] responseBytes = null;

        HttpPost httpPost = new HttpPost(reqURL);
        CloseableHttpClient httpClient = HttpClients.custom().disableAutomaticRetries().build();

        try {

            // timeout
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(500)
                    .setSocketTimeout(3000)
                    .setConnectTimeout(500)
                    .build();

            httpPost.setConfig(requestConfig);

            // data
            if (date != null) {
                httpPost.setEntity(new ByteArrayEntity(date, ContentType.DEFAULT_BINARY));
            }
            // do post
            HttpResponse response = httpClient.execute(httpPost);
            HttpEntity entity = response.getEntity();
            if (null != entity) {
                responseBytes = EntityUtils.toByteArray(entity);
                EntityUtils.consume(entity);
            }
        } catch (Exception e) {
            throw e;
        } finally {
            httpPost.releaseConnection();
            try {
                httpClient.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return responseBytes;
    }



    /**
     * read bytes from http request
     * @param request
     * @return
     * @throws IOException
     */
    public static final byte[] readBytes(HttpServletRequest request) throws IOException {
        request.setCharacterEncoding("UTF-8");
        int contentLen = request.getContentLength();
        InputStream is = request.getInputStream();
        if (contentLen > 0) {
            int readLen = 0;
            int readLengthThisTime = 0;
            byte[] message = new byte[contentLen];
            try {
                while (readLen != contentLen) {
                    readLengthThisTime = is.read(message, readLen, contentLen - readLen);
                    if (readLengthThisTime == -1) {
                        break;
                    }
                    readLen += readLengthThisTime;
                }
                return message;
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
                throw e;
            }
        }
        return new byte[] {};
    }

}
