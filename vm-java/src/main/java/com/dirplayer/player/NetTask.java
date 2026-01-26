package com.dirplayer.player;

import java.net.URI;

/**
 * Network task representation for HTTP requests.
 * Port of Rust NetTask and NetTaskState structs.
 */
public class NetTask {
    public int id;
    public String url;
    public URI resolvedUrl;
    public HttpMethod method;
    public String postData;

    /**
     * HTTP method enumeration.
     */
    public enum HttpMethod {
        GET,
        POST
    }

    /**
     * Create a new GET task.
     */
    public NetTask(int id, String url, URI resolvedUrl) {
        this.id = id;
        this.url = url;
        this.resolvedUrl = resolvedUrl;
        this.method = HttpMethod.GET;
        this.postData = null;
    }

    /**
     * Create a new POST task.
     */
    public static NetTask newPost(int id, String url, URI resolvedUrl, String postData) {
        NetTask task = new NetTask(id, url, resolvedUrl);
        task.method = HttpMethod.POST;
        task.postData = postData;
        return task;
    }

    /**
     * Network task state - tracks the result of a network operation.
     */
    public static class NetTaskState {
        /** The result of the task: Ok(byte[]) or Err(errorCode) */
        private NetResult result;

        public NetTaskState() {
            this.result = null;
        }

        public NetTaskState(NetResult result) {
            this.result = result;
        }

        public boolean isDone() {
            return result != null;
        }

        public NetResult getResult() {
            return result;
        }

        public void setResult(NetResult result) {
            this.result = result;
        }
    }

    /**
     * Network result - either success with data or error with code.
     */
    public static class NetResult {
        private final byte[] data;
        private final Integer errorCode;
        private final boolean isOk;

        private NetResult(byte[] data, Integer errorCode, boolean isOk) {
            this.data = data;
            this.errorCode = errorCode;
            this.isOk = isOk;
        }

        public static NetResult ok(byte[] data) {
            return new NetResult(data, null, true);
        }

        public static NetResult error(int errorCode) {
            return new NetResult(null, errorCode, false);
        }

        public boolean isOk() {
            return isOk;
        }

        public boolean isError() {
            return !isOk;
        }

        public byte[] getData() {
            return data;
        }

        public Integer getErrorCode() {
            return errorCode;
        }

        /**
         * Get the data as a string (UTF-8).
         */
        public String getDataAsString() {
            if (data != null) {
                return new String(data, java.nio.charset.StandardCharsets.UTF_8);
            }
            return "";
        }
    }
}
