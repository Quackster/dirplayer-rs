package com.dirplayer.player.handlers;

import com.dirplayer.director.lingo.Datum;
import com.dirplayer.player.DirPlayer;
import com.dirplayer.player.NetTask;
import com.dirplayer.player.ScriptError;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Network-related handler functions.
 * Port of Rust NetHandlers struct.
 */
public class NetHandlers {

    /**
     * netDone([taskId]) - Check if a network task is complete.
     * Returns TRUE if the task is done, FALSE otherwise.
     */
    public static int netDone(DirPlayer player, List<Integer> args) throws ScriptError {
        Integer taskId = null;
        if (!args.isEmpty()) {
            Datum taskIdDatum = player.getDatum(args.get(0));
            taskId = taskIdDatum.intValue();
        }

        NetTask.NetTaskState taskState = player.netManager.getTaskState(taskId);
        boolean isDone = taskState != null && taskState.isDone();

        return player.allocDatum(Datum.ofInt(isDone ? 1 : 0));
    }

    /**
     * preloadNetThing(url) - Preload a network resource.
     * Returns the task ID.
     */
    public static int preloadNetThing(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("preloadNetThing requires a URL argument");
        }

        String url = player.getDatum(args.get(0)).stringValue();
        int taskId = player.netManager.preloadNetThing(url);

        return player.allocDatum(Datum.ofInt(taskId));
    }

    /**
     * getNetText(url) - Download text from a URL.
     * Returns the task ID.
     */
    public static int getNetText(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("getNetText requires a URL argument");
        }

        String originalUrl = player.getDatum(args.get(0)).stringValue();

        // Decode URL-encoded characters
        String url;
        try {
            url = URLDecoder.decode(originalUrl, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new ScriptError("Cannot decode URL: " + e.getMessage());
        }

        // Tag the task as a text task for text retrieval
        int taskId = player.netManager.preloadNetThing(url);
        player.netManager.tagTaskAsText(taskId);

        return player.allocDatum(Datum.ofInt(taskId));
    }

    /**
     * getStreamStatus(taskIdOrUrl) - Get the status of a network stream.
     * Returns a property list with URL, state, bytesSoFar, bytesTotal, and error.
     */
    public static int getStreamStatus(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("getStreamStatus requires a task ID or URL argument");
        }

        Datum arg = player.getDatum(args.get(0));
        int taskId;

        // Support both task ID (int) and URL (string)
        if (arg.isInt()) {
            taskId = arg.intValue();
        } else if (arg.isString()) {
            String url = arg.stringValue();
            Integer foundTaskId = player.netManager.findTaskByUrl(url);
            if (foundTaskId == null) {
                throw new ScriptError("Network task not found for URL: " + url);
            }
            taskId = foundTaskId;
        } else {
            throw new ScriptError("getStreamStatus requires an integer task ID or URL string");
        }

        NetTask task = player.netManager.getTask(taskId);
        if (task == null) {
            throw new ScriptError("Network task " + taskId + " not found");
        }

        NetTask.NetTaskState taskState = player.netManager.getTaskState(taskId);
        if (taskState == null) {
            throw new ScriptError("Network task state " + taskId + " not found");
        }

        String state;
        String error;
        boolean isOk;

        if (taskState.isDone() && taskState.getResult() != null && taskState.getResult().isOk()) {
            state = "Complete";
            error = "OK";
            isOk = true;
        } else if (taskState.isDone() && taskState.getResult() != null && taskState.getResult().isError()) {
            state = "Complete";
            error = "Task failed";
            isOk = false;
        } else {
            state = "InProgress";
            error = "";
            isOk = false;
        }

        // Build the result property list
        List<int[]> propList = new ArrayList<>();

        propList.add(new int[] {
            player.allocDatum(Datum.ofString("URL")),
            player.allocDatum(Datum.ofString(task.url))
        });
        propList.add(new int[] {
            player.allocDatum(Datum.ofString("state")),
            player.allocDatum(Datum.ofString(state))
        });
        propList.add(new int[] {
            player.allocDatum(Datum.ofString("bytesSoFar")),
            player.allocDatum(Datum.ofInt(isOk ? 100 : 0))
        });
        propList.add(new int[] {
            player.allocDatum(Datum.ofString("bytesTotal")),
            player.allocDatum(Datum.ofInt(100))
        });
        propList.add(new int[] {
            player.allocDatum(Datum.ofString("error")),
            player.allocDatum(Datum.ofString(error))
        });

        Datum resultMap = Datum.ofPropList(propList, false, true);
        return player.allocDatum(resultMap);
    }

    /**
     * netError([taskId]) - Get the error status of a network task.
     * Returns "OK" if successful, or an error code if failed.
     */
    public static int netError(DirPlayer player, List<Integer> args) throws ScriptError {
        Integer taskId = null;
        if (!args.isEmpty()) {
            Datum datum = player.getDatum(args.get(0));
            if (!datum.isVoid()) {
                taskId = datum.intValue();
            }
        }

        NetTask.NetTaskState taskState = player.netManager.getTaskState(taskId);
        if (taskState == null) {
            throw new ScriptError("Network task not found");
        }

        boolean isOk = taskState.isDone() &&
                       taskState.getResult() != null &&
                       taskState.getResult().isOk();

        Datum error;
        if (isOk) {
            error = Datum.ofString("OK");
        } else if (taskState.getResult() != null && taskState.getResult().isError()) {
            error = Datum.ofInt(taskState.getResult().getErrorCode());
        } else {
            error = Datum.ofInt(0);
        }

        return player.allocDatum(error);
    }

    /**
     * netTextResult([taskId]) - Get the text result of a network task.
     * Returns the text content or empty string if not ready/failed.
     */
    public static int netTextResult(DirPlayer player, List<Integer> args) throws ScriptError {
        Integer taskId = null;
        if (!args.isEmpty()) {
            Datum datum = player.getDatum(args.get(0));
            if (!datum.isVoid()) {
                taskId = datum.intValue();
            }
        }

        NetTask.NetTaskState taskState = player.netManager.getTaskState(taskId);
        if (taskState == null) {
            throw new ScriptError("Network task not found");
        }

        boolean isOk = taskState.isDone() &&
                       taskState.getResult() != null &&
                       taskState.getResult().isOk();

        String text;
        if (isOk) {
            text = taskState.getResult().getDataAsString();
        } else {
            text = "";
        }

        return player.allocDatum(Datum.ofString(text));
    }

    /**
     * postNetText(url, [data], [serverOS], [charset]) - POST data to a URL.
     * Returns the task ID.
     */
    public static int postNetText(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("postNetText requires at least 1 argument (url)");
        }

        String url = player.getDatum(args.get(0)).stringValue();

        // Get the post data (can be a property list or string)
        String postData;
        if (args.size() > 1) {
            Datum dataDatum = player.getDatum(args.get(1));
            if (dataDatum.isPropList()) {
                // Convert property list to form data
                List<String> formParts = new ArrayList<>();
                List<int[]> propList = dataDatum.toPropList();
                for (int[] pair : propList) {
                    String key = player.getDatum(pair[0]).stringValue();
                    String value = player.getDatum(pair[1]).stringValue();
                    // URL encode the key and value
                    try {
                        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8.name());
                        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8.name());
                        formParts.add(encodedKey + "=" + encodedValue);
                    } catch (Exception e) {
                        throw new ScriptError("Failed to encode form data: " + e.getMessage());
                    }
                }
                postData = String.join("&", formParts);
            } else if (dataDatum.isString()) {
                postData = dataDatum.stringValue();
            } else {
                throw new ScriptError("postNetText second argument must be a property list or string, got " + dataDatum.typeStr());
            }
        } else {
            postData = "";
        }

        // Optional server OS string (3rd argument) - not used but accepted
        if (args.size() > 2) {
            // String serverOS = player.getDatum(args.get(2)).stringValue();
        }

        // Optional server charset string (4th argument) - not used but accepted
        if (args.size() > 3) {
            // String serverCharset = player.getDatum(args.get(3)).stringValue();
        }

        // Create the network task
        int taskId = player.netManager.postNetText(url, postData);

        return player.allocDatum(Datum.ofInt(taskId));
    }

    /**
     * downloadNetThing(url, localPath) - Download a file to local storage.
     * Returns the task ID.
     */
    public static int downloadNetThing(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.size() < 2) {
            throw new ScriptError("downloadNetThing requires 2 arguments (url, localPath)");
        }

        String url = player.getDatum(args.get(0)).stringValue();
        String localPath = player.getDatum(args.get(1)).stringValue();

        int taskId = player.netManager.downloadNetThing(url, localPath);

        return player.allocDatum(Datum.ofInt(taskId));
    }

    /**
     * netAbort([taskId]) - Abort a network task or all tasks.
     */
    public static int netAbort(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            // Abort all tasks
            player.netManager.abortAllTasks();
        } else {
            int taskId = player.getDatum(args.get(0)).intValue();
            player.netManager.abortTask(taskId);
        }
        return 0; // Void
    }

    /**
     * netMIME(taskId) - Get the MIME type of a completed network task.
     */
    public static int netMIME(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("netMIME requires a task ID argument");
        }

        int taskId = player.getDatum(args.get(0)).intValue();
        String mimeType = player.netManager.getTaskMimeType(taskId);

        return player.allocDatum(Datum.ofString(mimeType != null ? mimeType : ""));
    }

    /**
     * netLastModDate(taskId) - Get the last modified date of a completed network task.
     */
    public static int netLastModDate(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("netLastModDate requires a task ID argument");
        }

        int taskId = player.getDatum(args.get(0)).intValue();
        String lastModDate = player.netManager.getTaskLastModDate(taskId);

        return player.allocDatum(Datum.ofString(lastModDate != null ? lastModDate : ""));
    }

    /**
     * cacheSize() - Get the current cache size.
     */
    public static int cacheSize(DirPlayer player, List<Integer> args) throws ScriptError {
        long size = player.netManager.getCacheSize();
        return player.allocDatum(Datum.ofInt((int) size));
    }

    /**
     * cacheDocVerify() - Get or set cache document verification mode.
     */
    public static int cacheDocVerify(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            // Get current value
            boolean verify = player.netManager.getCacheDocVerify();
            return player.allocDatum(Datum.ofInt(verify ? 1 : 0));
        } else {
            // Set value
            boolean verify = player.getDatum(args.get(0)).intValue() != 0;
            player.netManager.setCacheDocVerify(verify);
            return 0; // Void
        }
    }

    /**
     * clearCache() - Clear the network cache.
     */
    public static int clearCache(DirPlayer player, List<Integer> args) throws ScriptError {
        player.netManager.clearCache();
        return 0; // Void
    }

    /**
     * externalEvent(eventString) - Send an external event (for browser integration).
     */
    public static int externalEvent(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("externalEvent requires an event string argument");
        }

        String eventString = player.getDatum(args.get(0)).stringValue();
        // In a browser environment, this would trigger a JavaScript callback
        // For now, just log it
        System.out.println("External event: " + eventString);

        return 0; // Void
    }

    /**
     * gotoNetPage(url, [target]) - Navigate to a URL in the browser.
     */
    public static int gotoNetPage(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("gotoNetPage requires a URL argument");
        }

        String url = player.getDatum(args.get(0)).stringValue();
        String target = args.size() > 1 ? player.getDatum(args.get(1)).stringValue() : "_self";

        // In a browser environment, this would navigate to the URL
        // For now, just log it
        System.out.println("gotoNetPage: " + url + " (target: " + target + ")");

        return 0; // Void
    }

    /**
     * gotoNetMovie(url) - Load a Director movie from a URL.
     */
    public static int gotoNetMovie(DirPlayer player, List<Integer> args) throws ScriptError {
        if (args.isEmpty()) {
            throw new ScriptError("gotoNetMovie requires a URL argument");
        }

        String url = player.getDatum(args.get(0)).stringValue();

        // This would trigger loading a new movie from the network
        // For now, just log it
        System.out.println("gotoNetMovie: " + url);

        return 0; // Void
    }
}
