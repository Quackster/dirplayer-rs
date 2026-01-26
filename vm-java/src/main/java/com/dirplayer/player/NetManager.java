package com.dirplayer.player;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Network manager for handling HTTP requests.
 * Port of Rust NetManager struct.
 */
public class NetManager {
    public String basePath;
    public Map<Integer, NetTask> tasks;
    public Map<Integer, NetTaskState> taskStates;
    private int nextTaskId;

    public NetManager() {
        this.basePath = null;
        this.tasks = new HashMap<>();
        this.taskStates = new HashMap<>();
        this.nextTaskId = 1;
    }

    public int createTask(String url, NetTaskType type) {
        int taskId = nextTaskId++;
        NetTask task = new NetTask(taskId, url, type);
        tasks.put(taskId, task);
        taskStates.put(taskId, NetTaskState.Pending);
        return taskId;
    }

    public NetTask getTask(int taskId) {
        return tasks.get(taskId);
    }

    public NetTaskState getTaskState(int taskId) {
        return taskStates.getOrDefault(taskId, NetTaskState.Unknown);
    }

    public void fulfillTask(int taskId, byte[] data) {
        NetTask task = tasks.get(taskId);
        if (task != null) {
            task.data = data;
            taskStates.put(taskId, NetTaskState.Complete);
        }
    }

    public void failTask(int taskId, String error) {
        NetTask task = tasks.get(taskId);
        if (task != null) {
            task.error = error;
            taskStates.put(taskId, NetTaskState.Error);
        }
    }

    public static class NetTask {
        public int id;
        public String url;
        public NetTaskType type;
        public byte[] data;
        public String error;
        public CompletableFuture<byte[]> future;

        public NetTask(int id, String url, NetTaskType type) {
            this.id = id;
            this.url = url;
            this.type = type;
            this.data = null;
            this.error = null;
            this.future = new CompletableFuture<>();
        }
    }

    public enum NetTaskType {
        GetNetText,
        PreloadNetThing,
        DownloadNetThing
    }

    public enum NetTaskState {
        Unknown,
        Pending,
        InProgress,
        Complete,
        Error
    }
}
