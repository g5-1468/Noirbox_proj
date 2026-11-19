package noirbox.api;

/**
 * Starts the NOIRBOX web API.
 *
 *   java -cp "out;lib/*" noirbox.api.ApiMain        (default port 8080)
 *   java -cp "out;lib/*" noirbox.api.ApiMain 8090   (custom port)
 *
 * The React website calls http://localhost:8080/api/...
 * Leave this running while you use the site.
 */
public class ApiMain {

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : portFromEnvironment();

        ApiServer server = new ApiServer(port);
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));

        System.out.println("=====================================================");
        System.out.println("   NOIRBOX API is running");
        System.out.println("   http://localhost:" + port + "/api/health");
        System.out.println("   Press Ctrl+C to stop");
        System.out.println("=====================================================");
    }

    /** Uses the PORT variable when it holds a usable number, otherwise 8080. */
    private static int portFromEnvironment() {
        String value = System.getenv("PORT");
        if (value == null || value.isBlank()) {
            return 8080;
        }
        try {
            int port = Integer.parseInt(value.trim());
            return port > 0 ? port : 8080;
        } catch (NumberFormatException notANumber) {
            return 8080;
        }
    }
}
