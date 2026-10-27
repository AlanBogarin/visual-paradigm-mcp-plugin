package com.brunnen.vp.usecase.mcp;

import java.util.concurrent.CountDownLatch;

/** Transport smoke test; actual modeling operations require Visual Paradigm. */
public final class StandaloneMcpServer {
  private StandaloneMcpServer() {}

  /** Starts the transport without a VP application for Java runtime smoke tests. */
  public static void main(String[] args) throws Exception {
    McpServer server = new McpServer();
    server.start();
    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    new CountDownLatch(1).await();
  }
}
