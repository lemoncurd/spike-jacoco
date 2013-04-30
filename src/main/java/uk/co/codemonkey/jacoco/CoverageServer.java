package uk.co.codemonkey.jacoco;



import org.jacoco.core.data.*;

import org.jacoco.core.runtime.RemoteControlReader;
import org.jacoco.core.runtime.RemoteControlWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.net.InetAddress.getByName;

/**
 *
 *
 */
public class CoverageServer {

    private static final int PORT = 9092;
    private static final String ADDRESS = "localhost";

    private final ServerSocket server;
    private final AtomicBoolean shouldRun = new AtomicBoolean(true);

    public CoverageServer(File destFile) throws IOException {
        final ExecutionDataWriter fileWriter = new ExecutionDataWriter(new FileOutputStream(destFile, true));
        server = new ServerSocket(PORT, 0, getByName(ADDRESS));
        new Thread(new Runnable() {
          @Override
            public void run() {
                while (shouldRun.get()) {
                    final Handler handler;
                    try {
                        handler = new Handler(server.accept(), fileWriter);
                        new Thread(handler).start();
                    } catch (IOException e) {
                        if(!server.isClosed()) {
                            throw new RuntimeException(e);
                        }
                    }
                }

            }
        }).start();
    }

    public void stop() throws IOException {
         shouldRun.set(false);
         server.close();
    }

    private static class Handler implements Runnable, ISessionInfoVisitor, IExecutionDataVisitor {

        private final Socket socket;
        private final RemoteControlReader reader;
        private final ExecutionDataWriter fileWriter;

        Handler(final Socket socket, final ExecutionDataWriter fileWriter) throws IOException {
            this.socket = socket;
            this.fileWriter = fileWriter;

            // Just send a valid header:
            new RemoteControlWriter(socket.getOutputStream());

            reader = new RemoteControlReader(socket.getInputStream());
            reader.setSessionInfoVisitor(this);
            reader.setExecutionDataVisitor(this);
        }

        public void run() {
            try {
                while (reader.read()) {
                }
                socket.close();
                synchronized (fileWriter) {
                    fileWriter.flush();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        public void visitSessionInfo(final SessionInfo info) {
            System.out.printf("Retrieving execution Data for session: %s%n", info.getId());
            synchronized (fileWriter) {
                fileWriter.visitSessionInfo(info);
            }
        }

        public void visitClassExecution(final ExecutionData data) {
            synchronized (fileWriter) {
                fileWriter.visitClassExecution(data);
            }
        }
    }

}
