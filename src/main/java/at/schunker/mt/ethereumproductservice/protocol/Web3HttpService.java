package at.schunker.mt.ethereumproductservice.protocol;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.CipherSuite;
import okhttp3.ConnectionSpec;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import okio.Buffer;
import okio.BufferedSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.web3j.protocol.Service;
import org.web3j.protocol.exceptions.ClientConnectionException;

import static okhttp3.ConnectionSpec.CLEARTEXT;

public class Web3HttpService extends Service {

    /** Copied from {@link ConnectionSpec#APPROVED_CIPHER_SUITES}. */
    @SuppressWarnings("JavadocReference")
    private static final CipherSuite[] INFURA_CIPHER_SUITES =
            new CipherSuite[] {
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_CHACHA20_POLY1305_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_CHACHA20_POLY1305_SHA256,

                    // Note that the following cipher suites are all on HTTP/2's bad cipher suites list.
                    // We'll
                    // continue to include them until better suites are commonly available. For example,
                    // none
                    // of the better cipher suites listed above shipped with Android 4.4 or Java 7.
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_RSA_WITH_AES_128_GCM_SHA256,
                    CipherSuite.TLS_RSA_WITH_AES_256_GCM_SHA384,
                    CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA,
                    CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA,
                    CipherSuite.TLS_RSA_WITH_3DES_EDE_CBC_SHA,

                    // Additional INFURA CipherSuites
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,
                    CipherSuite.TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384,
                    CipherSuite.TLS_RSA_WITH_AES_128_CBC_SHA256,
                    CipherSuite.TLS_RSA_WITH_AES_256_CBC_SHA256
            };

    private static final ConnectionSpec INFURA_CIPHER_SUITE_SPEC =
            new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .cipherSuites(INFURA_CIPHER_SUITES)
                    .build();

    /** The list of {@link ConnectionSpec} instances used by the connection. */
    private static final List<ConnectionSpec> CONNECTION_SPEC_LIST =
            Arrays.asList(INFURA_CIPHER_SUITE_SPEC, CLEARTEXT);

    public static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    public static final String DEFAULT_URL = "http://localhost:8545/";

    private static final Logger log = LoggerFactory.getLogger(Web3HttpService.class);

    private OkHttpClient httpClient;

    private final String url;

    private final boolean includeRawResponse;

    private HashMap<String, String> headers = new HashMap<>();

    public Web3HttpService(String url, OkHttpClient httpClient, boolean includeRawResponses) {
        super(includeRawResponses);
        this.url = url;
        this.httpClient = httpClient;
        this.includeRawResponse = includeRawResponses;
    }

    public Web3HttpService(OkHttpClient httpClient, boolean includeRawResponses) {
        this(DEFAULT_URL, httpClient, includeRawResponses);
    }

    public Web3HttpService(String url, OkHttpClient httpClient) {
        this(url, httpClient, false);
    }

    public Web3HttpService(String url) {
        this(url, createOkHttpClient());
    }

    public Web3HttpService(String url, boolean includeRawResponse) {
        this(url, createOkHttpClient(), includeRawResponse);
    }

    public Web3HttpService(OkHttpClient httpClient) {
        this(DEFAULT_URL, httpClient);
    }

    public Web3HttpService(boolean includeRawResponse) {
        this(DEFAULT_URL, includeRawResponse);
    }

    public Web3HttpService() {
        this(DEFAULT_URL);
    }

    private static OkHttpClient createOkHttpClient() {
        final OkHttpClient.Builder builder =
                new OkHttpClient.Builder().connectionSpecs(CONNECTION_SPEC_LIST);
        configureLogging(builder);
        return builder.build();
    }

    private static void configureLogging(OkHttpClient.Builder builder) {
        if (log.isDebugEnabled()) {
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor(log::debug);
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);
            builder.addInterceptor(logging);
        }
    }

    @Override
    protected InputStream performIO(String request) throws IOException {
        RequestBody requestBody = RequestBody.create(request, JSON_MEDIA_TYPE);
        Headers headers = buildHeaders();

        okhttp3.Request httpRequest =
                new okhttp3.Request.Builder().url(url).headers(headers).post(requestBody).build();

        try (okhttp3.Response response = httpClient.newCall(httpRequest).execute()) {
            processHeaders(response.headers());
            ResponseBody responseBody = response.body();
            if (response.isSuccessful()) {
                if (responseBody != null) {
                    return buildInputStream(responseBody);
                } else {
                    return null;
                }
            } else {
                int code = response.code();
                String text = responseBody == null ? "N/A" : responseBody.string();

                throw new ClientConnectionException(
                        "Invalid response received: " + code + "; " + text);
            }
        }
    }

    /*
    @Override
    protected InputStream performIO(String request) throws IOException {
        RequestBody requestBody = RequestBody.create(request, JSON_MEDIA_TYPE);
        Headers headers = buildHeaders();

        okhttp3.Request httpRequest =
                new okhttp3.Request.Builder().url(url).headers(headers).post(requestBody).build();

        okhttp3.Response response = httpClient.newCall(httpRequest).execute();
        processHeaders(response.headers());
        ResponseBody responseBody = response.body();
        if (response.isSuccessful()) {
            if (responseBody != null) {
                return buildInputStream(responseBody);
            } else {
                return null;
            }
        } else {
            int code = response.code();
            String text = responseBody == null ? "N/A" : responseBody.string();

            throw new ClientConnectionException("Invalid response received: " + code + "; " + text);
        }
    }
    */

    protected void processHeaders(Headers headers) {
        // Default implementation is empty
    }

    private InputStream buildInputStream(ResponseBody responseBody) throws IOException {
        InputStream inputStream = responseBody.byteStream();

        if (includeRawResponse) {
            // we have to buffer the entire input payload, so that after processing
            // it can be re-read and used to populate the rawResponse field.

            BufferedSource source = responseBody.source();
            source.request(Long.MAX_VALUE); // Buffer the entire body
            //Buffer buffer = source.buffer();
            Buffer buffer = source.getBuffer();

            long size = buffer.size();
            if (size > Integer.MAX_VALUE) {
                throw new UnsupportedOperationException(
                        "Non-integer input buffer size specified: " + size);
            }

            int bufferSize = (int) size;
            BufferedInputStream bufferedinputStream =
                    new BufferedInputStream(inputStream, bufferSize);

            bufferedinputStream.mark(inputStream.available());
            return bufferedinputStream;

        } else {
            return inputStream;
        }
    }

    private Headers buildHeaders() {
        return Headers.of(headers);
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void addHeaders(Map<String, String> headersToAdd) {
        headers.putAll(headersToAdd);
    }

    public HashMap<String, String> getHeaders() {
        return headers;
    }

    public String getUrl() {
        return url;
    }

    @Override
    public void close() throws IOException {}
}