package com.oujingzhou.censor;

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.proto.GraphDef;

import java.io.InputStream;

/**
 * load TensorFlow GraphDef (.pb)
 */
public class ModelHolder {
    private static volatile Graph graph;
    private static volatile Session session;
    private static final String PB_PATH = "/models/nsfw.299x299.pb";

    private ModelHolder() {  }

    /**
     * get singleton Session
     */
    public static Session getSession() {
        if (session == null) {
            synchronized (ModelHolder.class) {
                if (session == null) {
                    try (InputStream modelStream = ModelHolder.class.getResourceAsStream(PB_PATH)) {
                        if (modelStream == null) {
                            throw new RuntimeException("无法找到模型资源: /models/nsfw_model.pb");
                        }

                        byte[] graphBytes = modelStream.readAllBytes();
                        GraphDef graphDef = GraphDef.parseFrom(graphBytes);

                        graph = new Graph();
                        graph.importGraphDef(graphDef);
                        session = new Session(graph);
                        System.out.println(".pb 模型已加载（资源路径）");
                    } catch (Exception e) {
                        throw new RuntimeException("加载 .pb 模型失败（资源路径）", e);
                    }
                }
            }
        }
        return session;
    }

    /**
     * close Session
     */
    public static void close() {
        if (session != null) {
            session.close();
            graph.close();
            session = null;
            graph = null;
            System.out.println(".pb 模型已关闭");
        }
    }
}
