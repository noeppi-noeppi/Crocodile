package crocodile;

import crocodile.config.ConfigParser;
import crocodile.db.Database;
import crocodile.route.AlterEventRoute;
import crocodile.route.DeleteEventRoute;
import crocodile.route.NewEventRoute;
import crocodile.route.WebcalRoute;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.util.PathConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Service;

import javax.security.auth.login.Configuration;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) throws Exception {
        OptionParser options = new OptionParser(false);
        OptionSpec<Void> specHelp = options.accepts("help", "Show help.").forHelp();
        OptionSpec<Path> specConfig = options.accepts("config", "The config file to load.").withRequiredArg().withValuesConvertedBy(new PathConverter());
        OptionSpec<Path> specLogin = options.accepts("login", "The login file to load.").withRequiredArg().withValuesConvertedBy(new PathConverter());
        OptionSet set = options.parse(args);
        
        if (set.has(specHelp)) {
            options.printHelpOn(System.out);
            return;
        }
        
        if (set.has(specLogin)) {
            Path loginFilePath = set.valueOf(specLogin).toAbsolutePath().normalize();
            if (!Files.isRegularFile(loginFilePath)) throw new FileNotFoundException("Login file not found: " + loginFilePath);
            logger.info("Setting login file: {}", loginFilePath);
            System.setProperty("java.security.auth.login.config", loginFilePath.toString());
            Configuration.getConfiguration().refresh();
        }
        
        if (!set.has(specConfig)) {
            throw new IllegalStateException("No config file provided.");
        }
        
        logger.info("Reading config.");
        Config config = ConfigParser.readConfig(set.valueOf(specConfig));

        logger.info("Connecting to the database");
        Database database = Database.connect(config.database());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                database.close();
            } catch (Exception e) {
                //
            }
        }));
        
        logger.info("Starting Server on port {}.", config.port());
        Service spark = Service.ignite();
        spark.port(config.port());
        spark.withVirtualThread();

        // Support trailing slashes
        spark.before((req, res) -> {
            String path = req.pathInfo();
            if (path.length() > 1 && path.endsWith("/")) {
                res.redirect(path.substring(0, path.length() - 1));
            }
        });
        
        if (config.redirect() != null) spark.redirect.get("/", config.redirect());
        spark.get("/:calendar", new WebcalRoute(spark, config, database));
        spark.get("/:calendar/:uid", new WebcalRoute(spark, config, database));
        spark.put("/:calendar", new NewEventRoute(spark, config, database));
        spark.patch("/:calendar/:uid", new AlterEventRoute(spark, config, database));
        spark.delete("/:calendar", new DeleteEventRoute(spark, config, database));
        spark.delete("/:calendar/:uid", new DeleteEventRoute(spark, config, database));

        spark.awaitInitialization();
        logger.info("Server started.");
    }
}
