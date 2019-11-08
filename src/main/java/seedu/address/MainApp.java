package seedu.address;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.stage.Stage;
import seedu.address.commons.core.Config;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.core.Version;
import seedu.address.commons.exceptions.DataConversionException;
import seedu.address.commons.util.ConfigUtil;
import seedu.address.commons.util.StringUtil;
import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.model.Athletick;
import seedu.address.model.TrainingManager;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.Performance;
import seedu.address.model.ReadOnlyAthletick;
import seedu.address.model.ReadOnlyPerformance;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.UserPrefs;
import seedu.address.model.history.HistoryManager;
import seedu.address.model.util.SampleDataUtil;
import seedu.address.storage.AthletickStorage;
import seedu.address.storage.AttendanceStorage;
import seedu.address.storage.ImageStorage;
import seedu.address.storage.JsonAthletickStorage;
import seedu.address.storage.JsonAttendanceStorage;
import seedu.address.storage.JsonPerformanceStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.PerformanceStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.storage.UserPrefsStorage;
import seedu.address.ui.Ui;
import seedu.address.ui.UiManager;

/**
 * Runs the application.
 */
public class MainApp extends Application {

    public static final Version VERSION = new Version(1, 3, 3, true);

    private static final Logger logger = LogsCenter.getLogger(MainApp.class);

    protected Ui ui;
    protected Logic logic;
    protected Storage storage;
    protected Model model;
    protected Config config;
    @Override
    public void init() throws Exception {
        logger.info("=============================[ Initializing Athletick ]===========================");
        super.init();

        AppParameters appParameters = AppParameters.parse(getParameters());
        config = initConfig(appParameters.getConfigPath());

        UserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(config.getUserPrefsFilePath());
        UserPrefs userPrefs = initPrefs(userPrefsStorage);
        AthletickStorage athletickStorage = new JsonAthletickStorage(userPrefs.getAthletickFilePath());
        PerformanceStorage performanceStorage = new JsonPerformanceStorage(userPrefs.getPerformanceFilePath());
        AttendanceStorage attendanceStorage = new JsonAttendanceStorage(userPrefs.getAttendanceFilePath());
        ImageStorage imageStorage = new ImageStorage(userPrefs.getImageFilePath());
        storage = new StorageManager(athletickStorage, performanceStorage, attendanceStorage, userPrefsStorage);

        imageStorage.createImageFile();

        initLogging(config);

        model = initModelManager(storage, userPrefs);
        logic = new LogicManager(model, storage);

        ui = new UiManager(logic, model);
    }

    /**
     * Returns a {@code ModelManager} with the data from {@code storage}'s athletick data
     * and {@code userPrefs}. <br>
     * The data from the sample athletick will be used instead if
     * {@code storage}'s athletick data is not found, or an empty athletick will be
     * used instead if errors occur when reading {@code storage}'s athletick data.
     */
    private Model initModelManager(Storage storage, ReadOnlyUserPrefs userPrefs) {
        Optional<ReadOnlyAthletick> athletickOptional;
        ReadOnlyAthletick initialAthletick;
        ReadOnlyAthletick initialData;
        HistoryManager history = new HistoryManager();
        try {
            athletickOptional = storage.readAthletick();
            if (!athletickOptional.isPresent()) {
                logger.info("Data file for Athletick not found. Will be starting with a sample " + "team list");
            }
            initialAthletick = athletickOptional.orElseGet(SampleDataUtil::getSampleAthletick);
            initialData = athletickOptional.orElseGet(SampleDataUtil::getSampleAthletick);

        } catch (DataConversionException e) {
            logger.warning(
                    "Data file for Athletick not in the correct format. Will be starting with an " + "empty team list");
            initialAthletick = new Athletick();
        } catch (IOException e) {
            logger.warning("Problem while reading from Athletick file. Will be starting with an empty " + "team list");
            initialAthletick = new Athletick();
        }

        Optional<ReadOnlyPerformance> performanceOptional;
        ReadOnlyPerformance initialEventsList;
        try {
            performanceOptional = storage.readEvents();
            if (!performanceOptional.isPresent()) {
                logger.info("Data file for EventList not found. Will be starting with a sample EventList");
            }
            initialEventsList = performanceOptional.orElseGet(SampleDataUtil::getSamplePerformance);
        } catch (DataConversionException e) {
            logger.warning("Data file for EventList not in the correct format. Will be starting with empty EventList");
            initialEventsList = new Performance();
        } catch (IOException e) {
            logger.warning("Problem while reading from EventList file. Will be starting with an empty EventList");
            initialEventsList = new Performance();
        }

        Optional<TrainingManager> attendanceOptional;
        TrainingManager initialTrainingManager;
        try {
            attendanceOptional = storage.readTrainingManager();
            if (!attendanceOptional.isPresent()) {
                logger.info("Data file not found. Will be starting with a sample Attendance");
            }
            initialTrainingManager = attendanceOptional.orElse(new TrainingManager());
        } catch (DataConversionException e) {
            logger.warning("Data file not in the correct format. Will be starting with an empty Attendance");
            initialTrainingManager = new TrainingManager();
        } catch (IOException e) {
            logger.warning("Problem while reading from the file. Will be starting with an empty Attendance");
            initialTrainingManager = new TrainingManager();
        }
        return new ModelManager(initialAthletick, initialEventsList, initialTrainingManager, userPrefs, history);
    }

    private void initLogging(Config config) {
        LogsCenter.init(config);
    }

    /**
     * Returns a {@code Config} using the file at {@code configFilePath}. <br>
     * The default file path {@code Config#DEFAULT_CONFIG_FILE} will be used instead
     * if {@code configFilePath} is null.
     */
    protected Config initConfig(Path configFilePath) {
        Config initializedConfig;
        Path configFilePathUsed;

        configFilePathUsed = Config.DEFAULT_CONFIG_FILE;

        if (configFilePath != null) {
            logger.info("Custom Config file specified " + configFilePath);
            configFilePathUsed = configFilePath;
        }

        logger.info("Using config file : " + configFilePathUsed);

        try {
            Optional<Config> configOptional = ConfigUtil.readConfig(configFilePathUsed);
            initializedConfig = configOptional.orElse(new Config());
        } catch (DataConversionException e) {
            logger.warning("Config file at " + configFilePathUsed + " is not in the correct format. "
                    + "Using default config properties");
            initializedConfig = new Config();
        }

        // Update config file in case it was missing to begin with or there are
        // new/unused fields
        try {
            ConfigUtil.saveConfig(initializedConfig, configFilePathUsed);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }
        return initializedConfig;
    }

    /**
     * Returns a {@code UserPrefs} using the file at {@code storage}'s user prefs
     * file path, or a new {@code UserPrefs} with default configuration if errors
     * occur when reading from the file.
     */
    protected UserPrefs initPrefs(UserPrefsStorage storage) {
        Path prefsFilePath = storage.getUserPrefsFilePath();
        logger.info("Using prefs file : " + prefsFilePath);

        UserPrefs initializedPrefs;
        try {
            Optional<UserPrefs> prefsOptional = storage.readUserPrefs();
            initializedPrefs = prefsOptional.orElse(new UserPrefs());
        } catch (DataConversionException e) {
            logger.warning("UserPrefs file at " + prefsFilePath + " is not in the correct format. "
                    + "Using default user prefs");
            initializedPrefs = new UserPrefs();
        } catch (IOException e) {
            logger.warning("Problem while reading from the file. Will be starting with an empty team list");
            initializedPrefs = new UserPrefs();
        }

        // Update prefs file in case it was missing to begin with or there are
        // new/unused fields
        try {
            storage.saveUserPrefs(initializedPrefs);
        } catch (IOException e) {
            logger.warning("Failed to save config file : " + StringUtil.getDetails(e));
        }

        return initializedPrefs;
    }

    @Override
    public void start(Stage primaryStage) {
        logger.info("Starting Athletick " + MainApp.VERSION);
        ui.start(primaryStage);
    }

    @Override
    public void stop() {
        logger.info("============================ [ Stopping Athletick ] " + "=============================");
        try {
            storage.saveUserPrefs(model.getUserPrefs());
        } catch (IOException e) {
            logger.severe("Failed to save preferences " + StringUtil.getDetails(e));
        }
    }
}
