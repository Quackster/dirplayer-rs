package com.dirplayer.player;

import com.dirplayer.SimpleLogger;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;

/**
 * Network manager for handling HTTP requests.
 * Port of Rust NetManager struct.
 */
public class NetManager {
    private static final SimpleLogger logger = SimpleLogger.getLogger(NetManager.class);

    public String basePath;
    private URI basePathUri;
    public Map<Integer, NetTask> tasks;
    public Map<Integer, NetTask.NetTaskState> taskStates;
    private int nextTaskId;

    // Cache settings
    private boolean cacheDocVerify;
    private Map<String, byte[]> cache;
    private long cacheSize;

    // Task metadata
    private Map<Integer, String> taskMimeTypes;
    private Map<Integer, String> taskLastModDates;
    private Map<Integer, String> taskLocalPaths;

    // Pending tasks that need to be handled externally (e.g., by JavaScript in TeaVM)
    private final Map<Integer, Boolean> pendingTasks;

    // Tasks tagged as text retrieval tasks
    private final Map<Integer, Boolean> textTasks;

    // When true, execute network tasks synchronously (for testing)
    private boolean synchronousMode = false;

    public NetManager() {
        this.basePath = null;
        this.basePathUri = null;
        this.tasks = new ConcurrentHashMap<>();
        this.taskStates = new ConcurrentHashMap<>();
        this.nextTaskId = 1;
        this.cacheDocVerify = true;
        this.cache = new ConcurrentHashMap<>();
        this.cacheSize = 0;
        this.taskMimeTypes = new ConcurrentHashMap<>();
        this.taskLastModDates = new ConcurrentHashMap<>();
        this.taskLocalPaths = new ConcurrentHashMap<>();
        this.pendingTasks = new HashMap<>();
        this.textTasks = new ConcurrentHashMap<>();
    }

    /**
     * Enable or disable synchronous mode.
     * When enabled, network requests are executed immediately (useful for testing).
     */
    public void setSynchronousMode(boolean enabled) {
        this.synchronousMode = enabled;
    }

    /**
     * Check if synchronous mode is enabled.
     */
    public boolean isSynchronousMode() {
        return synchronousMode;
    }

    /**
     * Tag a task as a text retrieval task.
     * This affects how the result data is interpreted.
     */
    public void tagTaskAsText(int taskId) {
        textTasks.put(taskId, true);
    }

    /**
     * Check if a task is tagged as a text task.
     */
    public boolean isTextTask(int taskId) {
        return textTasks.getOrDefault(taskId, false);
    }

    /**
     * Set the base path for resolving relative URLs.
     */
    public void setBasePath(String basePath) {
        this.basePath = basePath;
        try {
            // Ensure the base path ends with a slash
            String sanitizedPath = basePath;
            if (!sanitizedPath.endsWith("/")) {
                sanitizedPath = sanitizedPath + "/";
            }
            this.basePathUri = new URI(sanitizedPath);
        } catch (Exception e) {
            logger.error("Failed to parse base path: {}", basePath, e);
        }
    }

    /**
     * Find a task by its URL.
     */
    public Integer findTaskByUrl(String url) {
        for (Map.Entry<Integer, NetTask> entry : tasks.entrySet()) {
            if (entry.getValue().url.equals(url)) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Get the state of a task.
     * If taskId is null, returns the state of the most recent task.
     */
    public NetTask.NetTaskState getTaskState(Integer taskId) {
        if (taskId == null) {
            // Return state of most recent task
            taskId = taskStates.size();
        }
        return taskStates.get(taskId);
    }

    /**
     * Check if a task is done.
     */
    public boolean isTaskDone(Integer taskId) {
        NetTask.NetTaskState state = getTaskState(taskId);
        return state != null && state.isDone();
    }

    /**
     * Wait for a task to complete (for synchronous testing).
     * In synchronous mode, tasks complete immediately so this is a no-op.
     * In async mode, this blocks until the task completes (with timeout).
     */
    public void awaitTask(int taskId) {
        awaitTask(taskId, 30000); // Default 30 second timeout
    }

    /**
     * Wait for a task to complete with specified timeout.
     */
    public void awaitTask(int taskId, long timeoutMs) {
        if (isTaskDone(taskId)) {
            return;
        }

        // For synchronous mode, task should already be done
        if (synchronousMode) {
            return;
        }

        // Poll until done or timeout
        long startTime = System.currentTimeMillis();
        while (!isTaskDone(taskId)) {
            if (System.currentTimeMillis() - startTime > timeoutMs) {
                logger.warn("Task {} timed out after {}ms", taskId, timeoutMs);
                return;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    /**
     * Get the result of a task.
     */
    public NetTask.NetResult getTaskResult(Integer taskId) {
        NetTask.NetTaskState state = getTaskState(taskId);
        if (state != null) {
            return state.getResult();
        }
        return null;
    }

    /**
     * Get a task by ID.
     */
    public NetTask getTask(int taskId) {
        return tasks.get(taskId);
    }

    /**
     * Check if a task is pending external handling.
     */
    public boolean isTaskPending(int taskId) {
        return pendingTasks.getOrDefault(taskId, false);
    }

    /**
     * Preload a network resource (GET request).
     * Returns the task ID.
     */
    public int preloadNetThing(String url) {
        // Check if the task already exists
        Integer existingTaskId = findTaskByUrl(url);
        if (existingTaskId != null) {
            return existingTaskId;
        }

        int taskId = nextTaskId++;
        URI resolvedUrl = normalizeTaskUrl(url);

        NetTask task = new NetTask(taskId, url, resolvedUrl);
        tasks.put(taskId, task);
        taskStates.put(taskId, new NetTask.NetTaskState());

        String resolvedUrlStr = resolvedUrl.toString();
        // Check for file:// or file:/ (Windows generates file:/C:/... with single slash)
        boolean isFileUrl = "file".equals(resolvedUrl.getScheme());

        // Mark task as pending for external handling (e.g., by JavaScript in TeaVM)
        // The external handler should call provideNetTaskData() when the fetch completes
        pendingTasks.put(taskId, true);

        // For synchronous environments or testing, execute immediately if enabled
        if (synchronousMode) {
            executeSynchronously(taskId, task, isFileUrl, resolvedUrl);
        }

        return taskId;
    }

    /**
     * POST data to a URL.
     * Returns the task ID.
     */
    public int postNetText(String url, String postData) {
        // For POST, always create a new task
        int taskId = nextTaskId++;
        URI resolvedUrl = normalizeTaskUrl(url);

        NetTask task = NetTask.newPost(taskId, url, resolvedUrl, postData);
        tasks.put(taskId, task);
        taskStates.put(taskId, new NetTask.NetTaskState());

        // Mark task as pending for external handling
        pendingTasks.put(taskId, true);

        // For synchronous environments or testing, execute immediately if enabled
        if (synchronousMode) {
            executeSynchronously(taskId, task, false, resolvedUrl);
        }

        return taskId;
    }

    /**
     * Download a file to a local path.
     * Returns the task ID.
     */
    public int downloadNetThing(String url, String localPath) {
        int taskId = preloadNetThing(url);
        taskLocalPaths.put(taskId, localPath);
        return taskId;
    }

    /**
     * Execute a network task synchronously (for non-TeaVM environments).
     * In TeaVM, network operations should be handled via JavaScript interop.
     */
    private void executeSynchronously(int taskId, NetTask task, boolean isFileUrl, URI resolvedUrl) {
        try {
            NetTask.NetResult result;

            if (isFileUrl) {
                result = handleFileUrlSync(resolvedUrl);
            } else {
                result = fetchNetTask(task);
            }

            fulfillTask(taskId, result);

            // If this was a download task, save to local path
            String localPath = taskLocalPaths.get(taskId);
            if (localPath != null && result.isOk()) {
                try {
                    Path path = Paths.get(localPath);
                    Files.createDirectories(path.getParent());
                    Files.write(path, result.getData());
                } catch (Exception e) {
                    logger.error("Failed to save downloaded file: {}", localPath, e);
                }
            }
        } catch (Exception e) {
            logger.error("Error executing network task {}: {}", taskId, e.getMessage(), e);
            fulfillTask(taskId, NetTask.NetResult.error(4));
        }
    }

    /**
     * Handle file:// URLs synchronously.
     */
    private NetTask.NetResult handleFileUrlSync(URI resolvedUrl) {
        try {
            logger.debug("handleFileUrlSync: {}", resolvedUrl);
            String path = resolvedUrl.getPath();
            if (path == null || path.isEmpty()) {
                // Try getSchemeSpecificPart for URIs like file:C:/path
                path = resolvedUrl.getSchemeSpecificPart();
            }
            logger.debug("handleFileUrlSync path before normalize: {}", path);

            // On Windows, remove leading slash from paths like /C:/...
            if (path != null && path.length() > 2 && path.charAt(0) == '/' && path.charAt(2) == ':') {
                path = path.substring(1);
            }

            logger.debug("handleFileUrlSync path after normalize: {}", path);

            if (path == null || path.isEmpty()) {
                logger.warn("Cannot extract path from file URL: {}", resolvedUrl);
                return NetTask.NetResult.error(4);
            }

            Path filePath = Paths.get(path);
            if (Files.exists(filePath)) {
                logger.info("Reading file: {}", filePath);
                byte[] data = Files.readAllBytes(filePath);
                logger.info("Read {} bytes from file", data.length);
                return NetTask.NetResult.ok(data);
            } else {
                logger.warn("File not found: {}", filePath);
                return NetTask.NetResult.error(4);
            }
        } catch (Exception e) {
            logger.error("Error reading file from URL {}: {}", resolvedUrl, e.getMessage(), e);
            return NetTask.NetResult.error(4);
        }
    }

    /**
     * Perform the actual HTTP fetch.
     */
    private NetTask.NetResult fetchNetTask(NetTask task) {
        String resolvedUrlStr = task.resolvedUrl.toString();
        logger.debug("execute_task #{} url: {} resolved: {}", task.id, task.url, resolvedUrlStr);

        // Safeguard: if a file:// URL reaches here, delegate to handleFileUrlSync
        if ("file".equals(task.resolvedUrl.getScheme())) {
            return handleFileUrlSync(task.resolvedUrl);
        }

        try {
            URL url = task.resolvedUrl.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            if (task.method == NetTask.HttpMethod.POST) {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                if (task.postData != null) {
                    try (OutputStream os = connection.getOutputStream()) {
                        os.write(task.postData.getBytes("UTF-8"));
                    }
                }
            } else {
                connection.setRequestMethod("GET");
            }

            int responseCode = connection.getResponseCode();

            // Store metadata
            String contentType = connection.getContentType();
            if (contentType != null) {
                taskMimeTypes.put(task.id, contentType);
            }

            long lastModified = connection.getLastModified();
            if (lastModified > 0) {
                taskLastModDates.put(task.id, String.valueOf(lastModified));
            }

            if (responseCode == 200) {
                try (InputStream is = connection.getInputStream();
                     ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    return NetTask.NetResult.ok(baos.toByteArray());
                }
            } else {
                logger.warn("HTTP error {} for URL: {}", responseCode, resolvedUrlStr);
                return NetTask.NetResult.error(4);
            }
        } catch (Exception e) {
            logger.error("Network error for URL {}: {}", resolvedUrlStr, e.getMessage());
            return NetTask.NetResult.error(4);
        }
    }

    /**
     * Complete a task with its result.
     */
    public void fulfillTask(int taskId, NetTask.NetResult result) {
        NetTask.NetTaskState state = taskStates.get(taskId);
        if (state != null) {
            state.setResult(result);
        } else {
            NetTask.NetTaskState newState = new NetTask.NetTaskState(result);
            taskStates.put(taskId, newState);
        }

        // Mark task as no longer pending
        pendingTasks.remove(taskId);

        // Cache successful results
        if (result.isOk()) {
            NetTask task = tasks.get(taskId);
            if (task != null) {
                cache.put(task.url, result.getData());
                cacheSize += result.getData().length;
            }
        }
    }

    /**
     * Fail a task with an error message.
     */
    public void failTask(int taskId, String errorMessage) {
        logger.warn("Task {} failed: {}", taskId, errorMessage);
        fulfillTask(taskId, NetTask.NetResult.error(4));
    }

    /**
     * Abort a specific task.
     */
    public void abortTask(int taskId) {
        NetTask.NetTaskState state = taskStates.get(taskId);
        if (state != null && !state.isDone()) {
            state.setResult(NetTask.NetResult.error(-1)); // Aborted
        }

        pendingTasks.remove(taskId);
    }

    /**
     * Abort all pending tasks.
     */
    public void abortAllTasks() {
        for (Map.Entry<Integer, NetTask.NetTaskState> entry : taskStates.entrySet()) {
            if (!entry.getValue().isDone()) {
                entry.getValue().setResult(NetTask.NetResult.error(-1)); // Aborted
            }
        }
        pendingTasks.clear();
    }

    /**
     * Get the MIME type of a completed task.
     */
    public String getTaskMimeType(int taskId) {
        return taskMimeTypes.get(taskId);
    }

    /**
     * Get the last modified date of a completed task.
     */
    public String getTaskLastModDate(int taskId) {
        return taskLastModDates.get(taskId);
    }

    /**
     * Get the current cache size.
     */
    public long getCacheSize() {
        return cacheSize;
    }

    /**
     * Get cache document verification mode.
     */
    public boolean getCacheDocVerify() {
        return cacheDocVerify;
    }

    /**
     * Set cache document verification mode.
     */
    public void setCacheDocVerify(boolean verify) {
        this.cacheDocVerify = verify;
    }

    /**
     * Clear the network cache.
     */
    public void clearCache() {
        cache.clear();
        cacheSize = 0;
    }

    /**
     * Normalize a URL, resolving it against the base path if necessary.
     */
    private URI normalizeTaskUrl(String url) {
        // Normalize slashes
        String slashNorm = url.replace("\\", "/");

        try {
            URI parsedUri = new URI(slashNorm);

            // If it has a scheme (http, https, file, etc.), use it as-is
            if (parsedUri.getScheme() != null) {
                return parsedUri;
            }

            // Check if it's an absolute path (no scheme)
            try {
                Path parsedPath = Paths.get(slashNorm);
                if (parsedPath.isAbsolute()) {
                    return parsedPath.toUri();
                }
            } catch (Exception pathEx) {
                // Not a valid path, continue with URI resolution
            }

            // Resolve against base path
            if (basePathUri != null) {
                return basePathUri.resolve(url);
            }

            return parsedUri;
        } catch (Exception e) {
            logger.error("Failed to normalize URL: {}", url, e);
            try {
                return new URI(slashNorm);
            } catch (Exception e2) {
                return URI.create("about:blank");
            }
        }
    }

    /**
     * Cleanup method (no-op for TeaVM compatibility).
     */
    public void shutdown() {
        // No executor to shutdown in TeaVM version
    }

    /**
     * Provide data for a pending network task (used by JS callback).
     */
    public void provideNetTaskData(int taskId, byte[] data) {
        if (data != null) {
            fulfillTask(taskId, NetTask.NetResult.ok(data));
        } else {
            fulfillTask(taskId, NetTask.NetResult.error(4));
        }
    }

    /**
     * Reset the manager to initial state.
     */
    public void reset() {
        abortAllTasks();
        tasks.clear();
        taskStates.clear();
        taskMimeTypes.clear();
        taskLastModDates.clear();
        taskLocalPaths.clear();
        nextTaskId = 1;
    }
}
