import javafx.application.Application;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main extends Application {
    private final ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(3);
    private HashMap<Integer, Boolean> activeKeys = new HashMap<>();
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Display display = new Display();
        Group group = new Group(display);
        Scene scene = new Scene(group, 640, 320);
        Media sound = new Media(new File("berp.mp3").toURI().toString());
        MediaPlayer mediaPlayer = new MediaPlayer(sound);
        scene.setOnKeyPressed(e -> {
            if (!activeKeys.containsKey(e.getCode().getCode())) {
                activeKeys.put(e.getCode().getCode(), true);
            }
        });
        scene.setOnKeyReleased(e -> {
            activeKeys.remove(e.getCode().getCode());
        });
        Emulator cpu = new Emulator(activeKeys);
        cpu.loadFile("roms/spaceinvaders.ch8");
        ScheduledFuture<?> cpuThread = pool.scheduleWithFixedDelay(cpu::cycle, 0, 1, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> timerThread = pool.scheduleWithFixedDelay(() -> {
            if (cpu.getDelayTimer() > 0) cpu.decrementDelayTimer();
            if (cpu.getSoundTimer() > 0) {
                cpu.decrementSoundTimer();
                if (cpu.getSoundTimer() > 0) {
                    mediaPlayer.play();
                }
            }
        }, 0, 17, TimeUnit.MILLISECONDS);
        ScheduledFuture<?> displayThread = pool.scheduleWithFixedDelay(() -> {
            display.draw(cpu.getDisplayBuffer());
        }, 0, 17, TimeUnit.MILLISECONDS);
        stage.setScene(scene);
        stage.show();
    }
    @Override
    public void stop() {
        pool.shutdown();
    }
}